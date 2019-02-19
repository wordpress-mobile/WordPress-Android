package org.wordpress.android.ui.stats.refresh.lists.sections

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.http.SslError
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.design.widget.TabLayout.Tab
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
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
import android.view.ViewGroup.LayoutParams
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.COLUMNS_VALUE_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ChartLegend
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.utils.draw
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import org.wordpress.android.util.image.ImageType.IMAGE
import org.wordpress.android.util.setVisible

sealed class BlockListItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class TitleViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_title_item
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(item: Title) {
            text.setTextOrHide(item.textResource, item.text)
        }
    }

    class ValueViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_value_item
    ) {
        private val container = itemView.findViewById<LinearLayout>(R.id.value_container)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val unit = itemView.findViewById<TextView>(R.id.unit)
        private val positiveChange = itemView.findViewById<TextView>(R.id.positive_change)
        private val negativeChange = itemView.findViewById<TextView>(R.id.negative_change)
        fun bind(item: ValueItem) {
            value.text = item.value
            unit.setText(item.unit)
            val hasChange = item.change != null
            positiveChange.visibility = if (hasChange && item.positive) View.VISIBLE else View.GONE
            negativeChange.visibility = if (hasChange && !item.positive) View.VISIBLE else View.GONE
            positiveChange.text = item.change
            negativeChange.text = item.change
            val params = container.layoutParams as RecyclerView.LayoutParams
            val topMargin = if (item.isFirst) container.resources.getDimensionPixelSize(R.dimen.margin_medium) else 0
            params.setMargins(0, topMargin, 0, 0)
            container.layoutParams = params
        }
    }

    class InformationViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_info_item
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(item: Information) {
            text.text = item.text
        }
    }

    open class ListItemWithIconViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_list_item
    ) {
        private val iconContainer = itemView.findViewById<LinearLayout>(R.id.icon_container)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val subtext = itemView.findViewById<TextView>(R.id.subtext)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)

        fun bind(item: ListItemWithIcon) {
            iconContainer.setIconOrAvatar(item, imageManager)
            text.setTextOrHide(item.textResource, item.text)
            subtext.setTextOrHide(item.subTextResource, item.subText)
            value.setTextOrHide(item.valueResource, item.value)
            divider.visibility = if (item.showDivider) {
                View.VISIBLE
            } else {
                View.GONE
            }
            val clickAction = item.navigationAction
            if (clickAction != null) {
                itemView.isClickable = true
                itemView.setOnClickListener { clickAction.click() }
            } else {
                itemView.isClickable = false
                itemView.background = null
                itemView.setOnClickListener(null)
            }
        }
    }

    class ListItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_list_item
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
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(message: Empty) {
            when {
                message.textResource != null -> text.setText(message.textResource)
                message.text != null -> text.text = message.text
                else -> text.setText(R.string.stats_no_data_yet)
            }
        }
    }

    class DividerViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_divider_item
    )

    class TextViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_text_item
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        fun bind(textItem: Text) {
            val loadedText = textItem.text
                    ?: textItem.textResource?.let { text.resources.getString(textItem.textResource) } ?: ""
            val spannableString = SpannableString(loadedText)
            textItem.links?.forEach { link ->
                spannableString.withClickableSpan(text.context, link.link) {
                    link.navigationAction.click()
                }
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
                    ds?.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL)
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

    class FourColumnsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_four_columns_item
    ) {
        private val columnLayouts = listOf<LinearLayout>(
                itemView.findViewById(R.id.column1),
                itemView.findViewById(R.id.column2),
                itemView.findViewById(R.id.column3),
                itemView.findViewById(R.id.column4)
        )

        fun bind(
            columns: Columns,
            payloads: List<Any>
        ) {
            val tabSelected = payloads.contains(SELECTED_COLUMN_CHANGED)
            val valuesChanged = payloads.contains(COLUMNS_VALUE_CHANGED)
            when {
                tabSelected -> {
                    columnLayouts.forEachIndexed { index, layout ->
                        layout.setSelection(columns.selectedColumn == index)
                    }
                }
                valuesChanged -> {
                    columnLayouts.forEachIndexed { index, layout ->
                        layout.value().text = columns.values[index]
                    }
                }
                else -> {
                    columnLayouts.forEachIndexed { index, layout ->
                        layout.setOnClickListener {
                            columns.onColumnSelected?.invoke(index)
                        }
                        layout.key().setText(columns.headers[index])
                        layout.value().text = columns.values[index]
                        layout.setSelection(columns.selectedColumn == null || columns.selectedColumn == index)
                    }
                }
            }
        }
        private fun LinearLayout.setSelection(isSelected: Boolean) {
            key().isSelected = isSelected
            value().isSelected = isSelected
            selector().visibility = if (isSelected) View.VISIBLE else View.GONE
        }
        private fun LinearLayout.key(): TextView = this.findViewById(R.id.key)
        private fun LinearLayout.value(): TextView = this.findViewById(R.id.value)
        private fun LinearLayout.selector(): View = this.findViewById(R.id.selector)
    }

    class LinkViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_link_item
    ) {
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val link = itemView.findViewById<View>(R.id.link_wrapper)

        fun bind(item: Link) {
            if (item.icon != null) {
                val drawable = text.context.resources.getDrawable(item.icon, text.context.theme)
                // Suppress getColor(int) warning since getColor(int, Theme) cannot be used until minSdkVersion is 23.
                @Suppress("DEPRECATION")
                DrawableCompat.setTint(drawable, text.context.resources.getColor(R.color.blue_medium))
                DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN)
                text.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            } else {
                text.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            text.setText(item.text)
            link.setOnClickListener { item.navigateAction.click() }
        }
    }

    class BarChartViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_bar_chart_item
    ) {
        private val chart = itemView.findViewById<BarChart>(R.id.chart)
        private val labelStart = itemView.findViewById<TextView>(R.id.label_start)
        private val labelEnd = itemView.findViewById<TextView>(R.id.label_end)

        fun bind(
            item: BarChartItem,
            barSelected: Boolean
        ) {
            GlobalScope.launch(Dispatchers.Main) {
                delay(50)
                chart.draw(item, labelStart, labelEnd)
            }
        }
    }

    class ChartLegendViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_legend_item
    ) {
        private val legend = itemView.findViewById<TextView>(R.id.legend)
        fun bind(item: ChartLegend) {
            legend.setText(item.text)
        }
    }

    class TabsViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_tabs_item
    ) {
        private val tabLayout = itemView.findViewById<TabLayout>(R.id.tab_layout)

        fun bind(item: TabsItem, tabChanged: Boolean) {
            tabLayout.clearOnTabSelectedListeners()
            if (!tabChanged) {
                tabLayout.removeAllTabs()
                item.tabs.forEach { tabItem ->
                    tabLayout.addTab(tabLayout.newTab().setText(tabItem))
                }
            }
            tabLayout.getTabAt(item.selectedTabPosition)?.select()

            tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabReselected(tab: Tab) {
                }

                override fun onTabUnselected(tab: Tab) {
                }

                override fun onTabSelected(tab: Tab) {
                    item.onTabSelected(tab.position)
                }
            })
        }
    }

    class HeaderViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_header_item
    ) {
        private val leftLabel = itemView.findViewById<TextView>(R.id.left_label)
        private val rightLabel = itemView.findViewById<TextView>(R.id.right_label)
        fun bind(item: Header) {
            leftLabel.setText(item.leftLabel)
            rightLabel.setText(item.rightLabel)
        }
    }

    class ExpandableItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_list_item
    ) {
        private val iconContainer = itemView.findViewById<LinearLayout>(R.id.icon_container)
        private val text = itemView.findViewById<TextView>(R.id.text)
        private val value = itemView.findViewById<TextView>(R.id.value)
        private val divider = itemView.findViewById<View>(R.id.divider)
        private val expandButton = itemView.findViewById<ImageView>(R.id.expand_button)

        fun bind(
            expandableItem: ExpandableItem,
            expandChanged: Boolean
        ) {
            val header = expandableItem.header
            iconContainer.setIconOrAvatar(header, imageManager)
            text.setTextOrHide(header.textResource, header.text)
            expandButton.visibility = View.VISIBLE
            value.setTextOrHide(header.valueResource, header.value)
            divider.setVisible(header.showDivider && !expandableItem.isExpanded)

            if (expandChanged) {
                val rotationAngle = if (expandButton.rotation == 0F) 180 else 0
                expandButton.animate().rotation(rotationAngle.toFloat()).setDuration(200).start()
            } else {
                expandButton.rotation = if (expandableItem.isExpanded) 180F else 0F
            }
            itemView.isClickable = true
            itemView.setOnClickListener {
                expandableItem.onExpandClicked(!expandableItem.isExpanded)
            }
        }
    }

    class MapViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
            parent,
            R.layout.stats_block_web_view_item
    ) {
        val webView = itemView.findViewById<WebView>(R.id.web_view)
        fun bind(item: MapItem) {
            GlobalScope.launch {
                delay(100)
                // See: https://developers.google.com/chart/interactive/docs/gallery/geochart
                // Loading the v42 of the Google Charts API, since the latest stable version has a problem with
                // the legend. https://github.com/wordpress-mobile/WordPress-Android/issues/4131
                // https://developers.google.com/chart/interactive/docs/release_notes#release-candidate-details
                val htmlPage = ("<html>" +
                        "<head>" +
                        "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>" +
                        "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>" +
                        "<script type=\"text/javascript\">" +
                        " google.charts.load('42', {'packages':['geochart']});" +
                        " google.charts.setOnLoadCallback(drawRegionsMap);" +
                        " function drawRegionsMap() {" +
                        " var data = google.visualization.arrayToDataTable(" +
                        " [" +
                        " ['Country', '${itemView.resources.getString(item.label)}'],${item.mapData}]);" +
                        " var options = {keepAspectRatio: true, region: 'world', colorAxis:" +
                        " { colors: [ '#FFF088', '#F24606' ] }, enableRegionInteractivity: false};" +
                        " var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));" +
                        " chart.draw(data, options);" +
                        " }" +
                        "</script>" +
                        "</head>" +
                        "<body>" +
                        "<div id=\"regions_div\" style=\"width: 100%; height: 100%;\"></div>" +
                        "</body>" +
                        "</html>")

                val width = itemView.width
                val height = width * 3 / 4

                val params = webView.layoutParams as FrameLayout.LayoutParams
                val wrapperParams = itemView.layoutParams as RecyclerView.LayoutParams
                params.width = LayoutParams.MATCH_PARENT
                params.height = height
                wrapperParams.width = LayoutParams.MATCH_PARENT
                wrapperParams.height = height

                launch(Dispatchers.Main) {
                    webView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

                    webView.layoutParams = params
                    itemView.layoutParams = wrapperParams

                    webView.webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError
                        ) {
                            super.onReceivedError(view, request, error)
                            itemView.visibility = View.GONE
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            super.onReceivedSslError(view, handler, error)
                            itemView.visibility = View.GONE
                        }
                    }
                    webView.settings.javaScriptEnabled = true
                    webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    webView.loadData(htmlPage, "text/html", "UTF-8")
                }
            }
        }
    }

    internal fun TextView.setTextOrHide(@StringRes resource: Int?, value: String?) {
        this.visibility = View.VISIBLE
        when {
            resource != null -> {
                this.visibility = View.VISIBLE
                this.setText(resource)
            }
            value != null -> {
                this.visibility = View.VISIBLE
                this.text = value
            }
            else -> this.visibility = GONE
        }
    }

    private fun ImageView.setImageOrLoad(
        item: ListItemWithIcon,
        imageManager: ImageManager
    ) {
        when {
            item.icon != null -> {
                this.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, R.color.grey_dark))
                this.visibility = View.VISIBLE
                imageManager.load(this, item.icon)
            }
            item.iconUrl != null -> {
                this.visibility = View.VISIBLE
                imageManager.load(this, IMAGE, item.iconUrl)
            }
            else -> this.visibility = View.GONE
        }
    }

    private fun ImageView.setAvatarOrLoad(
        item: ListItemWithIcon,
        imageManager: ImageManager
    ) {
        when {
            item.icon != null -> {
                this.visibility = View.VISIBLE
                imageManager.load(this, item.icon)
            }
            item.iconUrl != null -> {
                this.visibility = View.VISIBLE
                imageManager.loadIntoCircle(this, AVATAR_WITHOUT_BACKGROUND, item.iconUrl)
            }
            else -> this.visibility = View.GONE
        }
    }

    internal fun LinearLayout.setIconOrAvatar(item: ListItemWithIcon, imageManager: ImageManager) {
        val avatar = findViewById<ImageView>(R.id.avatar)
        val icon = findViewById<ImageView>(R.id.icon)
        val hasIcon = item.icon != null || item.iconUrl != null
        if (hasIcon) {
            this.visibility = View.VISIBLE
            when (item.iconStyle) {
                NORMAL -> {
                    findViewById<ImageView>(R.id.avatar).visibility = GONE
                    icon.setImageOrLoad(item, imageManager)
                }
                AVATAR -> {
                    icon.visibility = GONE
                    avatar.setAvatarOrLoad(item, imageManager)
                }
            }
        } else {
            this.visibility = View.GONE
        }
    }
}
