package org.wordpress.android.ui.jetpack.backup.download

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.BackupAvailableItemsProvider.BackupAvailableItemType
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ViewType.ACTION_BUTTON
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ViewType.CHECKBOX
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ViewType.DESCRIPTION
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ViewType.HEADER
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.ViewType.SUBHEADER
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams

// todo: annmarie - tighten this up as the remainder of backup download is implemented
sealed class BackupDownloadListItemState(val type: ViewType) {
    open fun longId(): Long = hashCode().toLong()

    sealed class HeaderState : BackupDownloadListItemState(HEADER) {
        abstract val text: UiString

        data class DetailsHeaderState(
            override val text: UiString = UiStringRes(R.string.backup_download_details_header)
        ) : HeaderState()
    }

    sealed class SubHeaderState : BackupDownloadListItemState(SUBHEADER) {
        abstract val text: UiString

        data class DetailsSubHeaderState(
            override val text: UiString = UiStringRes(R.string.backup_download_details_choose_items_header)
        ) : SubHeaderState()
    }

    sealed class DescriptionState : BackupDownloadListItemState(DESCRIPTION) {
        abstract val text: UiString

        data class DetailsSubHeaderState(
            override val text: UiString
        ) : DescriptionState() {
            constructor(params: List<UiString>) : this(
                    text = UiStringResWithParams(
                            R.string.backup_download_details_choose_items_header,
                            params
                    )
            )
        }
    }

    sealed class IconState : BackupDownloadListItemState(DESCRIPTION) {
        abstract val icon: Int
        abstract val contentDescription: UiString

        data class DetailsIconState(
            @DrawableRes override val icon: Int = R.drawable.ic_get_app_24dp,
            override val contentDescription: UiString =
                    UiStringRes(R.string.backup_download_details_icon_content_description)
        ) : IconState()
    }

    sealed class ActionButtonState : BackupDownloadListItemState(ACTION_BUTTON) {
        abstract val text: UiString
        abstract val contentDescription: UiString

        data class DetailsActionButtonState(
            override val text: UiString = UiStringRes(R.string.backup_download_details_action_button),
            override val contentDescription: UiString =
                    UiStringRes(R.string.backup_download_details_action_button_content_description),
            val onClick: () -> Unit
        ) : ActionButtonState()
    }

    data class CheckboxState(
        val availableItemType: BackupAvailableItemType,
        val label: UiString,
        val checked: Boolean = false,
        val onClick: (() -> Unit)
    ) : BackupDownloadListItemState(CHECKBOX)

    enum class ViewType(val id: Int) {
        IMAGE(0),
        HEADER(1),
        DESCRIPTION(2),
        SUBHEADER(3),
        ACTION_BUTTON(4),
        CHECKBOX(5)
    }
}
