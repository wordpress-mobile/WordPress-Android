package org.wordpress.android.ui.posts

import androidx.annotation.ColorRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

typealias PublishPost = Boolean

sealed class PrepublishingHomeItemUiState {
    data class HomeUiState(
        val actionType: ActionType,
        @ColorRes val actionTypeColor: Int = R.color.prepublishing_action_type_enabled_color,
        var actionResult: UiString? = null,
        @ColorRes val actionResultColor: Int = R.color.prepublishing_action_result_enabled_color,
        val actionClickable: Boolean,
        val onActionClicked: ((actionType: ActionType) -> Unit)?
    ) : PrepublishingHomeItemUiState()

    data class StoryTitleUiState(
        val storyThumbnailUrl: String,
        val storyTitle: UiStringText? = null,
        val onStoryTitleChanged: (String) -> Unit
    ) :
            PrepublishingHomeItemUiState()

    data class HeaderUiState(val siteName: UiStringText, val siteIconUrl: String) :
            PrepublishingHomeItemUiState()

    sealed class ButtonUiState(
        val buttonText: UiStringRes,
        val publishPost: PublishPost
    ) : PrepublishingHomeItemUiState() {
        open val onButtonClicked: ((PublishPost) -> Unit)? = null

        data class PublishButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_publish_button),
                true
        )

        data class ScheduleButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_schedule_button),
                false
        )

        data class UpdateButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_update_button),
                false
        )

        data class SubmitButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_submit_button),
                true
        )

        data class SaveButtonUiState(override val onButtonClicked: (PublishPost) -> Unit) : ButtonUiState(
                UiStringRes(R.string.prepublishing_nudges_home_save_button),
                false
        )
    }

    enum class ActionType(val textRes: UiStringRes) {
        PUBLISH(UiStringRes(R.string.prepublishing_nudges_publish_action)),
        VISIBILITY(UiStringRes(R.string.prepublishing_nudges_visibility_action)),
        TAGS(UiStringRes(R.string.prepublishing_nudges_tags_action))
    }
}
