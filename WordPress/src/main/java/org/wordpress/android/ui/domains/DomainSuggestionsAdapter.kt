package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter

class DomainSuggestionsAdapter(
    private val itemSelectionListener: (DomainSuggestionItem?, Int) -> Unit
) : Adapter<DomainSuggestionsViewHolder>() {
    private val list = mutableListOf<DomainSuggestionItem>()
    var selectedPosition = -1
    var isSiteDomainsFeatureEnabled: Boolean = false
    var isDomainCreditAvailable: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DomainSuggestionsViewHolder {
        return DomainSuggestionsViewHolder(
                parent,
                this::onDomainSuggestionSelected
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: DomainSuggestionsViewHolder, position: Int) {
        holder.bind(
                list[position],
                position,
                selectedPosition == position,
                isSiteDomainsFeatureEnabled,
                isDomainCreditAvailable)
    }

    private fun onDomainSuggestionSelected(suggestion: DomainSuggestionItem?, position: Int) {
        val previousSelectedPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelectedPosition)
        if (previousSelectedPosition != selectedPosition) {
            notifyItemChanged(selectedPosition)
        }
        itemSelectionListener(suggestion, position)
    }

    internal fun updateSuggestionsList(items: List<DomainSuggestionItem>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    internal fun updateDomainCreditAvailable(siteDomainsFeatureEnabled: Boolean, isDomainCreditAvailable: Boolean) {
        this.isSiteDomainsFeatureEnabled = siteDomainsFeatureEnabled
        this.isDomainCreditAvailable = isDomainCreditAvailable
    }
}
