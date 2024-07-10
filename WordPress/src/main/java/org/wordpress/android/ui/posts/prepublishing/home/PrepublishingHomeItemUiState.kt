package org.wordpress.android.ui.posts.prepublishing.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel
import org.wordpress.android.ui.posts.prepublishing.PrepublishingScreen
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

typealias PublishPost = Boolean

sealed class PrepublishingHomeItemUiState(
    val isVisible: Boolean = true
) {
    data class HomeUiState(
        val navigationAction: ActionType.PrepublishingScreenNavigation,
        @ColorRes val actionTypeColor: Int = R.color.prepublishing_action_type_enabled_color,
        val actionResult: UiString? = null,
        @ColorRes val actionResultColor: Int = R.color.prepublishing_action_result_enabled_color,
        val actionClickable: Boolean,
        val onNavigationActionClicked: ((navigationAction: ActionType.PrepublishingScreenNavigation) -> Unit)?
    ) : PrepublishingHomeItemUiState()

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

    sealed class SocialUiState(
        isVisible: Boolean = true
    ) : PrepublishingHomeItemUiState(isVisible) {
        abstract val serviceIcons: List<ConnectionServiceIcon>

        data class Visible(
            val state: EditorJetpackSocialViewModel.JetpackSocialUiState,
            val onItemClicked: (() -> Unit),
        ) : SocialUiState() {
            override val serviceIcons: List<ConnectionServiceIcon> = emptyList()
        }

        object Hidden : SocialUiState(
            isVisible = false
        ) {
            override val serviceIcons: List<ConnectionServiceIcon> = emptyList()
        }

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
            object Social : PrepublishingScreenNavigation(
                UiStringRes(R.string.prepublishing_nudges_social_action),
                PrepublishingScreen.SOCIAL,
            )
        }

        sealed class Action : ActionType {
            object NavigateToSharingSettings : Action()
        }
    }
}
