package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingScreenState.HomeState
import org.wordpress.android.ui.posts.PrepublishingScreenState.TagsState
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.PrepublishingScreen.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingScreen.TAGS
import org.wordpress.android.ui.posts.PrepublishingScreen.VISIBILITY
import org.wordpress.android.viewmodel.Event
import java.lang.IllegalArgumentException
import java.util.ArrayList
import javax.inject.Inject

const val KEY_TAGS_ACTION_STATE = "key_tags_action_state"

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
    data class HomeState(val tags: String? = null) : PrepublishingScreenState(HOME)
}

data class PrepublishingNavigationTarget(
    val site: SiteModel,
    val targetScreen: PrepublishingScreen,
    val screenState: PrepublishingScreenState?
)

class PrepublishingViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false
    private lateinit var site: SiteModel
    private var postRepository: EditPostRepository? = null
    private var tagsState: TagsState? = null
    private var currentScreenState: Pair<PrepublishingScreen, PrepublishingScreenState?>? = null

    private val _navigationTarget = MutableLiveData<Event<PrepublishingNavigationTarget>>()
    val navigationTarget: LiveData<Event<PrepublishingNavigationTarget>> = _navigationTarget

    fun start(
        postRepository: EditPostRepository?,
        site: SiteModel,
        screenState: PrepublishingScreenState?
    ) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.postRepository = postRepository

        /**
         * Restores the specific state if it was persisted.
         */
        screenState?.let {
            restoreCurrentScreenState(screenState)
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
    private fun restoreCurrentScreenState(screenState: PrepublishingScreenState) {
        when (screenState) {
            is TagsState -> tagsState = screenState
        }
    }

    private fun navigateToScreen(prepublishingScreen: PrepublishingScreen) {
        when (prepublishingScreen) {
            HOME -> updateNavigationTarget(PrepublishingNavigationTarget(site, HOME, HomeState(tagsState?.tags)))
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

    fun onCloseClicked() {
        currentScreenState?.let { (screen, state) ->
            when (screen) {
                TAGS -> updateTags((state as TagsState).tags)
                HOME -> TODO()
                PUBLISH -> TODO()
                VISIBILITY -> TODO()
            }
        }

        navigateToScreen(HOME)
    }

    private fun updateNavigationTarget(target: PrepublishingNavigationTarget) {
        _navigationTarget.postValue(Event(target))
    }

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_TAGS_ACTION_STATE, currentScreenState?.second)
    }

    fun onActionClicked(actionType: ActionType) {
        val screen = when (actionType) {
            ActionType.TAGS -> {
                currentScreenState = Pair(TAGS, null)
                TAGS
            }
            ActionType.PUBLISH -> TODO()
            ActionType.VISIBILITY -> TODO()
        }
        navigateToScreen(screen)
    }

    fun updateTagsState(tags: String) {
        tagsState = TagsState(tags)
        currentScreenState = Pair(TAGS, tagsState)
    }

    /**
     * This function updates the tags within the repository.
     */
    private fun updateTags(selectedTags: String?) {
        postRepository?.updateAsync({ postModel ->
            if (selectedTags != null && !TextUtils.isEmpty(selectedTags)) {
                val tags: String = selectedTags.replace("\n", " ")
                postModel.setTagNameList(TextUtils.split(tags, ",").toList())
            } else {
                postModel.setTagNameList(ArrayList())
            }
            true
        }, onCompleted = { postModel, result ->
            if (result == UpdatePostResult.Updated) {
                tagsState = TagsState(formatTags(postModel.tagNameList))
            }
        })
    }

    private fun formatTags(tags: List<String>): String {
        val formattedTags = TextUtils.join(",", tags)
        return StringEscapeUtils.unescapeHtml4(formattedTags)
    }
}
