package org.wordpress.android.ui.sitecreation.domains

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationDomainsItemBinding
import org.wordpress.android.databinding.SiteCreationSuggestionsErrorItemBinding
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

sealed class SiteCreationDomainViewHolder<T : ViewBinding>(protected val binding: T) :
    RecyclerView.ViewHolder(binding.root) {

    abstract fun onBind(uiState: DomainsListItemUiState)

    class DomainSuggestionItemViewHolder(
        parentView: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : SiteCreationDomainViewHolder<SiteCreationDomainsItemBinding>(
        parentView.viewBinding(SiteCreationDomainsItemBinding::inflate)
    ) {
        override fun onBind(uiState: DomainsListItemUiState) = with(binding) {
            uiState as DomainsModelUiState
            nameSuggestion.text = uiState.name
            domainSuggestion.text = uiState.domain
            domainSuggestionRadioButton.isChecked = uiState.checked
            domainSuggestionRadioButton.visibility = if (uiState.radioButtonVisibility) View.VISIBLE else View.INVISIBLE
            domainSuggestionRadioButton.buttonTintList = ContextCompat.getColorStateList(
                root.context,
                R.color.neutral_10_primary_40_selector
            )
            container.isEnabled = uiState.onClick != null
            binding.container.setOnClickListener {
                uiState.onClick?.invoke()
            }
            uiHelpers.setTextOrHide(domainUnavailability, uiState.subTitle)
        }
    }

    class DomainSuggestionErrorViewHolder(
        parentView: ViewGroup
    ) : SiteCreationDomainViewHolder<SiteCreationSuggestionsErrorItemBinding>(
        parentView.viewBinding(SiteCreationSuggestionsErrorItemBinding::inflate)
    ) {
        init {
            binding.addRetryCompoundDrawable()
        }

        private fun SiteCreationSuggestionsErrorItemBinding.addRetryCompoundDrawable() {
            ContextCompat.getDrawable(root.context, R.drawable.retry_icon)?.let { drawable ->
                drawable.setTint(ContextCompat.getColor(root.context, R.color.primary))
                retry.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }
        }

        override fun onBind(uiState: DomainsListItemUiState) = with(binding) {
            uiState as DomainsFetchSuggestionsErrorUiState
            errorText.text = itemView.context.getText(uiState.messageResId)
            retry.text = itemView.context.getText(uiState.retryButtonResId)
            requireNotNull(uiState.onClick) { "OnItemTapped is required." }
            itemView.setOnClickListener {
                uiState.onClick.invoke()
            }
        }
    }
}
