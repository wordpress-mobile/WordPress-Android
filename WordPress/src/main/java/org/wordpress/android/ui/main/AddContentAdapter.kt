package org.wordpress.android.ui.main

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

const val SHOW_QUICK_START_PAYLOAD = "SHOW_QUICK_START_PAYLOAD"

class AddContentAdapter(context: Context) : Adapter<ActionListItemViewHolder>() {
    private var items: List<MainActionListItem> = listOf()
    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

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

    override fun onBindViewHolder(holder: ActionListItemViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        // Currently we have only one ViewHolder type
        holder.bind(item as CreateAction, payloads.contains(SHOW_QUICK_START_PAYLOAD))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionListItemViewHolder {
        // Currently we have only one ViewHolder type
        return ActionListItemViewHolder(parent, imageManager)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].actionType.ordinal
    }

    fun getItemPositionByActionType(actionType: ActionType): Int {
        return items.indexOfFirst { it.actionType == actionType }
    }

    override fun onBindViewHolder(holder: ActionListItemViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}
