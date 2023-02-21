package org.wordpress.android.ui.sitecreation.domains

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationDomainsItemBinding
import org.wordpress.android.databinding.SiteCreationSuggestionsErrorItemBinding
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState.DomainsModelAvailableUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

sealed class SiteCreationDomainViewHolder<T : ViewBinding>(protected val binding: T) :
    RecyclerView.ViewHolder(binding.root) {

    class DomainSuggestionItemViewHolder(
        parentView: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : SiteCreationDomainViewHolder<SiteCreationDomainsItemBinding>(
        parentView.viewBinding(SiteCreationDomainsItemBinding::inflate)
    ) {
        fun onBind(uiState: DomainsModelUiState) = with(binding) {
            nameSuggestion.text = uiState.name
            domainSuggestion.text = uiState.domain
            domainSuggestionRadioButton.apply {
                isChecked = uiState.checked
                isInvisible = !uiState.radioButtonVisibility
                buttonTintList = ContextCompat.getColorStateList(context, R.color.neutral_10_primary_40_selector)
            }
            container.apply {
                val onClick = (uiState as? DomainsModelAvailableUiState)?.onClick
                isEnabled = onClick != null
                setOnClickListener { onClick?.invoke() }
            }
            uiHelpers.setTextOrHide(domainUnavailability, uiState.subTitle)
        }
    }

    class DomainSuggestionErrorViewHolder(
        parentView: ViewGroup
    ) : SiteCreationDomainViewHolder<SiteCreationSuggestionsErrorItemBinding>(
        parentView.viewBinding(SiteCreationSuggestionsErrorItemBinding::inflate)
    ) {
        fun onBind(uiState: DomainsFetchSuggestionsErrorUiState) = with(binding) {
            errorText.text = root.context.getText(uiState.messageResId)
            retry.apply {
                text = context.getText(uiState.retryButtonResId)
                val drawable = ContextCompat.getDrawable(context, R.drawable.retry_icon)?.apply {
                    setTint(ContextCompat.getColor(context, R.color.primary))
                }
                setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }
            container.setOnClickListener { uiState.onClick.invoke() }
        }
    }
}
