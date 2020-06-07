package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

typealias PublishPost = Boolean
sealed class PrepublishingHomeItemUiState {
    data class HomeUiState(
        val actionType: ActionType,
        var actionResult: UiString? = null,
        val onActionClicked: (actionType: ActionType) -> Unit
    ) : PrepublishingHomeItemUiState()

    data class HeaderUiState(val siteName: UiStringText, val siteIconUrl: String) :
            PrepublishingHomeItemUiState()

    sealed class ButtonUiState(
        val buttonText: UiStringRes,
        val publishPost: PublishPost,
        open val onButtonClicked: (PublishPost) -> Unit
    ) : PrepublishingHomeItemUiState() {
        data class PublishButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_publish_button),
                true,
                onButtonClicked
        )

        data class ScheduleButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_schedule_button),
                false,
                onButtonClicked
        )

        data class UpdateButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_update_button),
                false,
                onButtonClicked
        )

        data class SubmitButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_submit_button),
                false,
                onButtonClicked
        )

        data class SaveButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_save_button),
                false,
                onButtonClicked
        )
    }

    enum class ActionType(val textRes: UiStringRes) {
        PUBLISH(UiStringRes(R.string.prepublishing_nudges_publish_action)),
        VISIBILITY(UiStringRes(R.string.prepublishing_nudges_visibility_action)),
        TAGS(UiStringRes(R.string.prepublishing_nudges_tags_action))
    }
}
