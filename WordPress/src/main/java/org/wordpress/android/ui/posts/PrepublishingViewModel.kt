package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingActionState.HomeState
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
    data class HomeState(val tags: String? = null) : PrepublishingActionState(HOME)
}

data class PrepublishingNavigationState(
    val site: SiteModel,
    val prepublishingScreen: PrepublishingScreen,
    val screenState: PrepublishingActionState?
)

class PrepublishingViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false
    private lateinit var site: SiteModel
    private var tagsActionState: TagsActionState? = null
    private var currentActionState: PrepublishingActionState? = null
    private val _navigationState = MutableLiveData<Event<PrepublishingNavigationState>>()
    val navigationState: LiveData<Event<PrepublishingNavigationState>> = _navigationState

    fun start(site: SiteModel, prepublishingActionState: PrepublishingActionState?) {
        if (isStarted) return
        isStarted = true

        this.site = site

        prepublishingActionState?.let {
            updateActionState(prepublishingActionState)
            navigateToScreen(prepublishingActionState.prepublishingScreen)
        } ?: run { navigateToScreen(HOME) }
    }

    private fun updateActionState(prepublishingActionState: PrepublishingActionState) {
        when (prepublishingActionState) {
            is TagsActionState -> tagsActionState = prepublishingActionState
        }
    }

    private fun navigateToScreen(prepublishingScreen: PrepublishingScreen) {
        when (prepublishingScreen) {
            HOME -> updateNavigationState(PrepublishingNavigationState(site, HOME, HomeState(tagsActionState?.tags)))
            TAGS -> updateNavigationState(
                    PrepublishingNavigationState(
                            site,
                            prepublishingScreen,
                            tagsActionState
                    )
            )
        }
    }

    fun navigateHome() {
        navigateToScreen(HOME)
    }

    private fun updateNavigationState(state: PrepublishingNavigationState) {
        _navigationState.postValue(Event(state))
    }

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_TAGS_ACTION_STATE, currentActionState)
    }

    fun onActionClicked(actionType: ActionType) {
        when (actionType) {
            ActionType.TAGS -> navigateToScreen(TAGS)
            else -> TODO()
        }
    }

    fun updateTagsActionState(tags: String) {
        tagsActionState = TagsActionState(tags)
        currentActionState = tagsActionState
    }
}
