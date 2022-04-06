package org.wordpress.android.ui.sitecreation.sitename

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

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
        // TODO: analyticsTracker.trackSiteNameViewed()
        Log.d("siteNameWip", "Site name viewed")
        isInitialized = true
    }

    fun onSkipPressed() {
        // TODO: analyticsTracker.trackSiteNameSkipped()
        Log.d("siteNameWip", "Site name skipped")
        _onSkipButtonPressed.call()
    }

    fun onBackPressed() {
        // TODO: analyticsTracker.trackSiteNameCancelled()
        Log.d("siteNameWip", "Site name cancelled")
        _onBackButtonPressed.call()
    }

    fun onSiteNameEntered() {
        // TODO: analyticsTracker.trackSiteNameEntered(siteName?) maybe we don't want to track the names here?
        uiState.value?.siteName.let {
            if (it.isNullOrBlank()) return
            Log.d("siteNameWip", "Site name entered: $it")
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
