package org.wordpress.android.ui.reader.discover.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.utils.UiHelpers

class ReaderInterestViewHolder(
    private val uiHelpers: UiHelpers,
    internal val parent: ViewGroup
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.reader_interest_item,
                parent,
                false
        )
) {
    fun onBind(uiState: ReaderInterestUiState) {
        val interestTextView = this.itemView.findViewById<TextView>(R.id.interest)
        val dividerView = this.itemView.findViewById<View>(R.id.divider)
        uiHelpers.setTextOrHide(interestTextView, uiState.interest)
        uiHelpers.updateVisibility(dividerView, uiState.isDividerVisible)
    }
}
