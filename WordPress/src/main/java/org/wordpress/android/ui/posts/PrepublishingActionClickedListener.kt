package org.wordpress.android.ui.posts

import android.os.Bundle
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType

interface PrepublishingActionClickedListener {
    fun onActionClicked(actionType: ActionType, bundle: Bundle? = null)
    fun onSubmitButtonClicked(publishPost: PublishPost)
}
