package org.wordpress.android.ui.whatsnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.WordPress
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

    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true

        _uiModel.value = FeatureAnnouncementUiModel(WordPress.versionName, isProgressVisible = true)

        launch {
            delay(3000)
            _uiModel.value = _uiModel.value?.copy(isProgressVisible = false)
        }
    }

    fun onCloseDialogButtonPressed() {
        _onDialogClosed.call()
    }

    data class FeatureAnnouncementUiModel(
        val appVersion: String,
        val isProgressVisible: Boolean = false
    )
}
