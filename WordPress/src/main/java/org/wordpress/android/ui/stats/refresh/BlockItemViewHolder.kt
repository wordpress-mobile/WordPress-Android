package org.wordpress.android.ui.stats.refresh

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title

sealed class BlockItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class TitleViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_title
    ) {
        private val text = itemView.findViewById<TextView>(id.text)
        fun bind(item: Title) {
            text.setText(item.text)
        }
    }

    class ItemViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_item
    ) {
        private val icon = itemView.findViewById<ImageView>(id.icon)
        private val text = itemView.findViewById<TextView>(id.text)
        private val value = itemView.findViewById<TextView>(id.value)
        private val divider = itemView.findViewById<View>(id.divider)

        fun bind(item: Item) {
            icon.setImageResource(item.icon)
            text.setText(item.text)
            value.text = item.value
            divider.visibility = if (item.showDivider) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    class EmptyViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_empty_item
    )

    class TextViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_text_item
    ) {
        private val text = itemView.findViewById<TextView>(id.text)
        fun bind(textItem: Text) {
            text.text = textItem.text
            text.linksClickable = true
            text.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    class ColumnsViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_column_item
    ) {
        private val firstKey = itemView.findViewById<TextView>(id.first_key)
        private val secondKey = itemView.findViewById<TextView>(id.second_key)
        private val thirdKey = itemView.findViewById<TextView>(id.third_key)
        private val firstValue = itemView.findViewById<TextView>(id.first_value)
        private val secondValue = itemView.findViewById<TextView>(id.second_value)
        private val thirdValue = itemView.findViewById<TextView>(id.third_value)
        fun bind(columns: Columns) {
            firstKey.setText(columns.headers[0])
            secondKey.setText(columns.headers[1])
            thirdKey.setText(columns.headers[2])
            firstValue.text = columns.values[0]
            secondValue.text = columns.values[1]
            thirdValue.text = columns.values[2]
        }
    }

    class LinkViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_link_item
    ) {
        private val text = itemView.findViewById<TextView>(id.text)
        private val link = itemView.findViewById<View>(id.link_wrapper)

        fun bind(item: Link) {
            if (item.icon != null) {
                text.setCompoundDrawablesWithIntrinsicBounds(item.icon, 0, 0, 0)
            } else {
                text.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            text.setText(item.text)
            link.setOnClickListener { item.action() }
        }
    }

    class BarChartViewHolder(parent: ViewGroup) : BlockItemViewHolder(parent,
            layout.stats_block_bar_chart_item
    ) {
        private val chart = itemView.findViewById<BarChart>(id.chart)
        private val labelStart = itemView.findViewById<TextView>(id.label_start)
        private val labelEnd = itemView.findViewById<TextView>(id.label_end)

        fun bind(item: BarChartItem) {
            GlobalScope.launch(Dispatchers.Main) {
                chart.draw(item, labelStart, labelEnd)
            }
        }
    }
}
