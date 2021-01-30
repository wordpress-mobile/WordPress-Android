package org.wordpress.android.ui.jetpack.common

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class JetpackBackupRestoreListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class SubHeaderState(val text: UiString) :
            JetpackBackupRestoreListItemState(ViewType.BACKUP_RESTORE_SUB_HEADER)

    data class FootnoteState(val text: UiString) :
            JetpackListItemState(ViewType.BACKUP_RESTORE_FOOTNOTE)

    data class BulletState(
        @DrawableRes val icon: Int,
        @ColorRes val colorResId: Int? = null,
        @DimenRes val sizeResId: Int = R.dimen.jetpack_backup_restore_bullet_icon_size,
        @DimenRes val marginResId: Int = R.dimen.jetpack_icon_margin,
        val contentDescription: UiString,
        val label: UiString
    ) : JetpackListItemState(ViewType.BACKUP_RESTORE_BULLET)
}
