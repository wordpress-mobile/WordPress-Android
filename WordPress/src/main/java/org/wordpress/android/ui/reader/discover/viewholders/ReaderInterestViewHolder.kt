package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ReaderInterestItemBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class ReaderInterestViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup,
    private val binding: ReaderInterestItemBinding = parent.viewBinding(ReaderInterestItemBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(uiState: ReaderInterestUiState) = with(binding) {
        uiHelpers.setTextOrHide(interest, uiState.interest)
        interest.setOnClickListener { uiState.onClicked.invoke(uiState.interest) }

        with(uiState.chipStyle) {
            interest.setChipStrokeColorResource(chipStrokeColorResId)
            interest.setChipBackgroundColorResource(chipFillColorResId)
            interest.setTextColor(getColorStateList(interest.context, chipFontColorResId))
        }
    }
}
