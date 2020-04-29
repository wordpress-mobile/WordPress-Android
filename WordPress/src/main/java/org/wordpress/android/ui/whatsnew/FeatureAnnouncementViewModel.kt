package org.wordpress.android.ui.whatsnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class FeatureAnnouncementViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiModel = MutableLiveData<FeatureAnnouncementUiModel>()
    val uiModel: LiveData<FeatureAnnouncementUiModel> = _uiModel

    private val _onDialogClosed = SingleLiveEvent<Unit>()
    val onDialogClosed: LiveData<Unit> = _onDialogClosed

    private val _onAnnouncementDetailsRequested = SingleLiveEvent<String>()
    val onAnnouncementDetailsRequested: LiveData<String> = _onAnnouncementDetailsRequested

    private val _features = MutableLiveData<List<FeatureAnnouncementItem>>()
    val features: LiveData<List<FeatureAnnouncementItem>> = _features

    private var isStarted = false

    private lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider

    fun start(featureAnnouncementProvider: FeatureAnnouncementProvider) {
        if (isStarted) return
        isStarted = true

        this.featureAnnouncementProvider = featureAnnouncementProvider

        _uiModel.value = FeatureAnnouncementUiModel(isProgressVisible = true)

        loadFeatures()
    }

    private fun loadFeatures() {
        launch {
            delay(3000)
            _features.value = featureAnnouncementProvider.getAnnouncementFeatures()
            _uiModel.value = _uiModel.value?.copy(
                    appVersion = featureAnnouncementProvider.getAnnouncementAppVersion(),
                    isProgressVisible = false
            )
        }
    }

    fun onCloseDialogButtonPressed() {
        _onDialogClosed.call()
    }

    fun onFindMoreButtonPressedPressed() {
        _onAnnouncementDetailsRequested.value = featureAnnouncementProvider.getAnnouncementDetailsUrl()
    }

    data class FeatureAnnouncementUiModel(
        val appVersion: String = "",
        val isProgressVisible: Boolean = false
    )
}
