package org.wordpress.android.ui.sitecreation.errors

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.site_creation_error_with_retry.view.*
import org.wordpress.android.R

sealed class SiteCreationErrorUiState(val isVisible: Boolean) {
    open val showSubtitle = false
    open val titleResId: Int = R.string.empty
    open val subtitleResId: Int = R.string.empty

    object Hidden : SiteCreationErrorUiState(false)
    object GenericError : SiteCreationErrorUiState(true) {
        override val showSubtitle = true
        override val titleResId: Int = R.string.site_creation_error_generic_title
        override val subtitleResId: Int = R.string.site_creation_error_generic_subtitle
    }

    object ConnectionError : SiteCreationErrorUiState(true) {
        override val titleResId: Int = R.string.site_creation_error_connection_title
    }
}

fun updateSiteCreationErrorLayout(layout: ViewGroup, state: SiteCreationErrorUiState) {
    layout.error_subtitle.visibility = if (state.showSubtitle) View.VISIBLE else View.GONE
    layout.error_title.text = layout.resources.getString(state.titleResId)
    layout.error_subtitle.text = layout.resources.getString(state.subtitleResId)
}
