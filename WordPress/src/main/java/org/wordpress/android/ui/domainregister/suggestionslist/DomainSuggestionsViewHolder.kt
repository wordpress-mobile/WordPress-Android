package org.wordpress.android.ui.domainregister.suggestionslist

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
    private val itemSelectionListener: (DomainSuggestionResponse?, Int) -> Unit
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.domain_suggestion_list_item, parent, false)) {
    private val domainName: AppCompatTextView = itemView.findViewById(R.id.domainSuggestionsName)
    private val selectionRadioButton: AppCompatRadioButton = itemView.findViewById(R.id.domainSelectionRadioButton)
    private val container: View = itemView.findViewById(R.id.domainSuggestionContainer)

    fun bind(suggestion: DomainSuggestionResponse, position: Int, isSelectedPosition: Boolean) {
        domainName.text = suggestion.domain_name
        selectionRadioButton.isChecked = isSelectedPosition
        itemView.tag = suggestion

        container.setOnClickListener {
            val isSuggestionSelected = !selectionRadioButton.isChecked
            selectionRadioButton.isChecked = isSuggestionSelected
            if (isSuggestionSelected) {
                itemSelectionListener(itemView.tag as DomainSuggestionResponse?, position)
            } else {
                itemSelectionListener(null, -1)
            }
        }
    }
}
