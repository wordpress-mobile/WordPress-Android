package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingActionUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class PrepublishingActionsViewModel @Inject constructor(private val getPostTagsUseCase: GetPostTagsUseCase) : ViewModel() {
    private val _uiState = MutableLiveData<List<PrepublishingActionItemUiState>>()
    val uiState: LiveData<List<PrepublishingActionItemUiState>> = _uiState

    private val _onActionClicked = MutableLiveData<Event<ActionType>>()
    val onActionClicked: LiveData<Event<ActionType>> = _onActionClicked

    fun start(editPostRepository: EditPostRepository) {
        loadActionsUiState(editPostRepository)
    }

    // TODO remove hardcoded Immediately & Public with live data from the EditPostRepository / user changes.
    private fun loadActionsUiState(editPostRepository: EditPostRepository) {
        val prepublishingActionsUiStateList = listOf(
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
                        actionResult = getPostTagsUseCase.getTags(editPostRepository)?.let { UiStringText(it) },
                        onActionClicked = ::onActionClicked
                )
        )

        _uiState.postValue(prepublishingActionsUiStateList)
    }

    private fun onActionClicked(actionType: ActionType) {
        _onActionClicked.postValue(Event(actionType))
    }
}
