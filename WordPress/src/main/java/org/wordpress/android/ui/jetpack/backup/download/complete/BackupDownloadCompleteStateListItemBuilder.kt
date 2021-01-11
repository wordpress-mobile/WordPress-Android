package org.wordpress.android.ui.jetpack.backup.download.complete

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.AdditionalInformationState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import java.util.Date
import javax.inject.Inject

@Reusable
class BackupDownloadCompleteStateListItemBuilder @Inject constructor() {
    fun buildCompleteListStateItems(
        published: Date,
        onDownloadFileClick: () -> Unit,
        onShareLinkClick: () -> Unit
    ): List<JetpackListItemState> {
        return mutableListOf(
                buildIconState(),
                buildHeaderState(),
                buildDescriptionState(published),
                buildNotifyMeButtonActionState(onDownloadFileClick),
                buildShareLinkButtonActionState(onShareLinkClick),
                buildAdditionalInformationState()
        )
    }

    private fun buildIconState() = IconState(
            icon = R.drawable.ic_get_app_24dp, // todo: annmarie replace with cloud icon
            contentDescription = UiStringRes(R.string.backup_download_complete_icon_content_description),
            colorResId = R.color.success_50 // todo: annmarie make correct when doing design cleanup
    )

    private fun buildHeaderState() = HeaderState(
            UiStringRes(R.string.backup_download_complete_header)
    )

    private fun buildDescriptionState(published: Date) = DescriptionState(
            UiStringResWithParams(
                    R.string.backup_download_complete_description_with_two_parameters,
                    listOf(
                            UiStringText(published.toFormattedDateString()),
                            UiStringText(published.toFormattedTimeString())
                    )
            )
    )

    private fun buildNotifyMeButtonActionState(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.backup_download_complete_download_action_button),
            contentDescription =
                UiStringRes(R.string.backup_download_complete_download_action_button_content_description),
            onClick = onClick
    )

    private fun buildShareLinkButtonActionState(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.backup_download_complete_download_share_action_button),
            contentDescription =
                UiStringRes(R.string.backup_download_complete_download_share_action_button_content_description),
            onClick = onClick
    )

    private fun buildAdditionalInformationState() = AdditionalInformationState(
            UiStringRes(R.string.backup_download_complete_info)
    )

    fun buildCompleteListStateErrorItems(onDoneClick: () -> Unit) = mutableListOf(
                buildErrorIconState(),
                buildErrorDescriptionState(),
            buildErrorDoneActionState(onDoneClick)
    )

    private fun buildErrorIconState() = IconState(
            icon = R.drawable.ic_get_app_24dp, // todo: annmarie replace with cloud icon
            contentDescription = UiStringRes(R.string.backup_download_complete_failed_icon_content_description),
            colorResId = R.color.error_50 // todo: annmarie make correct when doing design cleanup
    )

    private fun buildErrorDescriptionState() = DescriptionState(
            UiStringRes(R.string.backup_download_complete_failed_description)
    )

    private fun buildErrorDoneActionState(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.backup_download_complete_failed_action_button),
            contentDescription =
            UiStringRes(R.string.backup_download_complete_failed_action_button_content_description),
            onClick = onClick
    )
}
