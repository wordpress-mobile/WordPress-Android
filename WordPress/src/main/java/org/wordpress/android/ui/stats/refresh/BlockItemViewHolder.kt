package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.graphics.Typeface
import android.support.annotation.LayoutRes
import android.support.design.widget.TabLayout
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.design.widget.TabLayout.Tab
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.BlockListItem.Item
import org.wordpress.android.ui.stats.refresh.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.BlockListItem.UserItem
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR

sealed class BlockItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class TitleViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_title
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(item: Title) {
            text.setText(item.text)
        }
    }

    class InformationViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_information
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(item: Information) {
            text.text = item.text
        }
    }

    class ItemViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_item
    ) {
        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)

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

    class UserItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_item
    ) {
        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)

        fun bind(item: UserItem) {
            imageManager.loadIntoCircle(icon, AVATAR, item.avatarUrl)
            text.text = item.text
            value.text = item.value
            divider.visibility = if (item.showDivider) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    class EmptyViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_empty_item
    )

    class TextViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_text_item
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(textItem: Text) {
            val spannableString = SpannableString(textItem.text)
            textItem.links?.forEach { link ->
                spannableString.withClickableSpan(text.context, link.link) { link.action(text.context) }
            }
            text.text = spannableString
            text.linksClickable = true
            text.movementMethod = LinkMovementMethod.getInstance()
        }

        private fun SpannableString.withClickableSpan(
            context: Context,
            clickablePart: String,
            onClickListener: (Context) -> Unit
        ): SpannableString {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View?) {
                    widget?.context?.let { onClickListener.invoke(it) }
                }

                override fun updateDrawState(ds: TextPaint?) {
                    ds?.color = ContextCompat.getColor(context, R.color.blue_wordpress)
                    ds?.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    ds?.isUnderlineText = false
                }
            }
            val clickablePartStart = indexOf(clickablePart)
            setSpan(
                    clickableSpan,
                    clickablePartStart,
                    clickablePartStart + clickablePart.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return this
        }
    }

    class ColumnsViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_column_item
    ) {
        private val firstKey = itemView.findViewById<TextView>(R.id.first_key)
        private val secondKey = itemView.findViewById<TextView>(R.id.second_key)
        private val thirdKey = itemView.findViewById<TextView>(R.id.third_key)
        private val firstValue = itemView.findViewById<TextView>(R.id.first_value)
        private val secondValue = itemView.findViewById<TextView>(R.id.second_value)
        private val thirdValue = itemView.findViewById<TextView>(R.id.third_value)
        fun bind(columns: Columns) {
            firstKey.setText(columns.headers[0])
            secondKey.setText(columns.headers[1])
            thirdKey.setText(columns.headers[2])
            firstValue.text = columns.values[0]
            secondValue.text = columns.values[1]
            thirdValue.text = columns.values[2]
        }
    }

    class LinkViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_link_item
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val link = itemView.findViewById<View>(R.id.link_wrapper)

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

    class BarChartViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_bar_chart_item
    ) {
        private val chart = itemView.findViewById<BarChart>(R.id.chart)
        private val labelStart = itemView.findViewById<TextView>(R.id.label_start)
        private val labelEnd = itemView.findViewById<TextView>(R.id.label_end)

        fun bind(item: BarChartItem) {
            GlobalScope.launch(Dispatchers.Main) {
                chart.draw(item, labelStart, labelEnd)
            }
        }
    }

    class TabsViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_tabs_item
    ) {
        private val tabLayout = itemView.findViewById<TabLayout>(R.id.tab_layout)
        private val list = itemView.findViewById<RecyclerView>(R.id.recycler_view)

        fun bind(item: TabsItem) {
            if (tabLayout.tabCount == 0) {
                item.tabs.forEach {
                    tabLayout.addTab(tabLayout.newTab().setText(it.title))
                }
            }
            list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
            if (list.adapter == null) {
                list.adapter = BlockListAdapter(imageManager)
            }
            (list.adapter as BlockListAdapter).update(item.tabs[tabLayout.selectedTabPosition].items)
            tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabReselected(tab: Tab) {
                }

                override fun onTabUnselected(tab: Tab) {
                }

                override fun onTabSelected(tab: Tab) {
                    (list.adapter as BlockListAdapter).update(item.tabs[tab.position].items)
                }
            })
        }
    }

    class LabelViewHolder(parent: ViewGroup) : BlockItemViewHolder(
            parent,
            R.layout.stats_block_label
    ) {
        private val leftLabel = itemView.findViewById<TextView>(R.id.left_label)
        private val rightLabel = itemView.findViewById<TextView>(R.id.right_label)
        fun bind(item: Label) {
            leftLabel.setText(item.leftLabel)
            rightLabel.setText(item.rightLabel)
        }
    }
}
