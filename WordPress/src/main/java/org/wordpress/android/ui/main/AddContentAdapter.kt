package org.wordpress.android.ui.main

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.WordPress
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.ANSWER_BLOGGING_PROMP
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class AddContentAdapter(context: Context) : Adapter<ActionListItemViewHolder>() {
    private var items: List<MainActionListItem> = listOf()
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers

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

    override fun onBindViewHolder(holder: ActionListItemViewHolder, position: Int) {
        val item = items[position]
        // Currently we have only one ViewHolder type
        holder.bind(item as CreateAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (ActionType.values()[viewType]) {
            ANSWER_BLOGGING_PROMP -> CompactBloggingPromptCardViewHolder(parent, uiHelpers)
            else -> ActionListItemViewHolder(parent, imageManager)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].actionType.ordinal
    }
}
