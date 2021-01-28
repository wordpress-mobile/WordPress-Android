package org.wordpress.android.ui.jetpack.restore.builders

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.SubHeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItem
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import java.util.Date
import javax.inject.Inject

@Reusable
class RestoreStateListItemBuilder @Inject constructor() {
    fun buildDetailsListStateItems(
        published: Date,
        availableItems: List<JetpackAvailableItem>,
        onCreateDownloadClick: () -> Unit,
        onCheckboxItemClicked: (availableItemType: JetpackAvailableItemType) -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf(
                buildIconState(
                        R.drawable.ic_history_white_24dp,
                        R.string.restore_details_icon_content_description,
                        R.color.success_50),
                buildHeaderState(R.string.restore_details_header),
                buildDescriptionState(published, R.string.restore_details_description_with_two_parameters),
                buildActionButtonState(
                        titleRes = R.string.restore_details_action_button,
                        contentDescRes = R.string.restore_details_action_button_content_description,
                        onClick = onCreateDownloadClick),
                buildSubHeaderState()
        )

        val availableItemsListItems: List<CheckboxState> = availableItems.map {
            CheckboxState(
                    availableItemType = it.availableItemType,
                    label = UiStringRes(it.labelResId),
                    checked = true,
                    onClick = { onCheckboxItemClicked(it.availableItemType) }
            )
        }
        items.addAll(availableItemsListItems)
        return items
    }

    fun buildWarningListStateItems(
        published: Date,
        onConfirmRestoreClick: () -> Unit,
        onCancelClick: () -> Unit
    ): List<JetpackListItemState> = listOf(
            buildIconState(
                    R.drawable.ic_notice_white_24dp,
                    R.string.restore_warning_icon_content_description,
                    R.color.error_50),
            buildHeaderState(R.string.restore_warning_header),
            buildDescriptionState(published, R.string.restore_warning_description_with_two_parameters),
            buildActionButtonState(
                    titleRes = R.string.restore_warning_action_button,
                    contentDescRes = R.string.restore_warning_action_button_content_description,
                    onClick = onConfirmRestoreClick),
            buildActionButtonState(
                    titleRes = R.string.cancel,
                    contentDescRes = R.string.cancel,
                    isSecondary = true,
                    onClick = onCancelClick)
    )

    fun buildProgressListStateItems(
        progress: Int = 0,
        published: Date,
        onNotifyMeClick: () -> Unit
    ): List<JetpackListItemState> {
        return mutableListOf(
                buildIconState(
                        R.drawable.ic_history_white_24dp,
                        R.string.restore_progress_icon_content_description,
                        R.color.success_50),
                buildHeaderState(R.string.restore_progress_header),
                buildDescriptionState(published, R.string.restore_progress_description_with_two_parameters),
                buildProgressState(progress),
                buildActionButtonState(
                        titleRes = R.string.restore_progress_action_button,
                        contentDescRes = R.string.restore_progress_action_button_content_description,
                        onClick = onNotifyMeClick),
                buildFootnoteState(R.string.restore_progress_footnote)
        )
    }

    fun buildCompleteListStateItems(
        published: Date,
        onDoneClick: () -> Unit,
        onVisitSiteClick: () -> Unit
    ): List<JetpackListItemState> {
        return listOf(
                buildIconState(
                        R.drawable.ic_history_white_24dp,
                        R.string.restore_complete_icon_content_description,
                        R.color.success_50),
                buildHeaderState(R.string.restore_complete_header),
                buildDescriptionState(published, R.string.restore_complete_description_with_two_parameters),
                buildActionButtonState(
                        titleRes = R.string.restore_complete_done_action_button,
                        contentDescRes = R.string.restore_complete_done_button_content_description,
                        onClick = onDoneClick),
                buildActionButtonState(
                        titleRes = R.string.restore_complete_visit_site_action_button,
                        contentDescRes = R.string.restore_complete_visit_site_button_content_description,
                        isSecondary = true,
                        onClick = onVisitSiteClick)
        )
    }

    fun buildCompleteListStateErrorItems(onDoneClick: () -> Unit) = listOf(
            buildIconState(
                    R.drawable.ic_notice_white_24dp,
                    R.string.restore_complete_failed_icon_content_description,
                    R.color.error_50),
            buildDescriptionState(R.string.restore_complete_failed_description),
            buildActionButtonState(
                    titleRes = R.string.restore_complete_failed_action_button,
                    contentDescRes = R.string.restore_complete_failed_action_button_content_description,
                    onClick = onDoneClick)
    )

    private fun buildIconState(
        @DrawableRes iconRes: Int,
        @StringRes contentDescRes: Int,
        @ColorRes colorRes: Int
    ) = IconState(
            icon = iconRes,
            contentDescription = UiStringRes(contentDescRes),
            colorResId = colorRes
    )

    private fun buildHeaderState(@StringRes titleRes: Int) = HeaderState(UiStringRes(titleRes))

    private fun buildDescriptionState(published: Date, @StringRes descRes: Int) = DescriptionState(
            UiStringResWithParams(
                    descRes,
                    listOf(
                            UiStringText(published.toFormattedDateString()),
                            UiStringText(published.toFormattedTimeString())
                    )
            )
    )

    private fun buildDescriptionState(@StringRes descRes: Int) = DescriptionState(UiStringRes(descRes))

    private fun buildActionButtonState(
        @StringRes titleRes: Int,
        @StringRes contentDescRes: Int,
        isSecondary: Boolean = false,
        onClick: () -> Unit
    ) = ActionButtonState(
        text = UiStringRes(titleRes),
        contentDescription = UiStringRes(contentDescRes),
        isSecondary = isSecondary,
        onClick = onClick
    )

    private fun buildSubHeaderState() =
            SubHeaderState(text = UiStringRes(R.string.restore_details_choose_items_header))

    private fun buildFootnoteState(@StringRes textRes: Int) = FootnoteState(
            UiStringRes(textRes)
    )

    private fun buildProgressState(progress: Int) = ProgressState(
            progress = progress,
            progressLabel = UiStringResWithParams(
                    R.string.restore_progress_label, listOf(UiStringText(progress.toString()))
            )
    )
}
