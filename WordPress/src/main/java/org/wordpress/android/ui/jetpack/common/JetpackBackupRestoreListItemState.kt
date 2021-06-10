package org.wordpress.android.ui.jetpack.common

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class JetpackBackupRestoreListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class SubHeaderState(
        val text: UiString,
        @DimenRes val itemTopMarginResId: Int? = null,
        @DimenRes val itemBottomMarginResId: Int? = null
    ) : JetpackBackupRestoreListItemState(ViewType.BACKUP_RESTORE_SUB_HEADER)

    data class FootnoteState(
        @DrawableRes val iconRes: Int? = null,
        @ColorRes val iconColorResId: Int? = null,
        @DimenRes val iconSizeResId: Int? = null,
        @DimenRes val textAlphaResId: Int? = null,
        val text: UiString,
        val isVisible: Boolean = true,
        val onIconClick: (() -> Unit)? = null
    ) : JetpackListItemState(ViewType.BACKUP_RESTORE_FOOTNOTE)

    data class BulletState(
        @DrawableRes val icon: Int,
        @ColorRes val colorResId: Int? = null,
        @DimenRes val sizeResId: Int = R.dimen.jetpack_icon_size,
        @DimenRes val itemBottomMarginResId: Int? = null,
        val contentDescription: UiString,
        val label: UiString
    ) : JetpackListItemState(ViewType.BACKUP_RESTORE_BULLET)
}
