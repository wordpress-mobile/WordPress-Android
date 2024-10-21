package org.wordpress.android.ui.sitecreation.domains

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationDomainsItemBinding
import org.wordpress.android.databinding.SiteCreationDomainsItemV2Binding
import org.wordpress.android.databinding.SiteCreationSuggestionsErrorItemBinding
import org.wordpress.android.ui.compose.theme.AppThemeM2WithoutBackground
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Old
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Old.DomainUiState.AvailableDomain
import org.wordpress.android.ui.sitecreation.domains.compose.DomainItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.editor.R as EditorR

sealed class SiteCreationDomainViewHolder<T : ViewBinding>(protected val binding: T) :
    RecyclerView.ViewHolder(binding.root) {
    class OldDomainViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
        SiteCreationDomainViewHolder<SiteCreationDomainsItemBinding>(
            parentView.viewBinding(SiteCreationDomainsItemBinding::inflate)
        ) {
        fun onBind(uiState: Old.DomainUiState) = with(binding) {
            nameSuggestion.text = uiState.name
            domainSuggestion.text = uiState.domain
            domainSuggestionRadioButton.apply {
                isChecked = uiState.checked
                isInvisible = !uiState.radioButtonVisibility
                buttonTintList = ContextCompat.getColorStateList(context, R.color.neutral_10_primary_40_selector)
            }
            container.apply {
                val onClick = (uiState as? AvailableDomain)?.onClick
                isEnabled = onClick != null
                setOnClickListener { onClick?.invoke() }
            }
            uiHelpers.setTextOrHide(domainUnavailability, uiState.subTitle)
        }
    }

    class OldDomainErrorViewHolder(parentView: ViewGroup) :
        SiteCreationDomainViewHolder<SiteCreationSuggestionsErrorItemBinding>(
            parentView.viewBinding(SiteCreationSuggestionsErrorItemBinding::inflate)
        ) {
        fun onBind(uiState: Old.ErrorItemUiState) = with(binding) {
            errorText.text = root.context.getText(uiState.messageResId)
            retry.apply {
                text = context.getText(uiState.retryButtonResId)
                val drawable = ContextCompat.getDrawable(context, EditorR.drawable.retry_icon)?.apply {
                    setTint(ContextCompat.getColor(context, R.color.primary))
                }
                setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }
            container.setOnClickListener { uiState.onClick.invoke() }
        }
    }

    class NewDomainViewHolder(parentView: ViewGroup) :
        SiteCreationDomainViewHolder<SiteCreationDomainsItemV2Binding>(
            parentView.viewBinding(SiteCreationDomainsItemV2Binding::inflate)
        ) {
        val composeView = binding.composeView

        fun onBind(uiState: New.DomainUiState) = with(binding) {
            composeView.setContent {
                AppThemeM2WithoutBackground {
                    DomainItem(uiState)
                }
            }
        }
    }
}
