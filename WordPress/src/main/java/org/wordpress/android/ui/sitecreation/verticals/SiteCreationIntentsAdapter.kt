package org.wordpress.android.ui.sitecreation.verticals

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentViewHolder.DefaultIntentItemViewHolder
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState.DefaultIntentItemUiState
import org.wordpress.android.ui.utils.UiHelpers

class SiteCreationIntentsAdapter(private val uiHelpers: UiHelpers) : Adapter<SiteCreationIntentViewHolder>() {
    private val items = mutableListOf<IntentListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SiteCreationIntentViewHolder {
        return when (viewType) {
            defaultSuggestionItemViewType -> DefaultIntentItemViewHolder(parent, uiHelpers)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SiteCreationIntentViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    @MainThread
    fun update(newItems: List<IntentListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(IntentsDiffUtils(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        // this will have more types later
        return when (items[position]) {
            is DefaultIntentItemUiState -> defaultSuggestionItemViewType
        }
    }

    private class IntentsDiffUtils(
        val oldItems: List<IntentListItemUiState>,
        val newItems: List<IntentListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is DefaultIntentItemUiState ->
                    oldItem.verticalSlug == (newItem as DefaultIntentItemUiState).verticalSlug
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }

    companion object {
        private const val defaultSuggestionItemViewType: Int = 1
    }
}
