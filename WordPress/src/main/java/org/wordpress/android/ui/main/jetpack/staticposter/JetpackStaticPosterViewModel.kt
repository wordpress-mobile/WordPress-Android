package org.wordpress.android.ui.main.jetpack.staticposter

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

private const val KEY_SOURCE = "source"

@HiltViewModel
class JetpackStaticPosterViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ViewModel() {
    private var isStarted = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private lateinit var data: UiData

    fun start(uiData: UiData) {
        if (isStarted) return else isStarted = true
        data = uiData
        _uiState.value = data.toContentUiState()
        trackStart()
    }

    fun onPrimaryClick() {
        trackPrimaryClick()
        _events.tryEmit(Event.PrimaryButtonClick)
    }

    fun onSecondaryClick() {
        trackSecondaryClick()
        _events.tryEmit(Event.SecondaryButtonClick)
    }

    private fun trackStart() {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_STATIC_POSTER_DISPLAYED,
            mapOf(KEY_SOURCE to data.screen.trackingName)
        )
    }

    private fun trackPrimaryClick() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.JETPACK_STATIC_POSTER_GET_JETPACK_TAPPED,
        mapOf(KEY_SOURCE to data.screen.trackingName)
    )

    private fun trackSecondaryClick() = analyticsTrackerWrapper.track(
        AnalyticsTracker.Stat.JETPACK_STATIC_POSTER_LINK_TAPPED,
        mapOf(KEY_SOURCE to data.screen.trackingName)
    )
}

sealed class UiState {
    object Loading : UiState()
    data class Content(
        val showTopBar: Boolean,
    ) : UiState()
}

sealed class Event {
    object PrimaryButtonClick : Event()
    object SecondaryButtonClick : Event()
}

typealias UiData = JetpackPoweredScreen.WithStaticPoster

fun UiData.toContentUiState() = UiState.Content(
    showTopBar = this == UiData.STATS,
)
