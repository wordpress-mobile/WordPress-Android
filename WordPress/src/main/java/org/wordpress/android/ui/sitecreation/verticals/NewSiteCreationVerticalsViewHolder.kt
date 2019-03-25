package org.wordpress.android.ui.sitecreation.verticals

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
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsCustomModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState

sealed class NewSiteCreationVerticalsViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: VerticalsListItemUiState)

    class VerticalsSuggestionItemViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_verticals_suggestion_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val suggestion = itemView.findViewById<TextView>(R.id.suggestion)
        private val divider = itemView.findViewById<View>(R.id.divider)

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsModelUiState
            suggestion.text = uiState.title
            divider.visibility = if (uiState.showDivider) View.VISIBLE else View.GONE
            requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            container.setOnClickListener {
                uiState.onItemTapped!!.invoke()
            }
        }
    }

    class VerticalsSuggestionCustomItemViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_verticals_custom_suggestion_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsCustomModelUiState
            title.text = uiState.title
            subtitle.text = parent.resources.getString(uiState.subTitleResId)
            requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            container.setOnClickListener {
                uiState.onItemTapped!!.invoke()
            }
        }
    }

    class VerticalsErrorViewHolder(
        parentView: ViewGroup
    ) : NewSiteCreationVerticalsViewHolder(parentView, R.layout.new_site_creation_suggestions_error_item) {
        private val text = itemView.findViewById<TextView>(R.id.error_text)
        private val retry = itemView.findViewById<TextView>(R.id.retry)

        init {
            addRetryCompoundDrawable()
        }

        private fun addRetryCompoundDrawable() {
            val drawable = itemView.context.getDrawable(drawable.retry_icon)
            drawable?.setTint(ContextCompat.getColor(itemView.context, color.wp_blue))
            retry.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }

        override fun onBind(uiState: VerticalsListItemUiState) {
            uiState as VerticalsFetchSuggestionsErrorUiState
            text.text = itemView.context.getText(uiState.messageResId)
            retry.text = itemView.context.getText(uiState.retryButtonResId)
            requireNotNull(uiState.onItemTapped) { "OnItemTapped is required." }
            itemView.setOnClickListener {
                uiState.onItemTapped!!.invoke()
            }
        }
    }
}
