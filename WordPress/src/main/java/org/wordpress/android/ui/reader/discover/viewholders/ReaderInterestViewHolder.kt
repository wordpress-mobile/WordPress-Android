package org.wordpress.android.ui.reader.discover.viewholders

import android.content.Context
import android.view.ViewGroup
import kotlinx.android.synthetic.main.reader_cardview_post.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ReaderInterestViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_interest_post) {
    val viewContext: Context = post_container.context
    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderInterestUiState
    }
}
