package com.opensandbox.dhizuku.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.opensandbox.dhizuku.R;

import java.util.ArrayList;
import java.util.List;

/** 只负责把 AppRow 画成一行，不做任何业务判断。 */
public final class AppListAdapter extends BaseAdapter {

    private final Context context;
    private final List<AppRow> rows = new ArrayList<>();

    public AppListAdapter(Context context) {
        this.context = context;
    }

    public void setRows(List<AppRow> next) {
        rows.clear();
        rows.addAll(next);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public AppRow getItem(int position) {
        return rows.get(position);
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
            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            int vertical = context.getResources().getDimensionPixelSize(R.dimen.spacing_row_vertical);
            row.setPadding(0, vertical, 0, vertical);

            TextView title = new TextView(context);
            title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimension(R.dimen.text_row_title));
            row.addView(title);

            TextView subtitle = new TextView(context);
            subtitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimension(R.dimen.text_row_subtitle));
            subtitle.setTextColor(context.getColor(R.color.text_secondary));
            row.addView(subtitle);
        }

        AppRow item = getItem(position);
        TextView title = (TextView) row.getChildAt(0);
        TextView subtitle = (TextView) row.getChildAt(1);

        StringBuilder flags = new StringBuilder();
        if (item.frozen) {
            flags.append(context.getString(R.string.flag_frozen));
        }
        if (item.uninstallBlocked) {
            flags.append(context.getString(R.string.flag_uninstall_blocked));
        }

        title.setText(flags + item.label);
        title.setTextColor(context.getColor(
                item.frozen ? R.color.text_frozen : R.color.text_primary));
        subtitle.setText(item.packageName);
        return row;
    }
}
