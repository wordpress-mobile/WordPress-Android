package org.wordpress.android.ui.reader.subfilter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.subfilter_list_item.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

open class SubfilterListItemViewHolder(
    internal val parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    open fun bind(filter: SubfilterListItem, uiHelpers: UiHelpers) {
        val selectedTick: ImageView? = this.itemView.findViewById(R.id.item_selected)
        selectedTick?.let {
            it.visibility = if (filter.isSelected) View.VISIBLE else View.GONE
        }

        this.itemView.setOnClickListener{
            filter.onClickAction?.invoke(filter)
        }
    }
}
