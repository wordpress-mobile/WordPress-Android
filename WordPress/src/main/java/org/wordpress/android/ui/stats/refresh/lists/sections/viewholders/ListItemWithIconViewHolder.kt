package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.constraint.ConstraintLayout
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
    private val constraintLayout = itemView.findViewById<ConstraintLayout>(id.stats_block_list_item_constraint_layout)
    private val iconContainer = itemView.findViewById<LinearLayout>(id.icon_container)
    private val text = itemView.findViewById<TextView>(id.text)
    private val subtext = itemView.findViewById<TextView>(id.subtext)
    private val value = itemView.findViewById<TextView>(id.value)
    private val divider = itemView.findViewById<View>(id.divider)
    private var percentageBar = itemView.findViewById<View>(id.percentage_bar)

    fun bind(item: ListItemWithIcon) {
        iconContainer.setIconOrAvatar(item, imageManager)
        text.setTextOrHide(item.textResource, item.text)
        val textColor = when (item.textStyle) {
            TextStyle.NORMAL -> R.color.neutral_700
            LIGHT -> R.color.neutral_500
        }

        text.setTextColor(ContextCompat.getColor(text.context, textColor))
        subtext.setTextOrHide(item.subTextResource, item.subText)
        value.setTextOrHide(item.valueResource, item.value)

        divider.visibility = if (item.showDivider) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if(item.percentageOfMaxValue != null) {
            percentageBar.visibility = View.VISIBLE

            val globalLayoutListener = PercentageBarGlobalListener(constraintLayout, text, value, percentageBar, item.percentageOfMaxValue)

            constraintLayout.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        } else {
            percentageBar.visibility = View.GONE
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
