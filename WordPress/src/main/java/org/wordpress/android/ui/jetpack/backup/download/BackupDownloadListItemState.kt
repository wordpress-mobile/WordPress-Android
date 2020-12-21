package org.wordpress.android.ui.jetpack.backup.download

import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString

sealed class BackupDownloadListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class DetailsSubHeaderState(val text: UiString) :
            BackupDownloadListItemState(ViewType.BACKUP_SUB_HEADER)
}
