package org.wordpress.android.ui.posts

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

data class PrepublishingActionListItem(
    val label: UiString,
    val content: UiString?,
    val onActionClicked: (actionType: ActionType) -> Unit,
    val actionType: ActionType
)

enum class ActionType(@StringRes val textRes: Int) {
    PUBLISH(R.string.prepublishing_nudges_publish_action),
    VISIBILITY(R.string.prepublishing_nudges_visibility_action),
    TAGS(R.string.prepublishing_nudges_tags_action)
}
