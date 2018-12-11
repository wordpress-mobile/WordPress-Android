package org.wordpress.android.ui.sitecreation.domain

import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState

sealed class NewSiteCreationDomainViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: DomainsListItemUiState)

    class DomainSuggestionItemViewHolder(
        parentView: ViewGroup
            // TODO: Rename the resource
    ) : NewSiteCreationDomainViewHolder(parentView, R.layout.new_site_creation_verticals_suggestion_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val suggestion = itemView.findViewById<TextView>(R.id.suggestion)
        private val divider = itemView.findViewById<View>(R.id.divider)

        override fun onBind(uiState: DomainsListItemUiState) {
            uiState as DomainsModelUiState
            suggestion.text = uiState.name
            divider.visibility = if (uiState.showDivider) View.VISIBLE else View.GONE
            requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            container.setOnClickListener {
                uiState.onItemTapped!!.invoke()
            }
        }
    }

    class DomainSuggestionErrorViewHolder(
        parentView: ViewGroup
            // TODO: Rename the resource
    ) : NewSiteCreationDomainViewHolder(parentView, R.layout.new_site_creation_verticals_error_item) {
        private val text = itemView.findViewById<TextView>(R.id.error_text)
        private val retry = itemView.findViewById<TextView>(R.id.retry)

        init {
            addRetryCompoundDrawable()
        }

        private fun addRetryCompoundDrawable() {
            val drawable = itemView.context.getDrawable(drawable.retry_icon)
            drawable.setTint(ContextCompat.getColor(itemView.context, color.wp_blue))
            retry.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }

        override fun onBind(uiState: DomainsListItemUiState) {
            uiState as DomainsFetchSuggestionsErrorUiState
            text.text = itemView.context.getText(uiState.messageResId)
            retry.text = itemView.context.getText(uiState.retryButtonResId)
            requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            itemView.setOnClickListener {
                uiState.onItemTapped!!.invoke()
            }
        }
    }
}
