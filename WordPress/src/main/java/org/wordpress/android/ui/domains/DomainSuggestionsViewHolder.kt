package org.wordpress.android.ui.domains

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse

class DomainSuggestionsViewHolder(
    parent: ViewGroup,
    private val itemSelectionListener: (DomainSuggestionResponse?, Int) -> Unit
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.domain_suggestion_list_item, parent, false)
) {
    private val domainName: TextView = itemView.findViewById(R.id.domain_name)
    private val domainCost: TextView = itemView.findViewById(R.id.domain_cost)
    private val selectionRadioButton: RadioButton = itemView.findViewById(R.id.domain_selection_radio_button)
    private val container: View = itemView.findViewById(R.id.domain_suggestions_container)

    fun bind(
        suggestion: DomainSuggestionResponse,
        position: Int,
        isSelectedPosition: Boolean,
        isDomainCreditAvailable: Boolean
    ) {
        domainName.text = suggestion.domain_name
        domainCost.text = getFormattedCost(suggestion, isDomainCreditAvailable)
        selectionRadioButton.isChecked = isSelectedPosition

        container.setOnClickListener {
            val isSuggestionSelected = !selectionRadioButton.isChecked
            selectionRadioButton.isChecked = isSuggestionSelected
            if (isSuggestionSelected) {
                itemSelectionListener(suggestion, position)
            } else {
                itemSelectionListener(null, -1)
            }
        }
    }

    private fun getFormattedCost(
        suggestion: DomainSuggestionResponse,
        isDomainCreditAvailable: Boolean
    ) = when {
        suggestion.is_free -> {
            suggestion.cost
        }
        isDomainCreditAvailable -> {
            HtmlCompat.fromHtml(
                    String.format(
                            container.context.getString(
                                    R.string.domain_suggestions_list_item_cost_free
                            ),
                            suggestion.cost
                    ),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
        else -> { // on free plan
            HtmlCompat.fromHtml(
                    String.format(
                            container.context.getString(
                                    R.string.domain_suggestions_list_item_cost
                            ),
                            suggestion.cost
                    ),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }
}
