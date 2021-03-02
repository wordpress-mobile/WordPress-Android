package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.reader_discover_sorting_button.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderSortingTypeUiState
import org.wordpress.android.ui.utils.UiHelpers

class ReaderSortingViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : ReaderViewHolder(parent, R.layout.reader_discover_sorting_button) {
    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderSortingTypeUiState
        uiHelpers.setTextOrHide(discover_sorting_btn, uiState.title)
        uiHelpers.setImageOrHide(discover_sorting_icon, uiState.icon)
        itemView.setOnClickListener {
            uiState.onFilterClicked()
        }
    }
}
