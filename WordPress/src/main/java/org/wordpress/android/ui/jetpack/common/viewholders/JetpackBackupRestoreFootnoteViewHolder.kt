package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_footnote_item.*
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_footnote_item.icon
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class JetpackBackupRestoreFootnoteViewHolder(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_backup_restore_list_footnote_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as FootnoteState
        val resources = itemView.context.resources

        footnote.text = uiHelpers.getTextOfUiString(itemView.context, state.text)
        footnote.visibility = if (state.isVisible) View.VISIBLE else View.GONE

        state.textAlphaResId?.let {
            footnote.setTextColor(footnote.textColors.withAlpha(state.textAlphaResId))
        }

        state.iconSizeResId?.let {
            with(icon.layoutParams) {
                val size = resources.getDimensionPixelSize(state.iconSizeResId)
                width = size
                height = size
            }
        }

        state.iconRes?.let {
            imageManager.load(icon, it)
            icon.visibility = if (state.isVisible) View.VISIBLE else View.GONE
        }
    }
}
