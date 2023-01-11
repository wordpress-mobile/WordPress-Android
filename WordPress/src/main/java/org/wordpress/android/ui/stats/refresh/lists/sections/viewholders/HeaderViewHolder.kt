package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header

class HeaderViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_header_item
) {
    private val startLabel = itemView.findViewById<TextView>(R.id.start_label)
    private val endLabel = itemView.findViewById<TextView>(R.id.end_label)
    fun bind(item: Header) {
        val spannableString = SpannableString(startLabel.resources.getString(item.startLabel))
        item.bolds?.forEach { bold ->
            spannableString.withBoldSpan(bold)
        }
        startLabel.text = spannableString
        endLabel.setText(item.endLabel)
    }

    private fun SpannableString.withBoldSpan(boldPart: String): SpannableString {
        val boldPartIndex = indexOf(boldPart)
        setSpan(
            StyleSpan(Typeface.BOLD),
            boldPartIndex,
            boldPartIndex + boldPart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }
}
