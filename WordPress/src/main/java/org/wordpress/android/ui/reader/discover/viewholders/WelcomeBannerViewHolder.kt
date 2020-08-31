package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.reader_cardview_welcome_banner.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState

class WelcomeBannerViewHolder(
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_cardview_welcome_banner) {
    override fun onBind(uiState: ReaderCardUiState) {
        val state = uiState as ReaderWelcomeBannerCardUiState
        welcome_title.setText(state.titleRes)
    }
}
