package org.wordpress.android.ui.quickstart.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.QuickStartListHeaderItemBinding
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard.QuickStartHeaderCard
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.extensions.viewBinding

class HeaderViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val binding: QuickStartListHeaderItemBinding = parent.viewBinding(QuickStartListHeaderItemBinding::inflate)
) : ViewHolder(binding.root) {
    fun bind(card: QuickStartHeaderCard) {
        uiHelpers.setTextOrHide(binding.headerTitle, card.title)
        binding.headerImage.setVisible(card.shouldShowHeaderImage)
    }
}
