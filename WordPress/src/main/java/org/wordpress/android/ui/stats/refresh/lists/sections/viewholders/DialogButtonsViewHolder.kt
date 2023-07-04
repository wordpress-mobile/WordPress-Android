package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.Button
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.DialogButtons

class DialogButtonsViewHolder(val parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_dialog_buttons_item
) {
    private val positiveButton = itemView.findViewById<Button>(R.id.button_positive)
    private val negativeButton = itemView.findViewById<Button>(R.id.button_negative)

    fun bind(item: DialogButtons) {
        positiveButton.setText(item.positiveButtonText)
        positiveButton.setOnClickListener { item.positiveAction.click() }
        negativeButton.setText(item.negativeButtonText)
        negativeButton.setOnClickListener { item.negativeAction.click() }
    }
}
