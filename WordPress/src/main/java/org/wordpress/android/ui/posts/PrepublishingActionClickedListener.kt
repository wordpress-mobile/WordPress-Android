package org.wordpress.android.ui.posts

import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType

interface PrepublishingActionClickedListener {
    fun onActionClicked(actionType: ActionType)
}
