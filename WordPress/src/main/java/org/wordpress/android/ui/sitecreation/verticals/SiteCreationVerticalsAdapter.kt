package org.wordpress.android.ui.sitecreation.verticals

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewHolder.VerticalsErrorViewHolder
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewHolder.VerticalsSuggestionCustomItemViewHolder
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewHolder.VerticalsSuggestionItemViewHolder
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsCustomModelUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState

private const val suggestionItemViewType: Int = 1
private const val suggestionCustomItemViewType: Int = 2
private const val suggestionErrorViewType: Int = 3

class SiteCreationVerticalsAdapter : Adapter<SiteCreationVerticalsViewHolder>() {
    private val items = mutableListOf<VerticalsListItemUiState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteCreationVerticalsViewHolder {
        return when (viewType) {
            suggestionItemViewType -> VerticalsSuggestionItemViewHolder(parent)
            suggestionCustomItemViewType -> VerticalsSuggestionCustomItemViewHolder(parent)
            suggestionErrorViewType -> VerticalsErrorViewHolder(parent)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SiteCreationVerticalsViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<VerticalsListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(VerticalsDiffUtils(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is VerticalsModelUiState -> suggestionItemViewType
            is VerticalsCustomModelUiState -> suggestionCustomItemViewType
            is VerticalsFetchSuggestionsErrorUiState -> suggestionErrorViewType
        }
    }

    private class VerticalsDiffUtils(
        val oldItems: List<VerticalsListItemUiState>,
        val newItems: List<VerticalsListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is VerticalsFetchSuggestionsErrorUiState -> true
                is VerticalsModelUiState -> oldItem.id == (newItem as VerticalsModelUiState).id
                is VerticalsCustomModelUiState -> oldItem.id == (newItem as VerticalsCustomModelUiState).id
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
