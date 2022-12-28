package org.wordpress.android.ui.jetpack.restore.builders

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.CheckboxSpannableLabel
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.BulletState
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.SubHeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.common.ViewType.CHECKBOX
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItem
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreUiState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.extensions.toFormattedDateString
import org.wordpress.android.util.extensions.toFormattedTimeString
import org.wordpress.android.util.text.PercentFormatter
import java.util.Date
import javax.inject.Inject

@Reusable
class RestoreStateListItemBuilder @Inject constructor(
    private val checkboxSpannableLabel: CheckboxSpannableLabel,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val percentFormatter: PercentFormatter
) {
    @Suppress("LongParameterList")
    fun buildDetailsListStateItems(
        published: Date,
        availableItems: List<JetpackAvailableItem>,
        siteId: Long,
        isAwaitingCredentials: Boolean,
        onCreateDownloadClick: () -> Unit,
        onCheckboxItemClicked: (availableItemType: JetpackAvailableItemType) -> Unit,
        onEnterServerCredsIconClicked: () -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf(
            buildIconState(
                R.drawable.ic_history_white_24dp,
                R.string.restore_details_icon_content_description,
                R.color.success
            ),
            buildHeaderState(R.string.restore_details_header),
            buildDescriptionState(published, R.string.restore_details_description_with_two_parameters),
            buildActionButtonState(
                titleRes = R.string.restore_details_action_button,
                contentDescRes = R.string.restore_details_action_button_content_description,
                isEnabled = !isAwaitingCredentials,
                onClick = onCreateDownloadClick
            )
        )
        if (isAwaitingCredentials) {
            items.add(
                buildEnterServerCredsMessageState(
                    onEnterServerCredsIconClicked,
                    siteId = siteId,
                    iconResId = R.drawable.ic_plus_white_24dp,
                    iconColorResId = R.color.colorPrimary,
                    iconSizeResId = R.dimen.jetpack_backup_restore_footnote_enter_server_creds_icon_size
                )
            )
        }
        items.add(buildSubHeaderState(R.string.restore_details_choose_items_header))

        val availableItemsListItems: List<CheckboxState> = availableItems.map {
            CheckboxState(
                availableItemType = it.availableItemType,
                label = UiStringRes(it.labelResId),
                labelSpannable = checkboxSpannableLabel.buildSpannableLabel(it.labelResId, it.labelHintResId),
                checked = !isAwaitingCredentials,
                isEnabled = !isAwaitingCredentials,
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
            R.color.error
        ),
        buildHeaderState(R.string.restore_warning_header),
        buildDescriptionState(published, R.string.restore_warning_description_with_two_parameters),
        buildActionButtonState(
            titleRes = R.string.restore_warning_action_button,
            contentDescRes = R.string.restore_warning_action_button_content_description,
            onClick = onConfirmRestoreClick
        ),
        buildActionButtonState(
            titleRes = R.string.cancel,
            contentDescRes = R.string.cancel,
            isSecondary = true,
            onClick = onCancelClick
        )
    )

    fun buildProgressListStateItems(
        progress: Int = 0,
        published: Date,
        isIndeterminate: Boolean = false
    ): List<JetpackListItemState> {
        return mutableListOf(
            buildIconState(
                R.drawable.ic_history_white_24dp,
                R.string.restore_progress_icon_content_description,
                R.color.success
            ),
            buildHeaderState(R.string.restore_progress_header),
            buildDescriptionState(published, R.string.restore_progress_description_with_two_parameters),
            buildProgressState(progress, isIndeterminate),
            buildFootnoteState(
                iconRes = R.drawable.ic_info_outline_white_24dp,
                iconSizeResId = R.dimen.jetpack_backup_restore_footnote_icon_size,
                textRes = R.string.restore_progress_footnote
            )
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
                R.color.success
            ),
            buildHeaderState(R.string.restore_complete_header),
            buildDescriptionState(published, R.string.restore_complete_description_with_two_parameters),
            buildActionButtonState(
                titleRes = R.string.restore_complete_done_action_button,
                contentDescRes = R.string.restore_complete_done_button_content_description,
                onClick = onDoneClick
            ),
            buildActionButtonState(
                titleRes = R.string.restore_complete_visit_site_action_button,
                contentDescRes = R.string.restore_complete_visit_site_button_content_description,
                isSecondary = true,
                onClick = onVisitSiteClick
            )
        )
    }

    fun buildErrorListStateErrorItems(errorType: RestoreErrorTypes, onDoneClick: () -> Unit) = (
            if (errorType == RemoteRequestFailure) buildStatusErrorListStateItems(onDoneClick)
            else buildGenericErrorListStateItems(onDoneClick)
            )

    private fun buildStatusErrorListStateItems(onDoneClick: () -> Unit) = listOf(
        buildHeaderState(R.string.restore_status_failure_heading),
        buildBulletState(
            R.drawable.ic_query_builder_white_24dp,
            R.string.restore_status_bullet_clock_icon_content_desc,
            R.color.warning,
            R.string.restore_status_failure_bullet1
        ),
        buildBulletState(
            R.drawable.ic_gridicons_checkmark_circle,
            R.string.restore_status_bullet_checkmark_icon_content_desc,
            R.color.success,
            R.string.restore_status_failure_bullet2
        ),
        buildBulletState(
            R.drawable.ic_gridicons_checkmark_circle,
            R.string.restore_status_bullet_checkmark_icon_content_desc,
            R.color.success,
            R.string.restore_status_failure_bullet3,
            R.dimen.jetpack_backup_restore_last_bullet_bottom_margin
        ),
        buildActionButtonState(
            titleRes = R.string.restore_complete_failed_action_button,
            contentDescRes = R.string.restore_complete_failed_action_button_content_description,
            onClick = onDoneClick
        )
    )

    private fun buildGenericErrorListStateItems(onDoneClick: () -> Unit) = listOf(
        buildIconState(
            R.drawable.ic_notice_white_24dp,
            R.string.restore_complete_failed_icon_content_description,
            R.color.error
        ),
        buildHeaderState(R.string.restore_complete_failed_description),
        buildDescriptionState(descRes = R.string.request_failed_message),
        buildActionButtonState(
            titleRes = R.string.restore_complete_failed_action_button,
            contentDescRes = R.string.restore_complete_failed_action_button_content_description,
            onClick = onDoneClick
        )
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

    private fun buildDescriptionState(published: Date? = null, @StringRes descRes: Int) = DescriptionState(
        if (published != null) {
            UiStringResWithParams(
                descRes,
                listOf(
                    UiStringText(published.toFormattedDateString()),
                    UiStringText(published.toFormattedTimeString())
                )
            )
        } else {
            UiStringRes(descRes)
        }
    )

    private fun buildActionButtonState(
        @StringRes titleRes: Int,
        @StringRes contentDescRes: Int,
        isSecondary: Boolean = false,
        isEnabled: Boolean = true,
        onClick: () -> Unit
    ) = ActionButtonState(
        text = UiStringRes(titleRes),
        contentDescription = UiStringRes(contentDescRes),
        isSecondary = isSecondary,
        onClick = onClick,
        isEnabled = isEnabled
    )

    private fun buildEnterServerCredsMessageState(
        onEnterServerCredsIconClicked: () -> Unit,
        siteId: Long,
        @DrawableRes iconResId: Int? = null,
        @ColorRes iconColorResId: Int? = null,
        @DimenRes iconSizeResId: Int? = null
    ): FootnoteState {
        return FootnoteState(
            iconRes = iconResId,
            iconColorResId = iconColorResId,
            iconSizeResId = iconSizeResId,
            text = UiStringText(
                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                    R.string.restore_details_enter_server_creds_msg,
                    "${Constants.URL_JETPACK_SETTINGS}/$siteId"
                )
            ),
            onIconClick = onEnterServerCredsIconClicked
        )
    }

    private fun buildSubHeaderState(
        @StringRes textResId: Int,
        @DimenRes topMarginResId: Int? = null,
        @DimenRes bottomMarginResId: Int? = null
    ) = SubHeaderState(
        text = UiStringRes(textResId),
        itemTopMarginResId = topMarginResId,
        itemBottomMarginResId = bottomMarginResId
    )

    private fun buildFootnoteState(
        @DrawableRes iconRes: Int? = null,
        @DimenRes iconSizeResId: Int? = null,
        @StringRes textRes: Int
    ) = FootnoteState(
        iconRes = iconRes,
        iconSizeResId = iconSizeResId,
        text = UiStringRes(textRes)
    )

    private fun buildProgressState(progress: Int, isIndeterminate: Boolean = false) = ProgressState(
        progress = progress,
        isIndeterminate = isIndeterminate,
        progressLabel = UiStringText(percentFormatter.format(progress))
    )

    private fun buildBulletState(
        @DrawableRes iconRes: Int,
        @StringRes contentDescRes: Int,
        @ColorRes colorRes: Int,
        @StringRes labelRes: Int,
        @DimenRes itemBottomMarginResId: Int? = null
    ) = BulletState(
        icon = iconRes,
        contentDescription = UiStringRes(contentDescRes),
        colorResId = colorRes,
        label = UiStringRes(labelRes),
        itemBottomMarginResId = itemBottomMarginResId
    )

    fun updateCheckboxes(
        uiState: RestoreUiState,
        itemType: JetpackAvailableItemType
    ): List<JetpackListItemState> {
        val updatedCheckboxes = uiState.items.map { state ->
            if (state.type == CHECKBOX) {
                state as CheckboxState
                if (state.availableItemType == itemType) {
                    state.copy(checked = !state.checked)
                } else {
                    state
                }
            } else {
                state
            }
        }
        val atLeastOneChecked = updatedCheckboxes.filterIsInstance<CheckboxState>().find { it.checked } != null
        return updateDetailsActionButtonState(updatedCheckboxes, atLeastOneChecked)
    }

    private fun updateDetailsActionButtonState(
        details: List<JetpackListItemState>,
        enableActionButton: Boolean
    ): List<JetpackListItemState> {
        return details.map { state ->
            if (state is ActionButtonState) {
                state.copy(isEnabled = enableActionButton)
            } else {
                state
            }
        }
    }

    fun updateProgressActionButtonState(uiState: RestoreUiState, value: Boolean): List<JetpackListItemState> {
        return uiState.items.map { state ->
            if (state is ActionButtonState) {
                state.copy(isEnabled = value)
            } else {
                state
            }
        }
    }
}
