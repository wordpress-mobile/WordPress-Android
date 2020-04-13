package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.ui.posts.ActionState.TagsActionState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

const val KEY_CURRENT_ACTION_TYPE_STATE = "key_current_action_type_state"
const val KEY_TAGS_ACTION_STATE = "key_tags_action_state"

sealed class CurrentActionTypeState() : Parcelable {
    @Parcelize
    object HomeActionTypeState : CurrentActionTypeState()

    @Parcelize
    data class ActionTypeTypeState(val actionType: ActionType) : CurrentActionTypeState()
}

@Parcelize
data class ActionsState(val tagsActionState: TagsActionState) : Parcelable

sealed class ActionState() : Parcelable {
    @Parcelize
    data class TagsActionState(val tags: String?) : ActionState()
}

class PrepublishingViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    private lateinit var tagsActionState: TagsActionState
    private lateinit var mCurrentActionTypeState: CurrentActionTypeState

    private val _currentActionType = MutableLiveData<Event<Pair<CurrentActionTypeState, TagsActionState>>>()
    val mCurrentActionType: LiveData<Event<Pair<CurrentActionTypeState, TagsActionState>>> = _currentActionType

    fun start(currentActionTypeState: CurrentActionTypeState, actionsState: ActionsState) {
        if (isStarted) return
        isStarted = true

        _currentActionType.postValue(Event(Pair(currentActionTypeState, tagsActionState)))
    }

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_CURRENT_ACTION_TYPE_STATE, mCurrentActionTypeState)
        outState.putParcelable(KEY_TAGS_ACTION_STATE, tagsActionState)
    }
}
