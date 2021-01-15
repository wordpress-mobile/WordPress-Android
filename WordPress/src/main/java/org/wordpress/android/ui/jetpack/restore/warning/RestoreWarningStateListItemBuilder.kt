package org.wordpress.android.ui.jetpack.restore.warning

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
class RestoreWarningStateListItemBuilder @Inject constructor() {
    fun buildWarningListStateItems(
        published: Date,
        onConfirmRestoreClick: () -> Unit
    ): List<JetpackListItemState> = listOf(
                buildIcon(),
                buildHeader(),
                buildDescriptionState(published),
                buildButtonAction(onConfirmRestoreClick)
    )

    private fun buildIcon() = IconState(
            icon = R.drawable.ic_trophy_white_24dp,
            contentDescription = UiStringRes(R.string.restore_warning_icon_content_description),
            colorResId = R.color.success_50 // todo: annmarie make correct when doing design cleanup
    )

    private fun buildHeader() = HeaderState(
            UiStringRes(R.string.restore_warning_header)
    )

    private fun buildDescriptionState(published: Date) = DescriptionState(
            UiStringResWithParams(
                    R.string.restore_warning_description_with_two_parameters,
                    listOf(
                            UiStringText(published.toFormattedDateString()),
                            UiStringText(published.toFormattedTimeString())
                    )
            )
    )

    private fun buildButtonAction(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.restore_warning_action_button),
            contentDescription = UiStringRes(R.string.restore_warning_action_button_content_description),
            onClick = onClick
    )
}
