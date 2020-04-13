package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.ui.posts.ActionState.TagsActionState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

const val KEY_TAGS_ACTION_STATE = "key_tags_action_state"

enum class PrepublishingScreen {
    HOME,
    PUBLISH,
    VISIBILITY,
    TAGS
}

@Parcelize
data class ActionsState(val currentScreen:PrepublishingScreen = HOME, val tagsActionState: TagsActionState) : Parcelable

sealed class ActionState() : Parcelable {
    @Parcelize
    data class TagsActionState(val tags: String?) : ActionState()
}

class PrepublishingViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    private lateinit var actionsState: ActionsState
    private val _currentActionType = MutableLiveData<Event<ActionsState>>()
    val currentActionType: LiveData<Event<ActionsState>> = _currentActionType

    fun start(actionsState: ActionsState) {
        if (isStarted) return
        isStarted = true
        this.actionsState = actionsState
        updateState()
    }

    private fun updateState() {
        _currentActionType.postValue(Event(actionsState))
    }

    fun updateCurrentActionTypeState(actionType: ActionType) {
        //update state her.
        updateState()
    }

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_TAGS_ACTION_STATE, actionsState)
    }
}
