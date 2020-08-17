package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.reader_cardview_welcome_banner.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.ui.utils.UiHelpers

class WelcomeBannerViewHolder(
    private val uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_cardview_welcome_banner) {
    override fun onBind(uiState: ReaderCardUiState) {
        if (uiState is ReaderWelcomeBannerCardUiState) {
            uiHelpers.updateVisibility(welcome_banner_wrapper, uiState.show)
        }
    }
}
