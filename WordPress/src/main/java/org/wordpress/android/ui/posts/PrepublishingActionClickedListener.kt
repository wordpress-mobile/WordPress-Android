package org.wordpress.android.ui.posts

import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType

interface PrepublishingActionClickedListener {
    fun onActionClicked(actionType: ActionType)
    fun onPublishButtonClicked(publishPost: PublishPost)
}
