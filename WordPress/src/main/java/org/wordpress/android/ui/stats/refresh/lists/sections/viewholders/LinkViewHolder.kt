package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.graphics.PorterDuff.Mode.SRC_IN
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link

class LinkViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_link_item
) {
    private val text = itemView.findViewById<TextView>(id.text)
    private val link = itemView.findViewById<View>(id.link_wrapper)

    fun bind(item: Link) {
        if (item.icon != null) {
            val drawable = text.context.resources.getDrawable(item.icon, text.context.theme)
            // Suppress getColor(int) warning since getColor(int, Theme) cannot be used until minSdkVersion is 23.
            @Suppress("DEPRECATION")
            (DrawableCompat.setTint(
                    drawable,
                    text.context.resources.getColor(R.color.blue_medium)
            ))
            DrawableCompat.setTintMode(
                    drawable,
                    SRC_IN
            )
            text.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        } else {
            text.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        text.setText(item.text)
        link.setOnClickListener { item.navigateAction.click() }
    }
}
