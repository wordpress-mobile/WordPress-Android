package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.PrepublishingScreen.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingScreen.TAGS
import org.wordpress.android.ui.posts.PrepublishingScreen.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingScreenState.ActionsState
import org.wordpress.android.ui.posts.PrepublishingScreenState.TagsState
import org.wordpress.android.viewmodel.Event
import java.util.ArrayList
import javax.inject.Inject

const val KEY_SCREEN_STATE = "key_screen_state"

enum class PrepublishingScreen {
    HOME,
    PUBLISH,
    VISIBILITY,
    TAGS
}

/**
 * Stores the data state of each of these screens so that they can be saved in onSavedInstanceState & can be passed as
 * an argument if necessary.
 */
sealed class PrepublishingScreenState(val prepublishingScreen: PrepublishingScreen) : Parcelable {
    @Parcelize
    data class TagsState(val tags: String? = null) : PrepublishingScreenState(TAGS)

    // TODO add the other values for this state object.
    @Parcelize
    data class ActionsState(val tags: String? = null) : PrepublishingScreenState(HOME)
}

data class PrepublishingNavigationTarget(
    val site: SiteModel,
    val targetScreen: PrepublishingScreen,
    val screenState: PrepublishingScreenState?
)

class PrepublishingViewModel @Inject constructor(private val dispatcher: Dispatcher) : ViewModel() {
    private var isStarted = false
    private lateinit var site: SiteModel
    private var postRepository: EditPostRepository? = null
    private var tagsState: TagsState? = null
    private var currentScreenState: Pair<PrepublishingScreen, PrepublishingScreenState?>? = null

    private val _navigationTarget = MutableLiveData<Event<PrepublishingNavigationTarget>>()
    val navigationTarget: LiveData<Event<PrepublishingNavigationTarget>> = _navigationTarget

    private val _dismissBottomSheet = MutableLiveData<Event<Unit>>()
    val dismissBottomSheet: LiveData<Event<Unit>> = _dismissBottomSheet

    fun start(
        postRepository: EditPostRepository?,
        site: SiteModel,
        screenState: PrepublishingScreenState?
    ) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.postRepository = postRepository

        fetchTags()

        /**
         * Restores the specific state if it was persisted.
         */
        screenState?.let {
            restoreSavedScreenState(screenState)
            updateScreenStatesFromPostRepository()
            navigateToScreen(screenState.prepublishingScreen)
        } ?: run {
            updateScreenStatesFromPostRepository()
            navigateToScreen(HOME)
        }
    }

    /**
     * This exists because any specific screen could be in the process of making changes to a value and process death
     * occurs. So this is to restore the specific screen state that is impacted by this process.
     */
    private fun restoreSavedScreenState(screenState: PrepublishingScreenState) {
        when (screenState) {
            is TagsState -> tagsState = screenState
        }
        // TODO all the other screens that can be persisted.
    }

    private fun navigateToScreen(prepublishingScreen: PrepublishingScreen) {
        when (prepublishingScreen) {
            HOME -> updateNavigationTarget(PrepublishingNavigationTarget(site, HOME, ActionsState(tagsState?.tags)))
            TAGS -> updateNavigationTarget(
                    PrepublishingNavigationTarget(
                            site,
                            prepublishingScreen,
                            tagsState
                    )
            )
            PUBLISH -> TODO()
            VISIBILITY -> TODO()
        }
    }

    /**
     * Checks to see if state was restored for a specific action that was ongoing. If not, then the Post Repository
     * is used to create the state.
     */
    private fun updateScreenStatesFromPostRepository() {
        if (tagsState == null) {
            val tags = postRepository?.getPost()?.tagNameList
            val formattedTags = tags?.let {
                return@let formatTags(tags)
            } ?: run {
                throw IllegalArgumentException("Post or PostRepository can't be null.")
            }
            tagsState = TagsState(formattedTags)
        }
        // TODO add other states
    }

    /**
     * Clicking back triggers the save operation.
     */
    fun onBackClicked() {
        currentScreenState?.let { (screen, state) ->
            when (screen) {
                TAGS -> updateTagsAndState((state as? TagsState)?.tags)
                HOME -> TODO()
                PUBLISH -> TODO()
                VISIBILITY -> TODO()
            }
            clearCurrentScreenState()
            navigateToScreen(HOME)
        } ?: run {
            _dismissBottomSheet.postValue(Event(Unit))
        }
    }

    private fun updateNavigationTarget(target: PrepublishingNavigationTarget) {
        _navigationTarget.postValue(Event(target))
    }

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_SCREEN_STATE, currentScreenState?.second)
    }

    fun onActionClicked(actionType: ActionType) {
        val screen = PrepublishingScreen.valueOf(actionType.name)
        clearCurrentStateAndTrackCurrentScreen(screen)
        navigateToScreen(screen)
    }

    /**
     * clear the current screen state since we are back at the home screen.
     */
    private fun clearCurrentScreenState() {
        currentScreenState = null
    }

    private fun clearCurrentStateAndTrackCurrentScreen(prepublishingScreen: PrepublishingScreen) {
        currentScreenState = Pair(prepublishingScreen, null)
    }

    fun updateTagsStateAndSetToCurrent(tags: String) {
        tagsState = TagsState(tags)
        currentScreenState = Pair(TAGS, tagsState)
    }

    /**
     * This function updates the tags within the repository.
     */
    private fun updateTagsAndState(selectedTags: String?) {
        tagsState = TagsState(selectedTags)
        postRepository?.updateAsync({ postModel ->
            if (selectedTags != null && !TextUtils.isEmpty(selectedTags)) {
                val tags: String = selectedTags.replace("\n", " ")
                postModel.setTagNameList(TextUtils.split(tags, ",").toList())
            } else {
                postModel.setTagNameList(ArrayList())
            }
            true
        })
    }

    private fun formatTags(tags: List<String>): String {
        val formattedTags = TextUtils.join(",", tags)
        return StringEscapeUtils.unescapeHtml4(formattedTags)
    }

    /**
     * Fetches the tags so that they will be available when the Tags action is clicked
     */
    private fun fetchTags() {
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(site))
    }
}
