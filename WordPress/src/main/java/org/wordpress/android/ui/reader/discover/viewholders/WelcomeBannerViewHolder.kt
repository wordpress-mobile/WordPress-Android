package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ReaderCardviewWelcomeBannerBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.util.extensions.viewBinding

class WelcomeBannerViewHolder(
    parentView: ViewGroup
) : ReaderViewHolder<ReaderCardviewWelcomeBannerBinding>(
    parentView.viewBinding(ReaderCardviewWelcomeBannerBinding::inflate)
) {
    override fun onBind(uiState: ReaderCardUiState) = with(binding) {
        val state = uiState as ReaderWelcomeBannerCardUiState
        welcomeTitle.setText(state.titleRes)
    }
}
