package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_bullet_item.*
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_bullet_item.icon
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.BulletState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.image.ImageManager

class JetpackBackupRestoreBulletViewHolder(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_backup_restore_list_bullet_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as BulletState
        val resources = itemView.context.resources

        with(icon.layoutParams) {
            val size = resources.getDimensionPixelSize(state.sizeResId)
            width = size
            height = size
        }

        state.itemBottomMarginResId?.let {
            with(itemView.layoutParams as MarginLayoutParams) {
                val margin = resources.getDimensionPixelSize(it)
                bottomMargin = margin
            }
        }

        if (state.colorResId == null) {
            imageManager.load(icon, state.icon)
        } else {
            ColorUtils.setImageResourceWithTint(
                    icon,
                    state.icon,
                    state.colorResId
            )
        }
        bullet_label.text = uiHelpers.getTextOfUiString(itemView.context, state.label)
    }
}
