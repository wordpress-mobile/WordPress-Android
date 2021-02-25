package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.widgets.QuickStartFocusPoint

class MySiteListItemViewHolder(parent: ViewGroup, private val uiHelpers: UiHelpers) : MySiteItemViewHolder(
        parent,
        R.layout.my_site_item_block
) {
    private val primaryIcon = itemView.findViewById<ImageView>(R.id.my_site_item_primary_icon)
    private val primaryText = itemView.findViewById<TextView>(R.id.my_site_item_primary_text)
    private val secondaryIcon = itemView.findViewById<ImageView>(R.id.my_site_item_secondary_icon)
    private val secondaryText = itemView.findViewById<TextView>(R.id.my_site_item_secondary_text)
    private val focusPoint = itemView.findViewById<QuickStartFocusPoint>(R.id.my_site_item_quick_start_focus_point)
    fun bind(item: MySiteItem.ListItem) {
        uiHelpers.setImageOrHide(primaryIcon, item.primaryIcon)
        uiHelpers.setImageOrHide(secondaryIcon, item.secondaryIcon)
        uiHelpers.setTextOrHide(primaryText, item.primaryText)
        uiHelpers.setTextOrHide(secondaryText, item.secondaryText)
        itemView.setOnClickListener { item.onClick.click() }
        focusPoint.setVisibleOrGone(item.showFocusPoint)
    }
}
