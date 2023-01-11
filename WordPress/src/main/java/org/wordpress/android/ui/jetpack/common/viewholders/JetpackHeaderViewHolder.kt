package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import org.wordpress.android.databinding.JetpackListHeaderItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getColorResIdFromAttribute

class JetpackHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<JetpackListHeaderItemBinding>(
    parent,
    JetpackListHeaderItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerState = itemUiState as HeaderState
        val context = itemView.context

        binding.header.text = uiHelpers.getTextOfUiString(context, headerState.text)
        val textColorRes = context.getColorResIdFromAttribute(headerState.textColorRes)
        binding.header.setTextColor(ContextCompat.getColor(context, textColorRes))
    }
}
