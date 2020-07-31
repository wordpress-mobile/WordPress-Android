package org.wordpress.android.ui.reader.discover.viewholders

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.reader_interest_card.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestCardUiState
import org.wordpress.android.ui.reader.discover.ReaderInterestAdapter
import org.wordpress.android.ui.stats.refresh.utils.WrappingLinearLayoutManager
import org.wordpress.android.ui.utils.UiHelpers

class ReaderInterestCardViewHolder(
    private val uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_interest_card) {
    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderInterestCardUiState

        interests_list.isNestedScrollingEnabled = false
        if (interests_list.adapter == null) {
            val readerInterestAdapter = ReaderInterestAdapter(uiHelpers)
            val layoutManager = WrappingLinearLayoutManager(
                    interests_list.context,
                    LinearLayoutManager.VERTICAL,
                    false
            )
            interests_list.adapter = readerInterestAdapter
            interests_list.layoutManager = layoutManager
        }
        (interests_list.layoutManager as WrappingLinearLayoutManager).init()
        (interests_list.adapter as ReaderInterestAdapter).update(uiState.interest)
    }
}
