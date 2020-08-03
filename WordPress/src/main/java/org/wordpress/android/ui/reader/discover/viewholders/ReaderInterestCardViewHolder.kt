package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.reader_interest_card.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestCardUiState
import org.wordpress.android.ui.reader.discover.ReaderInterestAdapter
import org.wordpress.android.ui.utils.UiHelpers

class ReaderInterestCardViewHolder(
    private val uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_interest_card) {
    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderInterestCardUiState

        if (interests_list.adapter == null) {
            interests_list.layoutManager = LinearLayoutManager(interests_list.context, RecyclerView.VERTICAL, false)
            val readerInterestAdapter = ReaderInterestAdapter(uiHelpers)
            interests_list.adapter = readerInterestAdapter
        }
        (interests_list.adapter as ReaderInterestAdapter).update(uiState.interest)
    }
}
