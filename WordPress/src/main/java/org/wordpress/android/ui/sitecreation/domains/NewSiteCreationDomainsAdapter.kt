package org.wordpress.android.ui.sitecreation.domains

import android.support.annotation.MainThread
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainViewHolder.DomainSuggestionErrorViewHolder
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainViewHolder.DomainSuggestionItemViewHolder
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsListItemUiState
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState

private const val suggestionItemViewType: Int = 1
private const val suggestionErrorViewType: Int = 2

class NewSiteCreationDomainsAdapter : Adapter<NewSiteCreationDomainViewHolder>() {
    private val items = mutableListOf<DomainsListItemUiState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewSiteCreationDomainViewHolder {
        return when (viewType) {
            suggestionItemViewType -> DomainSuggestionItemViewHolder(parent)
            suggestionErrorViewType -> DomainSuggestionErrorViewHolder(parent)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: NewSiteCreationDomainViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    @MainThread
    fun update(newItems: List<DomainsListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(DomainsDiffUtils(items.toList(), newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DomainsModelUiState -> suggestionItemViewType
            is DomainsFetchSuggestionsErrorUiState -> suggestionErrorViewType
        }
    }

    private class DomainsDiffUtils(
        val oldItems: List<DomainsListItemUiState>,
        val newItems: List<DomainsListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is DomainsFetchSuggestionsErrorUiState -> true
                is DomainsModelUiState -> oldItem.name == (newItem as DomainsModelUiState).name
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
