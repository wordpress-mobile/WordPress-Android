package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.reader_interest_card.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderInterestAdapter
import org.wordpress.android.ui.utils.UiHelpers

class ReaderInterestsCardViewHolder(
    uiHelpers: UiHelpers,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_interest_card) {
    init {
        if (interests_list.adapter == null) {
            interests_list.layoutManager = LinearLayoutManager(interests_list.context, RecyclerView.VERTICAL, false)
            val readerInterestAdapter = ReaderInterestAdapter(uiHelpers)
            interests_list.adapter = readerInterestAdapter
        }
    }

    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderInterestsCardUiState
        (interests_list.adapter as ReaderInterestAdapter).update(uiState.interest)
    }
}
