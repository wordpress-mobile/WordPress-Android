package org.wordpress.android.ui.posts.prepublishing.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

typealias PublishPost = Boolean

sealed class PrepublishingHomeItemUiState {
    data class HomeUiState(
        val navigationAction: ActionType.PrepublishingScreenNavigation,
        @ColorRes val actionTypeColor: Int = R.color.prepublishing_action_type_enabled_color,
        val actionResult: UiString? = null,
        @ColorRes val actionResultColor: Int = R.color.prepublishing_action_result_enabled_color,
        val actionClickable: Boolean,
        val onNavigationActionClicked: ((navigationAction: ActionType.PrepublishingScreenNavigation) -> Unit)?
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

    sealed class SocialUiState : PrepublishingHomeItemUiState() {
        abstract val serviceIcons: List<ConnectionServiceIcon>

        data class SocialSharingUiState(
            override val serviceIcons: List<ConnectionServiceIcon>,
            val title: UiString,
            val description: UiString,
            val isLowOnShares: Boolean,
            val onItemClicked: (() -> Unit),
        ) : SocialUiState()

        data class SocialConnectPromptUiState(
            override val serviceIcons: List<ConnectionServiceIcon>,
            val onConnectClicked: (() -> Unit),
            val onDismissClicked: (() -> Unit),
        ) : SocialUiState()

        data class ConnectionServiceIcon(
            @DrawableRes val iconRes: Int,
            val isEnabled: Boolean = true
        )
    }

    sealed interface ActionType {
        sealed class PrepublishingScreenNavigation(
            val textRes: UiStringRes,
            val prepublishingScreen: PrepublishingScreen,
        ) : ActionType {
            object Publish : PrepublishingScreenNavigation(
                UiStringRes(R.string.prepublishing_nudges_publish_action),
                PrepublishingScreen.PUBLISH,
            )
            object Tags : PrepublishingScreenNavigation(
                UiStringRes(R.string.prepublishing_nudges_tags_action),
                PrepublishingScreen.TAGS,
            )
            object Categories : PrepublishingScreenNavigation(
                UiStringRes(R.string.prepublishing_nudges_categories_action),
                PrepublishingScreen.CATEGORIES,
            )
            object AddCategory : PrepublishingScreenNavigation(
                UiStringRes(R.string.prepublishing_nudges_categories_action),
                PrepublishingScreen.ADD_CATEGORY,
            )
        }

        sealed class Action : ActionType {
            object NavigateToSharingSettings : Action()
        }
    }
}
