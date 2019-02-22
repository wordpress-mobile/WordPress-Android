package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.util.image.ImageManager

class ListItemWithIconViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        layout.stats_block_list_item
) {
    private val iconContainer = itemView.findViewById<LinearLayout>(id.icon_container)
    private val text = itemView.findViewById<TextView>(id.text)
    private val subtext = itemView.findViewById<TextView>(id.subtext)
    private val value = itemView.findViewById<TextView>(id.value)
    private val divider = itemView.findViewById<View>(id.divider)

    fun bind(item: ListItemWithIcon) {
        iconContainer.setIconOrAvatar(item, imageManager)
        text.setTextOrHide(item.textResource, item.text)
        val textColor = when (item.textStyle) {
            TextStyle.NORMAL -> R.color.grey_dark
            LIGHT -> R.color.grey_darken_20
        }
        text.setTextColor(ContextCompat.getColor(text.context, textColor))
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
