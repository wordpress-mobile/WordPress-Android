package org.wordpress.android.ui.jetpack.restore.details

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItem
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.jetpack.restore.RestoreListItemState.SubHeaderState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import java.util.Date
import javax.inject.Inject

@Reusable
class RestoreDetailsStateListItemBuilder @Inject constructor() {
    fun buildDetailsListStateItems(
        published: Date,
        availableItems: List<JetpackAvailableItem>,
        onCreateDownloadClick: () -> Unit,
        onCheckboxItemClicked: (availableItemType: JetpackAvailableItemType) -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf(
                buildDetailsIcon(),
                buildDetailsHeader(),
                buildDescriptionState(published),
                buildDetailsButtonAction(onCreateDownloadClick),
                buildDetailsSubHeader()
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

    private fun buildDetailsIcon() = IconState(
            icon = R.drawable.ic_history_white_24dp,
            contentDescription = UiStringRes(R.string.restore_details_icon_content_description),
            colorResId = R.color.success_50 // todo: annmarie make correct when doing design cleanup
    )

    private fun buildDetailsHeader() = HeaderState(
            UiStringRes(R.string.restore_details_header)
    )

    private fun buildDescriptionState(published: Date) = DescriptionState(
            UiStringResWithParams(
                    R.string.restore_details_description_with_two_parameters,
                    listOf(
                            UiStringText(published.toFormattedDateString()),
                            UiStringText(published.toFormattedTimeString())
                    )
            )
    )

    private fun buildDetailsButtonAction(onClick: () -> Unit) = ActionButtonState(
            text = UiStringRes(R.string.restore_details_action_button),
            contentDescription = UiStringRes(R.string.restore_details_action_button_content_description),
            onClick = onClick
    )

    private fun buildDetailsSubHeader() = SubHeaderState(
            text = UiStringRes(R.string.restore_details_choose_items_header)
    )
}
