package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.posts.ActionType.PUBLISH
import org.wordpress.android.ui.posts.ActionType.TAGS
import org.wordpress.android.ui.posts.ActionType.VISIBILITY
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class PrepublishingOptionsViewModel @Inject constructor(
) : ViewModel() {
    private val _prepublishingActions = MutableLiveData<List<PrepublishingActionListItem>>()
    val prepublishingActions: LiveData<List<PrepublishingActionListItem>> = _prepublishingActions

    private val _prepublishingAction = SingleLiveEvent<ActionType>()
    val prepublishingAction: LiveData<ActionType> = _prepublishingAction

    fun start() {
        loadActions()
    }

    private fun loadActions() {
        val prepublishingActionsList = arrayListOf<PrepublishingActionListItem>().apply {
            add(PrepublishingActionListItem(actionType = PUBLISH, onActionClicked = ::onActionClicked))
            add(PrepublishingActionListItem(actionType = VISIBILITY, onActionClicked = ::onActionClicked))
            add(PrepublishingActionListItem(actionType = TAGS, onActionClicked = ::onActionClicked))
        }

        _prepublishingActions.postValue(prepublishingActionsList)
    }

    private fun onActionClicked(actionType: ActionType) {
        _prepublishingAction.postValue(actionType)
    }
}
