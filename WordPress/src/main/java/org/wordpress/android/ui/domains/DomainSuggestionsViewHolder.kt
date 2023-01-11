package org.wordpress.android.ui.domains

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R

class DomainSuggestionsViewHolder(
    parent: ViewGroup,
    private val itemSelectionListener: (DomainSuggestionItem?) -> Unit
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.domain_suggestion_list_item, parent, false)
) {
    private val domainName: TextView = itemView.findViewById(R.id.domain_name)
    private val domainCost: TextView = itemView.findViewById(R.id.domain_cost)
    private val selectionRadioButton: RadioButton = itemView.findViewById(R.id.domain_selection_radio_button)
    private val container: View = itemView.findViewById(R.id.domain_suggestions_container)

    fun bind(suggestion: DomainSuggestionItem) {
        domainName.text = buildSpannedString {
            val tld = suggestion.domainName.split('.').last()
            append(suggestion.domainName.removeSuffix(tld))
            bold { append(tld) }
        }
        domainCost.isVisible = suggestion.isCostVisible
        domainCost.text = getFormattedCost(suggestion)
        selectionRadioButton.isChecked = suggestion.isSelected
        selectionRadioButton.isEnabled = suggestion.isEnabled

        if (suggestion.isEnabled) {
            container.setOnClickListener {
                val isSuggestionSelected = !selectionRadioButton.isChecked
                selectionRadioButton.isChecked = isSuggestionSelected
                if (isSuggestionSelected) {
                    itemSelectionListener(suggestion)
                } else {
                    itemSelectionListener(null)
                }
            }
        } else {
            container.isClickable = false
        }
    }

    private fun getFormattedCost(suggestion: DomainSuggestionItem) = when {
        suggestion.isFree -> suggestion.cost
        suggestion.isOnSale -> {
            HtmlCompat.fromHtml(
                String.format(
                    container.context.getString(R.string.domain_suggestions_list_item_cost_on_sale),
                    suggestion.saleCost,
                    suggestion.cost
                ),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
        suggestion.isFreeWithCredits -> {
            HtmlCompat.fromHtml(
                String.format(
                    container.context.getString(R.string.domain_suggestions_list_item_cost_free),
                    suggestion.cost
                ),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
        else -> { // on free plan
            HtmlCompat.fromHtml(
                String.format(
                    container.context.getString(R.string.domain_suggestions_list_item_cost),
                    suggestion.cost
                ),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }
}
