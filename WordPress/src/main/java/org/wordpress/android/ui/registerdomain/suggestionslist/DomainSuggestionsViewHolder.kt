package org.wordpress.android.ui.registerdomain.suggestionslist

import android.support.v7.widget.AppCompatRadioButton
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse

class DomainSuggestionsViewHolder(
    parent: ViewGroup,
    private val itemSelectionListener: (DomainSuggestionResponse) -> Unit
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.domain_suggestion_list_item, parent, false)) {
    private val domainName: AppCompatTextView = itemView.findViewById(R.id.domainSuggestionsName)
    private val selectionRadioButton: AppCompatRadioButton = itemView.findViewById(R.id.domainSelectionRadioButton)
    private val container: View = itemView.findViewById(R.id.domainSuggestionsName)

    fun bind(suggestion: DomainSuggestionResponse) {
        domainName.text = suggestion.domain_name
        container.setOnClickListener {
            selectionRadioButton.isSelected = true
            itemSelectionListener(suggestion)
        }
    }
}
