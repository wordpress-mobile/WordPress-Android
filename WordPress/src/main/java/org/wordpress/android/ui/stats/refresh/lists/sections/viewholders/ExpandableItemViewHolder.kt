package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.extensions.setVisible

class ExpandableItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_list_item
) {
    private val iconContainer = itemView.findViewById<LinearLayout>(id.icon_container)
    private val text = itemView.findViewById<TextView>(id.text)
    private val value = itemView.findViewById<TextView>(id.value)
    private val divider = itemView.findViewById<View>(id.divider)
    private val expandButton = itemView.findViewById<ImageView>(id.expand_button)
    private var bar = itemView.findViewById<ProgressBar>(R.id.bar)

    fun bind(
        expandableItem: ExpandableItem,
        expandChanged: Boolean
    ) {
        val header = expandableItem.header
        iconContainer.setIconOrAvatar(header, imageManager)
        text.setTextOrHide(header.textResource, header.text)
        val textColor = when (expandableItem.header.textStyle) {
            TextStyle.NORMAL -> text.context.getColorResIdFromAttribute(R.attr.colorOnSurface)
            LIGHT -> text.context.getColorResIdFromAttribute(R.attr.wpColorOnSurfaceMedium)
        }
        text.contentDescription = header.contentDescription
        expandButton.visibility = View.VISIBLE
        val expandButtonDescription = when (expandableItem.isExpanded) {
            true -> R.string.stats_collapse_content_description
            false -> R.string.stats_expand_content_description
        }
        expandButton.contentDescription = itemView.resources.getString(expandButtonDescription)
        value.setTextOrHide(header.valueResource, header.value)
        divider.setVisible(header.showDivider && !expandableItem.isExpanded)
        text.setTextColor(AppCompatResources.getColorStateList(text.context, textColor))
        if (header.barWidth != null) {
            bar.visibility = View.VISIBLE
            bar.progress = header.barWidth
        } else {
            bar.visibility = View.GONE
        }

        if (expandChanged) {
            val rotationAngle = if (expandButton.rotation == 0F) 180 else 0
            expandButton.animate().rotation(rotationAngle.toFloat()).setDuration(200).start()
        } else {
            expandButton.rotation = if (expandableItem.isExpanded) 180F else 0F
        }
        itemView.isClickable = true
        itemView.setOnClickListener {
            val announcement = if (expandableItem.isExpanded) {
                itemView.resources.getString(R.string.stats_item_collapsed)
            } else {
                itemView.resources.getString(R.string.stats_item_expanded)
            }
            itemView.announceForAccessibility(announcement)
            expandableItem.onExpandClicked(!expandableItem.isExpanded)
        }
        val longClickAction = expandableItem.header.longClickAction

        if (longClickAction != null) {
            itemView.setOnLongClickListener {
                longClickAction.invoke(itemView)
            }
        }
    }
}
