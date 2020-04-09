package org.wordpress.android.ui.posts

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class PrepublishingActionsAdapter(context: Context) : RecyclerView.Adapter<PrepublishingActionsListItemViewHolder>() {
    private var items: List<PrepublishingActionItemUiState> = listOf()
    @Inject lateinit var uiHelpers: UiHelpers

    init {
        (context.applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrepublishingActionsListItemViewHolder {
        return PrepublishingActionsListItemViewHolder(parent, uiHelpers)
    }

    fun update(newItems: List<PrepublishingActionItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(
                PrepublishingActionsDiffCallback(
                        this.items,
                        newItems
                )
        )
        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PrepublishingActionsListItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
}
