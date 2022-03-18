package org.wordpress.android.ui.sitecreation.verticals

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentListItemUiState.DefaultIntentItemUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel.IntentsUiState.DefaultItems
import org.wordpress.android.viewmodel.SingleLiveEvent
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

    fun start() {
        if (isStarted) return
        isStarted = true
        // tracker.trackSiteIntentQuestionViewed()
        val initialState = DefaultItems(items = listOf(
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Art", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Business", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Fashion", drawable.ic_wordpress_white_24dp),
                DefaultIntentItemUiState("Finance", drawable.ic_wordpress_white_24dp),
        ))
        initialState.items.forEach {
            val message = (it as DefaultIntentItemUiState).verticalText + " selected"
            it.onItemTapped = { Log.d("Intents", message) }
        }
        updateUiState(initialState)
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
            @DrawableRes val verticalIconResId: Int
        ) : IntentListItemUiState()
    }
}
