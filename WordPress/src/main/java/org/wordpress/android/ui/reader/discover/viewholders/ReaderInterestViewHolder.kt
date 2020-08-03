package org.wordpress.android.ui.reader.discover.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.reader_interest_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.utils.UiHelpers

class ReaderInterestViewHolder(
    private val uiHelpers: UiHelpers,
    internal val parent: ViewGroup,
    override val containerView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.reader_interest_item, parent, false)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun onBind(uiState: ReaderInterestUiState) {
        uiHelpers.setTextOrHide(interest, uiState.interest)
        uiHelpers.updateVisibility(divider, uiState.isDividerVisible)
        reader_interest_container.setOnClickListener { uiState.onClicked.invoke(uiState.interest) }
    }
}
