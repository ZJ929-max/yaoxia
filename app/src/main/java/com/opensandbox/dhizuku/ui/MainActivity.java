package com.opensandbox.dhizuku.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.opensandbox.dhizuku.BuildConfig;
import com.opensandbox.dhizuku.R;
import com.opensandbox.dhizuku.apps.AppCatalog;
import com.opensandbox.dhizuku.apps.AppControl;
import com.opensandbox.dhizuku.apps.AppEntry;
import com.opensandbox.dhizuku.apps.FrozenStore;
import com.opensandbox.dhizuku.apps.PolicyAppControl;
import com.opensandbox.dhizuku.channels.PolicyChannel;
import com.opensandbox.dhizuku.core.AsyncTaskRunner;
import com.opensandbox.dhizuku.core.Outcome;
import com.opensandbox.dhizuku.delegation.DelegationService;
import com.opensandbox.dhizuku.platform.CapabilityProvider;
import com.opensandbox.dhizuku.platform.CapabilityRouter;
import com.opensandbox.dhizuku.platform.ProviderRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 组装层：装配路由与通道，渲染状态，转发用户操作。
 * 这里不出现任何 dhi / she / shi 的具体类名——那些都在 ProviderRegistry 里。
 */
public final class MainActivity extends Activity {

    private final AsyncTaskRunner async = new AsyncTaskRunner("open-sandbox-io");
    private final AppCatalog catalog = new AppCatalog();
    private final List<AppRow> rows = new ArrayList<>();

    private CapabilityRouter router;
    private DelegationService delegation;
    private AppControl appControl;
    private FrozenStore frozenStore;
    private AppListAdapter adapter;

    private TextView statusView;
    private TextView channelsView;
    private String sessionText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        router = new CapabilityRouter(ProviderRegistry.providers(this));
        frozenStore = new FrozenStore(this);
        adapter = new AppListAdapter(this);

