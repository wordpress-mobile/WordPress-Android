package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ReaderCardviewAnnouncementBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.util.extensions.viewBinding

class ReaderAnnouncementCardViewHolder(
    parentView: ViewGroup,
) : ReaderViewHolder<ReaderCardviewAnnouncementBinding>(
    parentView.viewBinding(ReaderCardviewAnnouncementBinding::inflate)
) {
    override fun onBind(uiState: ReaderCardUiState) {
        (uiState as? ReaderCardUiState.ReaderAnnouncementCardUiState)?.let { state ->
            with(binding.root) {
                setItems(state.items)
                setOnDoneClickListener(state.onDoneClick)
            }
        }
    }
}
