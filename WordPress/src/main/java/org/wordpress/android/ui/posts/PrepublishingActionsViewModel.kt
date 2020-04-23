package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingActionUiState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingButtonUiState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingHomeHeaderUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingActionsViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    private val _prepublishingActionsUiState = MutableLiveData<List<PrepublishingActionItemUiState>>()
    val prepublishingActionsUiState: LiveData<List<PrepublishingActionItemUiState>> = _prepublishingActionsUiState

    private val _prepublishingActionType = MutableLiveData<Event<ActionType>>()
    val prepublishingActionType: LiveData<Event<ActionType>> = _prepublishingActionType

    fun start() {
        if (isStarted) return
        isStarted = true
        loadActionsUiState()
    }

    // TODO remove hardcoded Immediately & Public with live data from the EditPostRepository / user changes.
    private fun loadActionsUiState() {
        val prepublishingActionsUiStateList = listOf(
                PrepublishingHomeHeaderUiState(UiStringText("WPTest"), "WP Site Image Url"),
                PrepublishingActionUiState(
                        actionType = PUBLISH,
                        actionResult = UiStringText("Immediately"),
                        onActionClicked = ::onActionClicked
                ),
                PrepublishingActionUiState(
                        actionType = VISIBILITY,
                        actionResult = UiStringText("Public"),
                        onActionClicked = ::onActionClicked
                ),
                PrepublishingActionUiState(
                        actionType = TAGS,
                        actionResult = UiStringText("WPNot Set"),
                        onActionClicked = ::onActionClicked
                ),
                PrepublishingButtonUiState(UiStringRes(R.string.prepublishing_nudges_home_publish_button))
        )

        _prepublishingActionsUiState.postValue(prepublishingActionsUiStateList)
    }

    private fun onActionClicked(actionType: ActionType) {
        _prepublishingActionType.postValue(Event(actionType))
    }
}
