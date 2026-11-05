package com.opensandbox.dhizuku.ui

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.opensandbox.dhizuku.R

/** 只负责把 AppRow 画成一行，不做任何业务判断。 */
class AppListAdapter(private val context: Context) : BaseAdapter() {

    private val rows = mutableListOf<AppRow>()

    fun setRows(next: List<AppRow>) {
        rows.clear()
        rows.addAll(next)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = rows.size

    override fun getItem(position: Int): AppRow = rows[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = convertView as? LinearLayout ?: createRow()
        val item = getItem(position)

        val flags = buildString {
            if (item.frozen) append(context.getString(R.string.flag_frozen))
            if (item.uninstallBlocked) append(context.getString(R.string.flag_uninstall_blocked))
        }

        val title = row.getChildAt(0) as TextView
        title.text = flags + item.label
        title.setTextColor(
            context.getColor(if (item.frozen) R.color.text_frozen else R.color.text_primary)
        )

        (row.getChildAt(1) as TextView).text = item.packageName
        return row
    }

    private fun createRow(): LinearLayout {
        val vertical = context.resources.getDimensionPixelSize(R.dimen.spacing_row_vertical)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, vertical, 0, vertical)

            addView(TextView(context).apply {
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(R.dimen.text_row_title)
                )
            })

            addView(TextView(context).apply {
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(R.dimen.text_row_subtitle)
                )
                setTextColor(context.getColor(R.color.text_secondary))
            })
        }
    }
}