        setContentView(buildContentView());
        connect();
    }

    // ------------------------------------------------------------------ 界面

    private LinearLayout buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                dimension(R.dimen.spacing_screen_horizontal),
                dimension(R.dimen.spacing_screen_top),
                dimension(R.dimen.spacing_screen_horizontal),
                0);

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_title));
        title.setTextColor(getColor(R.color.text_primary));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_subtitle));
        subtitle.setTextColor(getColor(R.color.text_secondary));
        root.addView(subtitle);

        channelsView = new TextView(this);
        channelsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_subtitle));
        channelsView.setTextColor(getColor(R.color.text_secondary));
        channelsView.setPadding(0, dimension(R.dimen.spacing_small), 0, dimension(R.dimen.spacing_small));
        root.addView(channelsView);

        statusView = new TextView(this);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_status));
        statusView.setTextColor(getColor(R.color.text_secondary));
        statusView.setPadding(0, dimension(R.dimen.spacing_small), 0, dimension(R.dimen.spacing_small));
        statusView.setText(R.string.status_connecting);
        root.addView(statusView);

        root.addView(button(R.string.button_connect, view -> connect()));
        root.addView(button(R.string.button_scopes, view -> requestScopes()));
        root.addView(button(R.string.button_refresh, view -> refreshApps()));
        root.addView(button(R.string.button_copy_diagnostics, view -> copyDiagnostics()));

        ListView list = new ListView(this);
        list.setAdapter(adapter);
        list.setDividerHeight(0);
        list.setOnItemClickListener((parent, view, position, id) -> toggleFrozen(adapter.getItem(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            toggleUninstallBlock(adapter.getItem(position));
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    private Button button(int textRes, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(textRes);
        button.setOnClickListener(listener);
        return button;
    }

    private int dimension(int resId) {
        return getResources().getDimensionPixelSize(resId);
    }

    private void setStatus(String text) {
        statusView.setText(text);
    }

    // ------------------------------------------------------------------ 连接与通道

    private void connect() {
        setStatus(getString(R.string.status_connecting));
        renderChannels();
        async.execute(() -> {
            Outcome<DelegationService> resolved = router.resolve(this, DelegationService.class);
            if (!resolved.isSuccess()) {
                return Outcome.<Connection>fail(resolved.error());
            }
            DelegationService service = resolved.value();
            Outcome<DelegationService.Session> connected = service.connect(this);
            if (!connected.isSuccess()) {
                return Outcome.<Connection>fail(connected.error());
            }
            Outcome<Boolean> authorized = service.isAuthorized();
            if (!authorized.isSuccess()) {
                return Outcome.<Connection>fail(authorized.error());
            }
            return Outcome.<Connection>ok(new Connection(
                    service, connected.value(), Boolean.TRUE.equals(authorized.value())));
        }, this::renderConnection);
    }

    private void renderConnection(Outcome<Connection> outcome) {
        if (!outcome.isSuccess()) {
            setStatus(getString(R.string.status_connect_failed, outcome.error()));
            return;
        }
        Connection connection = outcome.value();
        delegation = connection.service;
        sessionText = getString(R.string.session_summary,
                connection.session.ownerPackage,
                connection.session.serverVersion,
                connection.session.providerAuthority);

        if (connection.authorized) {
            setStatus(getString(R.string.status_authorized, sessionText));
            resolvePolicyChannel();
        } else {
            setStatus(getString(R.string.status_waiting_authorization, sessionText));
            requestAuthorization();
        }
    }

    private void renderChannels() {
        async.execute(() -> {
            StringBuilder report = new StringBuilder();
            for (CapabilityProvider provider : router.providers()) {
                Outcome<Boolean> availability = provider.isAvailable(this);
                boolean available = availability.isSuccess()
                        && Boolean.TRUE.equals(availability.value());
                report.append(provider.describe())
                        .append("  ")
                        .append(getString(available
                                ? R.string.channel_available
                                : R.string.channel_unavailable))
                        .append('\n');
            }
            return Outcome.<String>ok(report.toString().trim());
        }, outcome -> {
            if (outcome.isSuccess()) {
                channelsView.setText(outcome.value());
            }
        });
    }

    private void requestAuthorization() {
        if (delegation == null) {
            return;
        }
        delegation.requestAuthorization(this, result -> {
            if (result.isSuccess() && Boolean.TRUE.equals(result.value())) {
                resolvePolicyChannel();
            } else {
                setStatus(getString(R.string.status_authorization_denied));
            }
        });
    }

    private void resolvePolicyChannel() {
        async.execute(() -> router.resolve(this, PolicyChannel.class), outcome -> {
            if (!outcome.isSuccess()) {
                setStatus(getString(R.string.status_no_policy_channel, outcome.error()));
                return;
            }
            appControl = new PolicyAppControl(outcome.value());
            loadScopes();
        });
    }

    // ------------------------------------------------------------------ 委派范围

    private void loadScopes() {
        if (delegation == null) {
            return;
        }
        async.execute(delegation::grantedScopes, outcome -> {
            if (!outcome.isSuccess()) {
                setStatus(getString(R.string.status_scopes_failed, outcome.error()));
                return;
            }
            Set<String> scopes = outcome.value();
            setStatus(scopes.isEmpty()
                    ? getString(R.string.status_no_scopes, sessionText)
                    : getString(R.string.status_scopes, sessionText, scopes.toString()));
            refreshApps();
        });
    }

    private void requestScopes() {
        if (delegation == null) {
            setStatus(getString(R.string.status_connecting));
            return;
        }
        setStatus(getString(R.string.status_requesting_scopes));
        async.execute(() -> delegation.requestScopes(delegation.supportedScopes()), outcome -> {
            if (!outcome.isSuccess()) {
                setStatus(getString(R.string.status_scope_request_failed, outcome.error()));
                return;
            }
            setStatus(getString(R.string.status_scopes_updated, outcome.value().toString()));
            refreshApps();
        });
    }

    // ------------------------------------------------------------------ 应用列表

    private void refreshApps() {
        if (appControl == null) {
            setStatus(getString(R.string.status_no_policy_channel, ""));
            return;
        }
        setStatus(getString(R.string.status_loading_apps));
        async.execute(() -> {
            List<AppEntry> entries = catalog.load(getPackageManager(), getPackageName());
            Set<String> remembered = frozenStore.load();

            List<AppRow> built = new ArrayList<>();
            Set<String> frozenNow = new HashSet<>();
            Set<String> seen = new HashSet<>();

            for (AppEntry entry : entries) {
                seen.add(entry.packageName);
                boolean frozen = remembered.contains(entry.packageName)
                        || appControl.isFrozen(entry.packageName);
                if (frozen) {
                    frozenNow.add(entry.packageName);
                }
                built.add(new AppRow(entry.packageName, entry.label, frozen,
                        appControl.isUninstallBlocked(entry.packageName)));
            }

            for (String packageName : remembered) {
                if (seen.contains(packageName) || !appControl.isFrozen(packageName)) {
                    continue;
                }
                frozenNow.add(packageName);
                built.add(new AppRow(packageName, packageName, true,
                        appControl.isUninstallBlocked(packageName)));
            }

            built.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.label, right.label));
            return Outcome.<Snapshot>ok(new Snapshot(built, frozenNow));
        }, outcome -> {
            if (!outcome.isSuccess()) {
                setStatus(getString(R.string.status_connect_failed, outcome.error()));
                return;
            }
            Snapshot snapshot = outcome.value();
            rows.clear();
            rows.addAll(snapshot.rows);
            adapter.setRows(rows);
            frozenStore.save(snapshot.frozen);
            setStatus(getString(R.string.status_app_count, rows.size()));
        });
    }

    private void toggleFrozen(AppRow row) {
        if (appControl == null) {
            return;
        }
        final boolean target = !row.frozen;
        async.execute(() -> appControl.setFrozen(row.packageName, target), outcome -> {
            if (!outcome.isSuccess()) {
                toast(getString(R.string.toast_action_failed, outcome.error()));
                return;
            }
            toast(getString(target ? R.string.toast_frozen : R.string.toast_unfrozen, row.label));
            refreshApps();
        });
    }

    private void toggleUninstallBlock(AppRow row) {
        if (appControl == null) {
            return;
        }
        final boolean target = !row.uninstallBlocked;
        async.execute(() -> appControl.setUninstallBlocked(row.packageName, target), outcome -> {
            if (!outcome.isSuccess()) {
                toast(getString(R.string.toast_action_failed, outcome.error()));
                return;
            }
            toast(getString(target
                    ? R.string.toast_uninstall_blocked
                    : R.string.toast_uninstall_allowed, row.label));
            refreshApps();
        });
    }

    private void copyDiagnostics() {
        String report = channelsView.getText() + "\n" + sessionText + "\n" + statusView.getText();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), report));
        toast(getString(R.string.toast_diagnostics_copied));
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        async.shutdown();
    }

    // ------------------------------------------------------------------ 内部数据

    private static final class Connection {
        final DelegationService service;
        final DelegationService.Session session;
        final boolean authorized;

        Connection(DelegationService service, DelegationService.Session session, boolean authorized) {
            this.service = service;
            this.session = session;
            this.authorized = authorized;
        }
    }

    private static final class Snapshot {
        final List<AppRow> rows;
        final Set<String> frozen;

        Snapshot(List<AppRow> rows, Set<String> frozen) {
            this.rows = rows;
            this.frozen = frozen;
        }
    }
}
