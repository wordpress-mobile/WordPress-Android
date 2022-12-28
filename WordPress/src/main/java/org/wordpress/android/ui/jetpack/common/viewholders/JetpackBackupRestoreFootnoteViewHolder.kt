package org.wordpress.android.ui.jetpack.common.viewholders

import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.JetpackBackupRestoreListFootnoteItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.image.ImageManager

class JetpackBackupRestoreFootnoteViewHolder(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<JetpackBackupRestoreListFootnoteItemBinding>(
    parent,
    JetpackBackupRestoreListFootnoteItemBinding::inflate
) {
    init {
        with(binding.footnote) {
            linksClickable = true
            isClickable = true
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onBind(itemUiState: JetpackListItemState) {
        with(binding) {
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
                if (state.iconColorResId == null) {
                    imageManager.load(icon, it)
                } else {
                    ColorUtils.setImageResourceWithTint(
                        icon,
                        it,
                        state.iconColorResId
                    )
                }
                icon.visibility = if (state.isVisible) View.VISIBLE else View.GONE
            }

            state.onIconClick?.let {
                icon.isClickable = true
                icon.setOnClickListener { state.onIconClick.invoke() }
            }
        }
    }
}
