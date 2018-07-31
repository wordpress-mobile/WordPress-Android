package org.wordpress.android.ui.registerdomain.suggestionslist

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse

class DomainSuggestionsViewHolder(
    parent: ViewGroup,
    private val itemSelectionListener: (DomainSuggestionResponse) -> Unit
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.domain_suggestion_list_item, parent, false)) {
    fun bind(suggestion: DomainSuggestionResponse) {
    }
}
