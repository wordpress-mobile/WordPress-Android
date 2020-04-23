package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

sealed class PrepublishingActionItemUiState {
    data class PrepublishingActionUiState(
        val actionType: ActionType,
        var actionResult: UiString? = null,
        val onActionClicked: (actionType: ActionType) -> Unit
    ) : PrepublishingActionItemUiState()

    data class PrepublishingHomeHeaderUiState(val siteName: UiStringText, val siteIconUrl: String) :
            PrepublishingActionItemUiState()

    data class PrepublishingButtonUiState(val buttonText: String) :
            PrepublishingActionItemUiState()

    enum class ActionType(val textRes: UiStringRes) {
        PUBLISH(UiStringRes(R.string.prepublishing_nudges_publish_action)),
        VISIBILITY(UiStringRes(R.string.prepublishing_nudges_visibility_action)),
        TAGS(UiStringRes(R.string.prepublishing_nudges_tags_action))
    }
}
