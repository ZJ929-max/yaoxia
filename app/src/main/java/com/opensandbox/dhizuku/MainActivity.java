package com.opensandbox.dhizuku;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.app.admin.DevicePolicyManager;
import android.net.Uri;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String PREFS = "sandbox";
    private static final String KEY_FROZEN = "frozen";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView status;
    private final AppAdapter adapter = new AppAdapter();

    private volatile IRemoteDpm remote;
    private String diagnostics = "";

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            remote = IRemoteDpm.Stub.asInterface(binder);
            setStatus("沙盒引擎已就绪（运行在 Dhizuku 进程内）");
            refreshApps();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remote = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        connect();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), 0);

        TextView title = new TextView(this);
        title.setText("OpenSandbox");
        title.setTextSize(22f);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(13f);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, dp(6), 0, dp(12));
        status.setText("启动中…");
        root.addView(status);

        Button connectBtn = new Button(this);
        connectBtn.setText("连接 Dhizuku 并授权");
        connectBtn.setOnClickListener(v -> connect());
        root.addView(connectBtn);

        Button diagBtn = new Button(this);
        diagBtn.setText("复制诊断信息");
        diagBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("opensandbox-diag", diagnostics));
            toast("诊断信息已复制");
        });
        root.addView(diagBtn);

        Button refreshBtn = new Button(this);
        refreshBtn.setText("刷新应用列表");
        refreshBtn.setOnClickListener(v -> refreshApps());
        root.addView(refreshBtn);

        ListView list = new ListView(this);
        list.setAdapter(adapter);
        list.setDividerHeight(0);
        list.setOnItemClickListener((parent, view, position, id) -> toggle(adapter.getItem(position)));
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return root;
    }

    private void connect() {
        setStatus("正在连接 Dhizuku…");
        io.execute(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Android SDK ").append(android.os.Build.VERSION.SDK_INT).append("\n");

            try {
                PackageInfo pi = getPackageManager().getPackageInfo("com.rosan.dhizuku", 0);
                sb.append("Dhizuku ").append(pi.versionName)
                        .append(" (").append(pi.getLongVersionCode()).append(")\n");
            } catch (Throwable t) {
                sb.append("Dhizuku 包: 查不到（未安装或不可见）\n");
            }

            int perm = checkSelfPermission("com.rosan.dhizuku.permission.API");
            sb.append("API 权限: ")
                    .append(perm == PackageManager.PERMISSION_GRANTED ? "已授予" : "未授予")
                    .append("\n");

            // 不依赖 Dhizuku-API 的版本差异，直接查系统里的活跃管理员
            ComponentName owner = null;
            try {
                android.app.admin.DevicePolicyManager dpm =
                        getSystemService(android.app.admin.DevicePolicyManager.class);
                java.util.List<ComponentName> admins = dpm.getActiveAdmins();
                if (admins != null) {
                    for (ComponentName c : admins) {
                        if (dpm.isDeviceOwnerApp(c.getPackageName())) {
                            owner = c;
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                sb.append("读 owner 异常: ").append(t.getClass().getSimpleName())
                        .append(": ").append(t.getMessage()).append("\n");
            }
            sb.append("owner 组件: ")
                    .append(owner == null ? "无（没有 DeviceOwner/ProfileOwner）" : owner.flattenToShortString())
                    .append("\n");

            try {
                getContentResolver().call(
                        Uri.parse("content://com.rosan.dhizuku.server.provider"),
                        "diagnose", null, new Bundle());
                sb.append("Provider: 可达\n");
            } catch (Throwable t) {
                sb.append("Provider 异常: ").append(t.getClass().getSimpleName())
                        .append(": ").append(t.getMessage()).append("\n");
            }

            boolean inited = false;
            boolean granted = false;
            String version = "";
            try {
                inited = Dhizuku.init(getApplicationContext());
                if (inited) {
                    granted = Dhizuku.isPermissionGranted();
                    try {
                        version = Dhizuku.getVersionName();
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable t) {
                sb.append("init 抛异常: ").append(t.getClass().getSimpleName())
                        .append(": ").append(t.getMessage()).append("\n");
            }
            sb.append("init: ").append(inited ? "成功" : "失败")
                    .append(" / 已授权: ").append(granted ? "是" : "否")
                    .append(" / server: ").append(version).append("\n");

            final String diag = sb.toString();
            final boolean fInited = inited;
            final boolean fGranted = granted;
            final String fVersion = version;

            ui.post(() -> {
                diagnostics = diag;
                if (!fInited) {
                    setStatus("连接失败，诊断如下：\n" + diag);
                    return;
                }
                if (fGranted) {
                    onGranted(fVersion);
                } else if (checkSelfPermission("com.rosan.dhizuku.permission.API")
                        != PackageManager.PERMISSION_GRANTED) {
                    // Dhizuku 的 API 权限是 dangerous 级，必须先向系统申请
                    setStatus("需要先授予 Dhizuku API 权限…\n" + diag);
                    requestPermissions(new String[]{"com.rosan.dhizuku.permission.API"}, 100);
                } else {
                    setStatus("已连上，等待授权…\n" + diag);
                    requestPermission();
                }
            });
        });
    }

    private void requestPermission() {
        try {
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    ui.post(() -> {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            onGranted("");
                        } else {
                            setStatus("授权被拒绝，或 Dhizuku 没有响应。点上面的按钮重试。");
                        }
                    });
                }
            });
        } catch (Throwable t) {
            setStatus("发起授权失败：" + t.getMessage());
        }
    }

    private void onGranted(String version) {
        setStatus("已授权" + (version.isEmpty() ? "" : "（server " + version + "）") + "，正在启动沙盒引擎…");
        try {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(
                    new ComponentName(this, RemoteDpmService.class));
            Dhizuku.bindUserService(args, connection);
        } catch (Throwable t) {
            setStatus("启动沙盒引擎失败：" + t.getMessage());
        }
    }

    private void refreshApps() {
        io.execute(() -> {
            PackageManager pm = getPackageManager();

            // 只列出"有启动图标"的应用，避免把几百个系统组件塞进列表
            Map<String, String> launchable = new LinkedHashMap<>();
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
            if (resolved != null) {
                for (ResolveInfo ri : resolved) {
                    if (ri.activityInfo == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.equals(getPackageName())) continue;
                    launchable.put(pkg, String.valueOf(ri.loadLabel(pm)));
                }
            }

            // 候选集 = 启动器里看得到的 + 上次记录为已冻结的
            Set<String> candidates = new LinkedHashSet<>(launchable.keySet());
            candidates.addAll(loadFrozen());

            Set<String> frozen = new HashSet<>();
            IRemoteDpm r = remote;
            if (r != null) {
                try {
                    String[] arr = r.filterSuspended(candidates.toArray(new String[0]));
                    if (arr != null) {
                        for (String s : arr) frozen.add(s);
                    }
                } catch (Throwable ignored) {
                }
            }

            List<AppEntry> entries = new ArrayList<>();
            for (Map.Entry<String, String> e : launchable.entrySet()) {
                entries.add(new AppEntry(e.getKey(), e.getValue(), frozen.contains(e.getKey())));
            }
            // 被冻结后从启动器消失的应用也要列出来，否则解冻不回来
            for (String pkg : frozen) {
                if (!launchable.containsKey(pkg) && !pkg.equals(getPackageName())) {
                    entries.add(new AppEntry(pkg, pkg, true));
                }
            }
            entries.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label));

            saveFrozen(frozen);

            final List<AppEntry> result = entries;
            ui.post(() -> {
                adapter.setAll(result);
                setStatus(remote == null
                        ? "引擎未就绪，只能查看列表"
                        : "共 " + result.size() + " 个应用，点一下即可冻结 / 解冻");
            });
        });
    }

    private void toggle(AppEntry entry) {
        IRemoteDpm r = remote;
        if (r == null) {
            toast("沙盒引擎未就绪：先连接并授权");
            return;
        }
        final boolean target = !entry.frozen;
        io.execute(() -> {
            try {
                r.setSuspended(new String[]{entry.packageName}, target);
                ui.post(() -> {
                    toast((target ? "已冻结 " : "已解冻 ") + entry.label);
                    refreshApps();
                });
            } catch (Throwable t) {
                ui.post(() -> toast("操作失败：" + t.getMessage()));
            }
        });
    }

    private Set<String> loadFrozen() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        return new HashSet<>(sp.getStringSet(KEY_FROZEN, new HashSet<>()));
    }

    private void saveFrozen(Set<String> frozen) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putStringSet(KEY_FROZEN, new HashSet<>(frozen))
                .apply();
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    static final class AppEntry {
        final String packageName;
        final String label;
        final boolean frozen;

        AppEntry(String packageName, String label, boolean frozen) {
            this.packageName = packageName;
            this.label = label;
            this.frozen = frozen;
        }
    }

    final class AppAdapter extends BaseAdapter {
        private final List<AppEntry> items = new ArrayList<>();

        void setAll(List<AppEntry> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public AppEntry getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
            } else {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, dp(10), 0, dp(10));

                TextView name = new TextView(MainActivity.this);
                name.setTextSize(16f);
                name.setTextColor(Color.BLACK);
                row.addView(name);

                TextView pkg = new TextView(MainActivity.this);
                pkg.setTextSize(12f);
                pkg.setTextColor(Color.GRAY);
                row.addView(pkg);
            }

            AppEntry item = getItem(position);
            TextView name = (TextView) row.getChildAt(0);
            TextView pkg = (TextView) row.getChildAt(1);

            name.setText(item.frozen ? "❄ " + item.label : item.label);
            name.setTextColor(item.frozen ? Color.parseColor("#1565C0") : Color.BLACK);
            pkg.setText(item.packageName);
            return row;
        }
    }
}
