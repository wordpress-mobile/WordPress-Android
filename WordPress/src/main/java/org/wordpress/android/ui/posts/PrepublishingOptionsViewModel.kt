package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingActionUiState
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class PrepublishingOptionsViewModel @Inject constructor(
) : ViewModel() {
    private var isStarted = false

    private val _prepublishingActionsUiState = MutableLiveData<List<PrepublishingActionItemUiState>>()
    val prepublishingActionsUiState: LiveData<List<PrepublishingActionItemUiState>> = _prepublishingActionsUiState

    private val _prepublishingAction = SingleLiveEvent<ActionType>()
    val prepublishingAction: LiveData<ActionType> = _prepublishingAction

    fun start() {
        if (isStarted) return
        isStarted = true
        loadActionsUiState()
    }

    private fun loadActionsUiState() {
        val prepublishingActionsUiStateList = arrayListOf<PrepublishingActionUiState>().apply {
            add(PrepublishingActionUiState(actionType = PUBLISH, onActionClicked = ::onActionClicked))
            add(PrepublishingActionUiState(actionType = VISIBILITY, onActionClicked = ::onActionClicked))
            add(PrepublishingActionUiState(actionType = TAGS, onActionClicked = ::onActionClicked))
        }

        _prepublishingActionsUiState.postValue(prepublishingActionsUiStateList)
    }

    private fun onActionClicked(actionType: ActionType) {
        _prepublishingAction.postValue(actionType)
    }
}
