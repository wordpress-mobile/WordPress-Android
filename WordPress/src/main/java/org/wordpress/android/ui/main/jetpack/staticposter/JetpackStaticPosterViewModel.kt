package org.wordpress.android.ui.main.jetpack.staticposter

import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.PhaseThreeBlogPostLinkConfig
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

private const val KEY_SOURCE = "source"

@HiltViewModel
class JetpackStaticPosterViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val phaseThreeBlogPostLinkConfig: PhaseThreeBlogPostLinkConfig,
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _events = SingleLiveEvent<Event>()
    val events = _events as LiveData<Event>

    private lateinit var data: UiData

    fun start(uiData: UiData) {
        if (!isStarted || uiData != data) trackStart(uiData.screen.trackingName)
        if (isStarted) return else isStarted = true
        data = uiData
        _uiState.value = data.toContentUiState()
    }

    fun onPrimaryClick() {
        trackPrimaryClick()
        launch { _events.value = Event.PrimaryButtonClick }
    }

    fun onSecondaryClick() {
        trackSecondaryClick()
        launch { _events.value = Event.SecondaryButtonClick(phaseThreeBlogPostLinkConfig.getValue()) }
    }

    private fun trackStart(source: String) = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.REMOVE_STATIC_POSTER_DISPLAYED,
        mapOf(KEY_SOURCE to source)
    )

    private fun trackPrimaryClick() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.REMOVE_STATIC_POSTER_GET_JETPACK_TAPPED,
        mapOf(KEY_SOURCE to data.screen.trackingName)
    )

    private fun trackSecondaryClick() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.REMOVE_STATIC_POSTER_LINK_TAPPED,
        mapOf(KEY_SOURCE to data.screen.trackingName)
    )
}

sealed class UiState {
    object Loading : UiState()
    data class Content(
        val showTopBar: Boolean,
        val featureName: UiString,
        val showPluralTitle: Boolean
        ) : UiState()
}

sealed class Event {
    object PrimaryButtonClick : Event()
    data class SecondaryButtonClick(val url: String?) : Event()
}

typealias UiData = JetpackPoweredScreen.WithStaticPoster

fun UiData.toContentUiState() = UiState.Content(
    showTopBar = this == UiData.STATS,
    featureName = screen.featureName,
    showPluralTitle = screen.isPlural
)
