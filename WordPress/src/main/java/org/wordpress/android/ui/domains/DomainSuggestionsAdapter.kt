package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class DomainSuggestionsAdapter(
    private val itemSelectionListener: (DomainSuggestionItem?) -> Unit
) : ListAdapter<DomainSuggestionItem, DomainSuggestionsViewHolder>(DomainSuggestionItemDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DomainSuggestionsViewHolder(parent, itemSelectionListener)

    override fun onBindViewHolder(holder: DomainSuggestionsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DomainSuggestionItemDiffCallback : DiffUtil.ItemCallback<DomainSuggestionItem>() {
        override fun areItemsTheSame(old: DomainSuggestionItem, new: DomainSuggestionItem) =
            old.domainName == new.domainName

        override fun areContentsTheSame(old: DomainSuggestionItem, new: DomainSuggestionItem) = old == new
    }
}
