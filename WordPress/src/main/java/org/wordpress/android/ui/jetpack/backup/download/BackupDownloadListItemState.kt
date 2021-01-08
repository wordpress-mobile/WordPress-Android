package org.wordpress.android.ui.jetpack.backup.download

import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString

sealed class BackupDownloadListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class SubHeaderState(val text: UiString) :
            BackupDownloadListItemState(ViewType.BACKUP_SUB_HEADER)

    data class ProgressState(val progress: Int, val label: UiString) :
            JetpackListItemState(ViewType.BACKUP_PROGRESS)

    data class AdditionalInformationState(val text: UiString) :
            JetpackListItemState(ViewType.BACKUP_ADDITIONAL_INFORMATION)
}
