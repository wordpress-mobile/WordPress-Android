package org.wordpress.android.ui.stats.refresh.lists.sections

import android.content.Context
import android.graphics.Typeface
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
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
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.UserItem
import org.wordpress.android.ui.stats.refresh.utils.draw
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR
import org.wordpress.android.util.image.ImageType.IMAGE
import org.wordpress.android.util.setVisible

sealed class BlockListItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class TitleViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_title
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(item: Title) {
            text.setText(item.text)
        }
    }

    class InformationViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_information
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(item: Information) {
            text.text = item.text
        }
    }

    class ListItemWithIconViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_item
    ) {
        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)

        fun bind(item: ListItemWithIcon) {
            icon.setImageOrLoad(item.icon, item.iconUrl) { imageView, url ->
                imageManager.load(imageView, IMAGE, url)
            }
            if (item.icon != null) {
                icon.setImageResource(item.icon)
            } else if (item.iconUrl != null) {
                imageManager.load(icon, IMAGE, item.iconUrl)
            }
            text.setTextOrHide(item.textResource, item.text)
            value.setTextOrHide(item.valueResource, item.value)
            divider.visibility = if (item.showDivider) {
                View.VISIBLE
            } else {
                View.GONE
            }
            if (item.clickAction != null) {
                itemView.isClickable = true
                itemView.setOnClickListener { item.clickAction.invoke() }
            } else {
                itemView.isClickable = false
                itemView.background = null
                itemView.setOnClickListener(null)
            }
        }
    }

    class UserItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
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

    class ListItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_item
    ) {
        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)

        fun bind(item: ListItem) {
            icon.visibility = GONE
            text.text = item.text
            value.text = item.value
            divider.visibility = if (item.showDivider) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    class EmptyViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_empty_item
    )

    class TextViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
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

    class ColumnsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
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

    class LinkViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
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

    class BarChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
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

    class TabsViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
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
            list.isNestedScrollingEnabled = false
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

    class LabelViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
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

    class ExpandableItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_expandable_item
    ) {
        private val list = itemView.findViewById<RecyclerView>(R.id.expandable_items)
        private val expandedListDivider = itemView.findViewById<View>(R.id.expanded_list_divider)

        private val icon = itemView.findViewById<ImageView>(R.id.icon)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)
        private val expandButton = itemView.findViewById<View>(R.id.expand_button)

        fun bind(expandableItem: ExpandableItem) {
            val header = expandableItem.header
            icon.setImageOrLoad(header.icon, header.iconUrl) { imageView, url ->
                imageManager.load(
                        imageView,
                        IMAGE,
                        url
                )
            }
            text.setTextOrHide(header.textResource, header.text)
            expandButton.visibility = View.VISIBLE
            value.setTextOrHide(header.valueResource, header.value)
            divider.setVisible(header.showDivider)

            list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
            list.isNestedScrollingEnabled = false
            if (list.adapter == null) {
                list.adapter = BlockListAdapter(imageManager)
            }
            updateExpandedList(expandableItem)
            itemView.isClickable = true
            itemView.setOnClickListener {
                expandableItem.isExpanded = !expandableItem.isExpanded
                val rotationAngle = if (expandableItem.isExpanded) 180 else 0
                expandButton.animate().rotation(rotationAngle.toFloat()).setDuration(200).start()
                updateExpandedList(expandableItem)
            }
        }

        private fun updateExpandedList(expandableItem: ExpandableItem) {
            if (expandableItem.isExpanded) {
                (list.adapter as BlockListAdapter).update(expandableItem.expandedItems)
            } else {
                (list.adapter as BlockListAdapter).update(listOf())
            }
            divider.setVisible(!expandableItem.isExpanded && expandableItem.header.showDivider)
            expandedListDivider.setVisible(expandableItem.isExpanded)
        }
    }

    fun TextView.setTextOrHide(@StringRes resource: Int?, value: String?) {
        when {
            resource != null -> this.setText(resource)
            value != null -> this.text = value
            else -> this.visibility = GONE
        }
    }

    fun ImageView.setImageOrLoad(
        @DrawableRes resource: Int?,
        url: String?,
        loadMethod: (imageView: ImageView, url: String) -> Unit
    ) {
        when {
            resource != null -> this.setImageResource(resource)
            url != null -> loadMethod(this, url)
            else -> this.visibility = GONE
        }
    }
}
