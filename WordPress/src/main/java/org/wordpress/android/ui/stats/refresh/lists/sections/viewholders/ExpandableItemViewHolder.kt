package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintLayout.LayoutParams
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.setVisible

class ExpandableItemViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        layout.stats_block_list_item
) {
    private val iconContainer = itemView.findViewById<LinearLayout>(id.icon_container)
    private val text = itemView.findViewById<TextView>(id.text)
    private val value = itemView.findViewById<TextView>(id.value)
    private val divider = itemView.findViewById<View>(id.divider)
    private val expandButton = itemView.findViewById<ImageView>(id.expand_button)
    private var percentageBar = itemView.findViewById<View>(id.percentage_bar)

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

        if(header.percentageOfMaxValue != null) {
            percentageBar.visibility = View.VISIBLE

            val params: LayoutParams = percentageBar.layoutParams as LayoutParams
            params.matchConstraintPercentWidth = header.percentageOfMaxValue.toFloat()
            percentageBar.layoutParams = params
        } else {
            percentageBar.visibility = View.GONE
        }
    }
}
