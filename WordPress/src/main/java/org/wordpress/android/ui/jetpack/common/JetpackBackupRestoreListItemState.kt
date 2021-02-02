package org.wordpress.android.ui.jetpack.common

import org.wordpress.android.ui.utils.UiString

sealed class JetpackBackupRestoreListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class SubHeaderState(val text: UiString) :
            JetpackBackupRestoreListItemState(ViewType.BACKUP_RESTORE_SUB_HEADER)

    data class FootnoteState(val text: UiString) :
            JetpackListItemState(ViewType.BACKUP_RESTORE_FOOTNOTE)
}
