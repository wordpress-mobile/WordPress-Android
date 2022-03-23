package org.wordpress.android.ui.sitecreation.verticals

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState.DefaultIntentItemUiState
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class SiteCreationIntentsViewModel @Inject constructor(
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private var isStarted = false

    private val _uiState: MutableLiveData<IntentsUiState> = MutableLiveData()
    val uiState: LiveData<IntentsUiState> = _uiState

    private val _onSkipButtonPressed = SingleLiveEvent<Unit>()
    val onSkipButtonPressed: LiveData<Unit> = _onSkipButtonPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    private val _onIntentSelected = SingleLiveEvent<String>()
    val onIntentSelected: LiveData<String> = _onIntentSelected

    fun start() {
        if (isStarted) return
        isStarted = true
        analyticsTracker.trackSiteIntentQuestionViewed()
    }

    fun onSkipPressed() {
        analyticsTracker.trackSiteIntentQuestionSkipped()
        _onSkipButtonPressed.call()
    }

    fun onBackPressed() {
        analyticsTracker.trackSiteIntentQuestionCanceled()
        _onBackButtonPressed.call()
    }

    fun updateUiState(uiState: IntentsUiState) {
        _uiState.value = uiState
    }

    fun initializeFromResources(resources: Resources) {
        val slugsArray = resources.getStringArray(R.array.site_creation_intents_slugs)
        val verticalArray = resources.getStringArray(R.array.site_creation_intents_strings)
        val emojiArray = resources.getStringArray(R.array.site_creation_intents_emojis)
        if (slugsArray.size != verticalArray.size || slugsArray.size != emojiArray.size) {
            throw IllegalStateException("Intents arrays size mismatch")
        }
        val newItems = slugsArray.mapIndexed { index, slug ->
            val vertical = verticalArray[index]
            val emoji = emojiArray[index]
            val item = DefaultIntentItemUiState(slug, vertical, emoji)
            item.onItemTapped = { intentSelected(slug, vertical) }
            return@mapIndexed item
        }
        _uiState.value = IntentsUiState(
                content = IntentsUiState.Content.DefaultItems(items = newItems)
        )
    }

    private fun intentSelected(slug: String, vertical: String) {
        // TODO: determine what slugs (and ids) to use for the default intents
        analyticsTracker.trackSiteIntentQuestionVerticalSelected(vertical, slug)
        _onIntentSelected.value = vertical
    }

    /**
     * Appbar scrolled event used to set the header and title visibility
     * @param verticalOffset the scroll state vertical offset
     * @param scrollThreshold the scroll threshold
     */
    fun onAppBarOffsetChanged(verticalOffset: Int, scrollThreshold: Int) {
        val shouldAppBarTitleBeVisible = verticalOffset < scrollThreshold

        uiState.value?.let { state ->
            if (state.isAppBarTitleVisible == shouldAppBarTitleBeVisible || !state.isHeaderVisible) return
            updateUiState(
                    state.copy(isAppBarTitleVisible = shouldAppBarTitleBeVisible)
            )
        }
    }

    fun onInputFocused() {
        uiState.value?.let { state ->
            if (!state.isHeaderVisible) return
            updateUiState(
                    state.copy(
                            isAppBarTitleVisible = true,
                            isHeaderVisible = false
                    )
            )
        }
    }

    data class IntentsUiState(
        val isAppBarTitleVisible: Boolean = false,
        val isHeaderVisible: Boolean = true,
        val content: Content
    ) {
        sealed class Content(
            val items: List<IntentListItemUiState>
        ) {
            class DefaultItems(
                items: List<IntentListItemUiState>
            ) : Content(items = items)
        }
    }

    sealed class IntentListItemUiState {
        var onItemTapped: (() -> Unit)? = null

        data class DefaultIntentItemUiState(
            val verticalSlug: String,
            val verticalText: String,
            val emoji: String
        ) : IntentListItemUiState()
    }
}
