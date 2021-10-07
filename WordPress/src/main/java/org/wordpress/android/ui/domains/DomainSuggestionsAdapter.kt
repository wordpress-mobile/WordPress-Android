package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse

class DomainSuggestionsAdapter(
    private val itemSelectionListener: (DomainSuggestionResponse?, Int) -> Unit
) : Adapter<DomainSuggestionsViewHolder>() {
    private val list = mutableListOf<DomainSuggestionResponse>()
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

    private fun onDomainSuggestionSelected(suggestion: DomainSuggestionResponse?, position: Int) {
        val previousSelectedPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelectedPosition)
        if (previousSelectedPosition != selectedPosition) {
            notifyItemChanged(selectedPosition)
        }
        itemSelectionListener(suggestion, position)
    }

    internal fun updateSuggestionsList(items: List<DomainSuggestionResponse>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    internal fun updateDomainCreditAvailable(siteDomainsFeatureEnabled: Boolean, isDomainCreditAvailable: Boolean) {
        this.isSiteDomainsFeatureEnabled = siteDomainsFeatureEnabled
        this.isDomainCreditAvailable = isDomainCreditAvailable
    }
}
