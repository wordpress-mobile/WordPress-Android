package org.wordpress.android.ui.reader.subfilter.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getColorFromAttribute

open class SubfilterListItemViewHolder(
    internal val parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    open fun bind(filter: SubfilterListItem, uiHelpers: UiHelpers) {
        val selectedTick: ImageView? = this.itemView.findViewById(R.id.item_selected)
        selectedTick?.let {
            it.visibility = if (filter.isSelected) View.VISIBLE else View.GONE
        }
        val itemText: TextView? = this.itemView.findViewById(R.id.item_title)
        itemText?.let {
            it.setTextColor(
                    parent.context.getColorFromAttribute(
                            if (filter.isSelected) {
                                R.attr.colorPrimary
                            } else {
                                R.attr.colorOnSurface
                            }
                    )
            )
        }

        this.itemView.setOnClickListener {
            filter.onClickAction?.invoke(filter)
        }
    }
}
