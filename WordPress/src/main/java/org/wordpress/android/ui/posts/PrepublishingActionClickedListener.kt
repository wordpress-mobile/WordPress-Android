package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType

interface PrepublishingActionClickedListener {
    fun onActionClicked(actionType: ActionType)
    fun onSubmitButtonClicked(postId: LocalId, publishPost: PublishPost)
}
