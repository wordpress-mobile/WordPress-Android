package org.wordpress.android.ui.jetpack.restore.complete

import dagger.Reusable
import org.wordpress.android.R
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
class RestoreCompleteStateListItemBuilder @Inject constructor() {
    fun buildCompleteListStateItems(
        published: Date,
        onDoneClick: () -> Unit,
        onVisitSiteClick: () -> Unit
    ): List<JetpackListItemState> {
        return listOf(
                buildIconState(),
                buildHeaderState(),
                buildDescriptionState(published),
                buildDoneActionState(onDoneClick),
                buildVisitSiteActionState(onVisitSiteClick)
        )
    }

    private fun buildIconState() = IconState(
            icon = R.drawable.ic_history_white_24dp,
            contentDescription = UiStringRes(R.string.restore_complete_icon_content_description),
            colorResId = R.color.success_50 // todo: annmarie make correct when doing design cleanup
    )

    private fun buildHeaderState() = HeaderState(
            UiStringRes(R.string.restore_complete_header)
    )

    private fun buildDescriptionState(published: Date) = DescriptionState(
            UiStringResWithParams(
                    R.string.restore_complete_description_with_two_parameters,
                    listOf(
                            UiStringText(published.toFormattedDateString()),
                            UiStringText(published.toFormattedTimeString())
                    )
            )
    )

    private fun buildVisitSiteActionState(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.restore_complete_visit_site_action_button),
            contentDescription = UiStringRes(R.string.restore_complete_done_button_content_description),
            onClick = onClick
    )

    private fun buildDoneActionState(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.restore_complete_done_action_button),
            contentDescription = UiStringRes(R.string.restore_complete_visit_site_button_content_description),
            onClick = onClick
    )

    fun buildCompleteListStateErrorItems(onDoneClick: () -> Unit) = listOf(
            buildErrorIconState(),
            buildErrorDescriptionState(),
            buildErrorDoneActionState(onDoneClick)
    )

    private fun buildErrorIconState() = IconState(
            icon = R.drawable.ic_get_app_24dp, // todo: annmarie replace with error icon
            contentDescription = UiStringRes(R.string.restore_complete_failed_icon_content_description),
            colorResId = R.color.error_50 // todo: annmarie make correct when doing design cleanup
    )

    private fun buildErrorDescriptionState() = DescriptionState(
            UiStringRes(R.string.restore_complete_failed_description)
    )

    private fun buildErrorDoneActionState(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.restore_complete_failed_action_button),
            contentDescription =
            UiStringRes(R.string.restore_complete_failed_action_button_content_description),
            onClick = onClick
    )
}
