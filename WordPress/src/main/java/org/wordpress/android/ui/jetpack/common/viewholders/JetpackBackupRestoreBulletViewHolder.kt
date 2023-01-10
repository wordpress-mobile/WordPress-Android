package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import org.wordpress.android.databinding.JetpackBackupRestoreListBulletItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.BulletState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.image.ImageManager

class JetpackBackupRestoreBulletViewHolder(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<JetpackBackupRestoreListBulletItemBinding>(
    parent,
    JetpackBackupRestoreListBulletItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) = with(binding) {
        val state = itemUiState as BulletState
        val resources = root.context.resources

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
        bulletLabel.text = uiHelpers.getTextOfUiString(itemView.context, state.label)
    }
}
