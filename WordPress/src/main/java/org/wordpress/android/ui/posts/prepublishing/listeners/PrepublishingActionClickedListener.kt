package org.wordpress.android.ui.posts.prepublishing.listeners

import android.os.Bundle
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.prepublishing.home.PublishPost

interface PrepublishingActionClickedListener {
    fun onActionClicked(actionType: ActionType, bundle: Bundle? = null)
    fun onSubmitButtonClicked(publishPost: PublishPost)
}
