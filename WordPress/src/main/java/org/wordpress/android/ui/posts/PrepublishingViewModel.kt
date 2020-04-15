package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionState.InitialState
import org.wordpress.android.ui.posts.PrepublishingActionState.TagsActionState
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.PrepublishingScreen.TAGS
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

const val KEY_TAGS_ACTION_STATE = "key_tags_action_state"

enum class PrepublishingScreen {
    HOME,
    PUBLISH,
    VISIBILITY,
    TAGS
}

sealed class PrepublishingActionState(val prepublishingScreen: PrepublishingScreen) : Parcelable {
    @Parcelize
    data class TagsActionState(val tags: String? = null) : PrepublishingActionState(TAGS)

    @Parcelize
    object InitialState : PrepublishingActionState(HOME)
}

data class PrepublishingNavigationState(val site: SiteModel, val prepublishingActionState: PrepublishingActionState)

class PrepublishingViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false
    private lateinit var site: SiteModel
    private var prepublishingActionState: PrepublishingActionState? = null
    private val _navigationState = MutableLiveData<Event<PrepublishingNavigationState>>()
    val navigationState: LiveData<Event<PrepublishingNavigationState>> = _navigationState

    fun start(site: SiteModel, prepublishingActionState: PrepublishingActionState?) {
        if (isStarted) return
        isStarted = true

        this.prepublishingActionState = prepublishingActionState
        this.site = site

        navigateToScreen()
    }

    private fun navigateToScreen() {
        if (prepublishingActionState != null) {
            updateNavigationState(prepublishingActionState as PrepublishingActionState)
        } else {
            updateNavigationState(InitialState)
        }
    }

    fun updateNavigationState(state: PrepublishingActionState) {
        _navigationState.postValue(Event(PrepublishingNavigationState(site, state)))
    }

    private

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_TAGS_ACTION_STATE, prepublishingActionState)
    }

    fun onActionClicked(actionType: ActionType) {
        when (actionType) {
            ActionType.TAGS -> updateNavigationState(TagsActionState())
            else -> TODO()
        }
    }

    fun updateTags(tags: String) {

    }
}
