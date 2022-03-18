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
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState.DefaultIntentItemUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentsUiState.DefaultItems
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class SiteCreationIntentsViewModel @Inject constructor(
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
        // tracker.trackSiteIntentQuestionViewed()
    }

    fun onSkipPressed() {
        // tracker.trackSiteIntentQuestionSkipped()
        _onSkipButtonPressed.call()
    }

    fun onBackPressed() {
        // tracker.trackSiteIntentQuestionCanceled()
        _onBackButtonPressed.call()
    }

    fun updateUiState(uiState: IntentsUiState) {
        _uiState.value = uiState
    }

    fun initializeFromResources(resources: Resources) {
        val intentArray = resources.getStringArray(R.array.site_creation_intents_strings)
        val emojiArray = resources.getStringArray(R.array.site_creation_intents_emojis)
        if (intentArray.size != emojiArray.size) {
            throw Exception("Intents arrays size mismatch")
        }
        val newItems = intentArray.mapIndexed { index, verticalText ->
            val item = DefaultIntentItemUiState(verticalText, emojiArray[index])
            item.onItemTapped = { intentSelected(verticalText) }
            return@mapIndexed item
        }
        _uiState.value = DefaultItems(items = newItems)
    }

    private fun intentSelected(intent: String) {
        _onIntentSelected.value = intent
    }

    sealed class IntentsUiState(
        val items: List<IntentListItemUiState>
    ) {
        class DefaultItems(items: List<IntentListItemUiState>) : IntentsUiState(
                items = items
        )
    }

    sealed class IntentListItemUiState {
        var onItemTapped: (() -> Unit)? = null

        data class DefaultIntentItemUiState(
            val verticalText: String,
            val emoji: String
        ) : IntentListItemUiState()
    }
}
