package org.wordpress.android.ui.whatsnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class FeatureAnnouncementViewModel @Inject constructor(
    private val featureAnnouncementProvider: FeatureAnnouncementProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _currentFeatureAnnouncement = MutableLiveData<FeatureAnnouncement>()

    private val timeOnScreenParameter = "time_on_screen_sec"
    private var screenTimeStart = 0L

    private val _uiModel = MediatorLiveData<FeatureAnnouncementUiModel>()
    val uiModel: LiveData<FeatureAnnouncementUiModel> = _uiModel

    private val _onDialogClosed = SingleLiveEvent<Unit>()
    val onDialogClosed: LiveData<Unit> = _onDialogClosed

    private val _onAnnouncementDetailsRequested = SingleLiveEvent<String>()
    val onAnnouncementDetailsRequested: LiveData<String> = _onAnnouncementDetailsRequested

    private val _featureItems = MediatorLiveData<List<FeatureAnnouncementItem>>()
    val featureItems: LiveData<List<FeatureAnnouncementItem>> = _featureItems

    private var isStarted = false

    init {
        _uiModel.addSource(_currentFeatureAnnouncement) { featureAnnouncement ->
            _uiModel.value = _uiModel.value?.copy(
                    appVersion = featureAnnouncement.appVersionName, isProgressVisible = false
            )
        }

        _featureItems.addSource(_currentFeatureAnnouncement) { featureAnnouncement ->
            _featureItems.value = featureAnnouncement.features
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        screenTimeStart = System.currentTimeMillis()

        _uiModel.value = FeatureAnnouncementUiModel(isProgressVisible = true)

        loadFeatures()
    }

    private fun loadFeatures() {
        launch {
            delay(3000)
            _currentFeatureAnnouncement.value = featureAnnouncementProvider.getLatestFeatureAnnouncement()
        }
    }

    fun onCloseDialogButtonPressed() {
        val timeOnScreen = (System.currentTimeMillis() - screenTimeStart) / 1000
        analyticsTrackerWrapper.track(
                Stat.FEATURE_ANNOUNCEMENT_CLOSE_DIALOG_BUTTON_TAPPED,
                mapOf(timeOnScreenParameter to timeOnScreen)
        )
        _onDialogClosed.call()
    }

    fun onFindMoreButtonPressed() {
        analyticsTrackerWrapper.track(Stat.FEATURE_ANNOUNCEMENT_FIND_OUT_MORE_TAPPED)
        _onAnnouncementDetailsRequested.value = _currentFeatureAnnouncement.value?.detailsUrl
    }

    data class FeatureAnnouncementUiModel(
        val appVersion: String = "",
        val isProgressVisible: Boolean = false
    )
}
