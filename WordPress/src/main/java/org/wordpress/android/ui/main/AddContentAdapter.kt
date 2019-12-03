package org.wordpress.android.ui.main

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.main.MainActionListItem.CreateAction

class AddContentAdapter : Adapter<ActionListItemViewHolder>() {
    private var items: List<MainActionListItem> = listOf()
    fun update(newItems: List<MainActionListItem>) {
        val diffResult = DiffUtil.calculateDiff(
                MainActionDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ActionListItemViewHolder, position: Int) {
        val item = items[position]
        // Currently we have only one ViewHolder type
        holder.bind(item as CreateAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionListItemViewHolder {
        // Currently we have only one ViewHolder type
        return ActionListItemViewHolder(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].actionType.ordinal
    }
}
