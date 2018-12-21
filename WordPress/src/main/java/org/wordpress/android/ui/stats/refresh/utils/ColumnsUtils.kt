package org.wordpress.android.ui.stats.refresh.utils

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.COLUMNS_VALUE_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns

fun LinearLayout.drawColumns(
    payloads: List<Any>,
    columns: Columns,
    layout: Int
) {
    val inflater = LayoutInflater.from(context)
    val tabSelected = payloads.contains(SELECTED_COLUMN_CHANGED)
    val valuesChanged = payloads.contains(COLUMNS_VALUE_CHANGED)
    when {
        tabSelected -> {
            for (index in 0 until this.childCount) {
                val parent = this.getChildAt(index)
                val key = parent.findViewById<TextView>(R.id.key)
                val isSelected = columns.selectedColumn == index
                key.isSelected = isSelected
                val value = parent.findViewById<TextView>(R.id.value)
                value.isSelected = isSelected
            }
        }
        valuesChanged -> {
            for (index in 0 until this.childCount) {
                this.getChildAt(index).findViewById<TextView>(R.id.value)
                        .text = columns.values[index]
            }
        }
        else -> {
            this.removeAllViewsInLayout()
            for (index in 0 until columns.headers.size) {
                val item = inflater.inflate(layout, this, false)
                val previousParams = item.layoutParams as LinearLayout.LayoutParams
                previousParams.weight = 1F
                previousParams.width = 0
                this.addView(
                        item,
                        previousParams
                )
                item.setOnClickListener {
                    columns.onColumnSelected?.invoke(index)
                }
                val isSelected = columns.selectedColumn == null || columns.selectedColumn == index
                val key = item.findViewById<TextView>(R.id.key)
                key.setText(columns.headers[index])
                key.isSelected = isSelected
                val value = item.findViewById<TextView>(R.id.value)
                value.text = columns.values[index]
                value.isSelected = isSelected
            }
        }
    }
}
