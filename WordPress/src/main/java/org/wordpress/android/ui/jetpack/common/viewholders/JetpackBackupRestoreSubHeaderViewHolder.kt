package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import org.wordpress.android.databinding.JetpackBackupRestoreListSubheaderItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.SubHeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.utils.UiHelpers

class JetpackBackupRestoreSubHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<JetpackBackupRestoreListSubheaderItemBinding>(
    parent,
    JetpackBackupRestoreListSubheaderItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) = with(binding) {
        val subHeaderItemState = itemUiState as SubHeaderState
        val resources = root.context.resources

        with(root.layoutParams as MarginLayoutParams) {
            subHeaderItemState.itemTopMarginResId?.let {
                val margin = resources.getDimensionPixelSize(it)
                topMargin = margin
            }
            subHeaderItemState.itemBottomMarginResId?.let {
                val margin = resources.getDimensionPixelSize(it)
                bottomMargin = margin
            }
        }

        subheader.text = uiHelpers.getTextOfUiString(root.context, subHeaderItemState.text)
    }
}
