package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_subheader_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.SubHeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.utils.UiHelpers

class JetpackBackupRestoreSubHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_backup_restore_list_subheader_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val subHeaderItemState = itemUiState as SubHeaderState
        val resources = itemView.context.resources

        with(itemView.layoutParams as MarginLayoutParams) {
            subHeaderItemState.itemTopMarginResId?.let {
                val margin = resources.getDimensionPixelSize(it)
                topMargin = margin
            }
            subHeaderItemState.itemBottomMarginResId?.let {
                val margin = resources.getDimensionPixelSize(it)
                bottomMargin = margin
            }
        }

        subheader.text = uiHelpers.getTextOfUiString(itemView.context, subHeaderItemState.text)
    }
}
