package org.wordpress.android.ui.sitecreation.progress

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.SiteCreationResult
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotCreated
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotInLocalDb
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Error.ConnectionError
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Error.GenericError
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Loading
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

const val KEY_RESULT = "KEY_RESULT"
private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000L
const val LOADING_STATE_TEXT_ANIMATION_DELAY = 2000L
private const val ERROR_CONTEXT = "site_preview"

private val loadingTexts = listOf(
    UiStringRes(R.string.new_site_creation_creating_site_loading_1),
    UiStringRes(R.string.new_site_creation_creating_site_loading_2),
    UiStringRes(R.string.new_site_creation_creating_site_loading_3),
    UiStringRes(R.string.new_site_creation_creating_site_loading_4)
)

@HiltViewModel
class SiteProgressViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val urlUtils: UrlUtilsWrapper,
    private val tracker: SiteCreationTracker,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext get() = mainDispatcher + job
    private var isStarted = false
    private var loadingAnimationJob: Job? = null

    private lateinit var siteCreationState: SiteCreationState
    private lateinit var result: SiteCreationResult

    private var urlWithoutScheme: String? = null
    private var siteTitle: String? = null
    private var lastReceivedServiceState: SiteCreationServiceState? = null
    private var serviceStateForRetry: SiteCreationServiceState? = null

    private val _uiState: MutableLiveData<SiteProgressUiState> = MutableLiveData()
    val uiState: LiveData<SiteProgressUiState> = _uiState

    private val _startCreateSiteService: SingleLiveEvent<StartServiceData> = SingleLiveEvent()
    val startCreateSiteService: LiveData<StartServiceData> = _startCreateSiteService

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    private val _onCancelWizardClicked = SingleLiveEvent<SiteCreationResult>()
    val onCancelWizardClicked: LiveData<SiteCreationResult> = _onCancelWizardClicked

    private val _onSiteCreationCompleted = SingleLiveEvent<SiteCreationResult>()
    val onSiteCreationCompleted: LiveData<SiteCreationResult> = _onSiteCreationCompleted

    override fun onCleared() {
        super.onCleared()
        job.cancel()
        loadingAnimationJob?.cancel()
    }

    fun writeToBundle(outState: Bundle) = outState.putParcelable(KEY_RESULT, result)

    fun start(siteCreationState: SiteCreationState, savedState: Bundle?) {
        if (isStarted) return
        isStarted = true
        this.siteCreationState = siteCreationState
        urlWithoutScheme = siteCreationState.domain?.domainName
        siteTitle = siteCreationState.siteName

        val restoredResult = savedState?.getParcelable<SiteCreationResult>(KEY_RESULT)
        result = restoredResult ?: NotCreated
        when (result) {
            NotCreated -> {
                runLoadingAnimationUi()
                startCreateSiteService()
            }
            is NotInLocalDb -> {
                runLoadingAnimationUi()
                _onSiteCreationCompleted.postValue(result)
            }
            else -> Unit
        }
    }

    private fun startCreateSiteService(previousState: SiteCreationServiceState? = null) {
        if (networkUtils.isNetworkAvailable()) {
            siteCreationState.apply {
                // A non-null [segmentId] may invalidate the [siteDesign] selection
                // https://github.com/wordpress-mobile/WordPress-Android/issues/13749
                val segmentIdentifier = segmentId.takeIf { siteDesign != null }
                val serviceData = SiteCreationServiceData(
                    segmentIdentifier,
                    siteDesign,
                    urlWithoutScheme,
                    siteTitle
                )
                _startCreateSiteService.value = StartServiceData(serviceData, previousState)
            }
        } else {
            showFullscreenErrorWithDelay()
        }
    }

    fun retry() {
        runLoadingAnimationUi()
        startCreateSiteService(serviceStateForRetry)
    }

    fun onHelpClicked() = _onHelpClicked.call()

    fun onCancelWizardClicked() {
        _onCancelWizardClicked.value = result
    }

    private fun showFullscreenErrorWithDelay() {
        runLoadingAnimationUi()
        launch(mainDispatcher) {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            tracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR)
            updateUiState(ConnectionError)
        }
    }

    /**
     * The service automatically shows system notifications when site creation is in progress and the app is in
     * the background. We need to connect to the `AutoForeground` service from the View(Fragment), as only the View
     * knows when the app is in the background. Required parameter for `ServiceEventConnection` is also
     * the observer/listener of the `SiteCreationServiceState` (VM in our case), therefore we can't simply register
     * to the EventBus from the ViewModel and we have to use `sticky` events instead.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND, sticky = true)
    @Suppress("unused")
    fun onSiteCreationServiceStateUpdated(event: SiteCreationServiceState) {
        if (lastReceivedServiceState == event) return // filter out events which we've already received
        lastReceivedServiceState = event
        when (event.step) {
            IDLE, CREATE_SITE -> Unit
            SUCCESS -> {
                val remoteSiteId = (event.payload as Pair<*, *>).first as Long
                urlWithoutScheme = urlUtils.removeScheme(event.payload.second as String).trimEnd('/')
                result = NotInLocalDb(remoteSiteId, !siteTitle.isNullOrBlank())
                _onSiteCreationCompleted.postValue(result)
            }
            FAILURE -> {
                serviceStateForRetry = event.payload as SiteCreationServiceState
                tracker.trackErrorShown(
                    ERROR_CONTEXT,
                    UNKNOWN,
                    "SiteCreation service failed"
                )
                updateUiStateAsync(GenericError)
            }
        }
    }

    private fun runLoadingAnimationUi() {
        loadingAnimationJob?.cancel()
        loadingAnimationJob = launch(mainDispatcher) {
            loadingTexts.forEachIndexed { i, uiString ->
                updateUiState(
                    Loading(
                        animate = i != 0, // the first text should appear without an animation
                        loadingTextResId = uiString
                    )
                )
                delay(LOADING_STATE_TEXT_ANIMATION_DELAY)
            }
        }
    }

    private fun updateUiState(uiState: SiteProgressUiState) {
        if (uiState !is Loading) loadingAnimationJob?.cancel()
        _uiState.value = uiState
    }

    private fun updateUiStateAsync(uiState: SiteProgressUiState) {
        if (uiState !is Loading) loadingAnimationJob?.cancel()
        _uiState.postValue(uiState)
    }

    sealed class SiteProgressUiState(
        val progressLayoutVisibility: Boolean = false,
        val errorLayoutVisibility: Boolean = false
    ) {
        data class Loading(val loadingTextResId: UiString, val animate: Boolean) :
            SiteProgressUiState(progressLayoutVisibility = true)

        sealed class Error constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null,
            val showContactSupport: Boolean = false,
            val showCancelWizardButton: Boolean = true
        ) : SiteProgressUiState(errorLayoutVisibility = true) {
            object GenericError : Error(
                R.string.site_creation_error_generic_title,
                R.string.site_creation_error_generic_subtitle,
                showContactSupport = true
            )

            object ConnectionError : Error(
                R.string.no_network_message
            )
        }
    }

    data class StartServiceData(
        val serviceData: SiteCreationServiceData,
        val previousState: SiteCreationServiceState?
    )
}
