package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.util.image.ImageManager

class ListItemWithIconViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_list_item
) {
    private val iconContainer = itemView.findViewById<LinearLayout>(R.id.icon_container)
    private val text = itemView.findViewById<TextView>(R.id.text)
    private val value = itemView.findViewById<TextView>(R.id.value)
    private val divider = itemView.findViewById<View>(R.id.divider)
    private var topMargin = itemView.findViewById<View>(R.id.top_margin)
    private var bar = itemView.findViewById<ProgressBar>(R.id.bar)

    fun bind(item: ListItemWithIcon) {
        iconContainer.setIconOrAvatar(item, imageManager)
        text.setTextOrHide(item.textResource, item.text)
        val textColor = when (item.textStyle) {
            TextStyle.NORMAL -> R.color.neutral_700
            LIGHT -> R.color.neutral_500
        }
        text.setTextColor(ContextCompat.getColor(text.context, textColor))
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
        if (item.barWidth != null) {
            bar.visibility = View.VISIBLE
            topMargin.visibility = View.VISIBLE
            bar.progress = item.barWidth
        } else {
            bar.visibility = View.GONE
            topMargin.visibility = View.GONE
        }
        itemView.contentDescription = item.contentDescription
    }
}
