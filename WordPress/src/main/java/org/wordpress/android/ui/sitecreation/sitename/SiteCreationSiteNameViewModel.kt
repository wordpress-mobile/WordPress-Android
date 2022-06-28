package org.wordpress.android.ui.sitecreation.sitename

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class SiteCreationSiteNameViewModel @Inject constructor(
    private val analyticsTracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private var isInitialized = false

    private val _onSkipButtonPressed = SingleLiveEvent<Unit>()
    val onSkipButtonPressed: LiveData<Unit> = _onSkipButtonPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    private val _onSiteNameEntered = SingleLiveEvent<String>()
    val onSiteNameEntered: LiveData<String> = _onSiteNameEntered

    private val _uiState: MutableLiveData<SiteNameUiState> = MutableLiveData()
    val uiState: LiveData<SiteNameUiState> = _uiState

    fun start() {
        if (isInitialized) return
        analyticsTracker.trackSiteNameViewed()
        isInitialized = true
    }

    fun onSkipPressed() {
        analyticsTracker.trackSiteNameSkipped()
        _onSkipButtonPressed.call()
    }

    fun onBackPressed() {
        analyticsTracker.trackSiteNameCanceled()
        _onBackButtonPressed.call()
    }

    fun onSiteNameEntered() {
        uiState.value?.siteName.let {
            if (it.isNullOrBlank()) return
            analyticsTracker.trackSiteNameEntered(it)
            _onSiteNameEntered.value = it
        }
    }

    fun onSiteNameChanged(siteName: String) {
        _uiState.value = SiteNameUiState(siteName)
    }

    data class SiteNameUiState(val siteName: String) {
        val isContinueButtonEnabled get() = siteName.isNotBlank()
    }
}
