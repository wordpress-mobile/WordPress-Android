package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewConnectionErrorUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewGenericErrorUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.creation.FetchSiteUseCase
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceData
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000
private const val FETCH_SITE_NUMBER_OF_RETRIES = 5
private const val FETCH_SITE_BASE_RETRY_DELAY_IN_MILLIS = 1000

class NewSitePreviewViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val fetchSiteUseCase: FetchSiteUseCase,
    private val networkUtils: NetworkUtilsWrapper,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

    private lateinit var siteCreationState: SiteCreationState
    private lateinit var nonNullDomain: String
    private var lastReceivedServiceState: NewSiteCreationServiceState? = null
    private var serviceStateForRetry: NewSiteCreationServiceState? = null
    private var newlyCreatedSiteLocalId: Int? = null

    private val _uiState: MutableLiveData<SitePreviewUiState> = MutableLiveData()
    val uiState: LiveData<SitePreviewUiState> = _uiState

    private val _preloadPreview: SingleLiveEvent<String> = SingleLiveEvent()
    val preloadPreview: LiveData<String> = _preloadPreview

    private val _startCreateSiteService: SingleLiveEvent<SitePreviewStartServiceData> = SingleLiveEvent()
    val startCreateSiteService: LiveData<SitePreviewStartServiceData> = _startCreateSiteService

    private val _hideGetStartedBar: SingleLiveEvent<Unit> = SingleLiveEvent()
    val hideGetStartedBar: LiveData<Unit> = _hideGetStartedBar

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    private val _onOkButtonClicked = SingleLiveEvent<Int?>()
    val onOkButtonClicked: LiveData<Int?> = _onOkButtonClicked

    init {
        dispatcher.register(fetchSiteUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchSiteUseCase)
        job.cancel()
    }

    fun start(siteCreationState: SiteCreationState) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.siteCreationState = siteCreationState
        requireNotNull(siteCreationState.domain)
        nonNullDomain =  siteCreationState.domain!!

        updateUiState(SitePreviewFullscreenProgressUiState)
        startCreateSiteService()
    }

    private fun startCreateSiteService(previousState: NewSiteCreationServiceState? = null) {
        if (networkUtils.isNetworkAvailable()) {
            siteCreationState.apply {
                val serviceData = NewSiteCreationServiceData(
                        segmentId,
                        verticalId,
                        siteTitle,
                        siteTagLine,
                        nonNullDomain
                )
                _startCreateSiteService.value = SitePreviewStartServiceData(serviceData, previousState)
            }
        } else {
            showFullscreenErrorWithDelay()
        }
    }

    fun retry() {
        updateUiState(SitePreviewFullscreenProgressUiState)
        startCreateSiteService(serviceStateForRetry)
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    fun onOkButtonClicked() {
        /**
         * newlyCreatedSiteLocalId might be null if the fetchSite request failed or haven't finished yet.
         * The observer should handle this accordingly.
         */
        _onOkButtonClicked.value = newlyCreatedSiteLocalId
    }

    private fun showFullscreenErrorWithDelay() {
        updateUiState(SitePreviewFullscreenProgressUiState)
        launch {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            withContext(MAIN) {
                updateUiState(SitePreviewConnectionErrorUiState)
            }
        }
    }

    /**
     * The service automatically shows system notifications when site creation is in progress and the app is in
     * the background. We need to connect to the `AutoForeground` service from the View(Fragment), as only the View
     * knows when the app is in the background. Required parameter for `ServiceEventConnection` is also
     * the observer/listener of the `NewSiteCreationServiceState` (VM in our case), therefore we can't simply register
     * to the EventBus from the ViewModel and we have to use `sticky` events instead.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND, sticky = true)
    fun onSiteCreationServiceStateUpdated(event: NewSiteCreationServiceState) {
        if (lastReceivedServiceState == event) return // filter out events which we've already received
        lastReceivedServiceState = event
        when (event.step) {
            IDLE, CREATE_SITE -> {
            }// do nothing
            SUCCESS -> {
                startPreloadingWebView()
                fetchNewlyCreatedSiteModel(event.payload as Long)
            }
            FAILURE -> {
                serviceStateForRetry = event.payload as NewSiteCreationServiceState
                updateUiStateAsync(SitePreviewGenericErrorUiState(this::onHelpClicked))
            }
        }
    }

    /**
     * Fetch newly created site model - supports retry with linear backoff.
     */
    private fun fetchNewlyCreatedSiteModel(remoteSiteId: Long) {
        launch {
            repeat(FETCH_SITE_NUMBER_OF_RETRIES) { attemptNumber ->
                val onSiteFetched = fetchSiteUseCase.fetchSite(remoteSiteId, isWpCom = true)
                if (!onSiteFetched.isError) {
                    val siteBySiteId = siteStore.getSiteBySiteId(remoteSiteId)
                    if (siteBySiteId != null) {
                        newlyCreatedSiteLocalId = siteBySiteId.id
                    } else {
                        throw IllegalStateException("Site successfully fetched but has not been found in the local db.")
                    }
                    return@launch
                }
                delay((attemptNumber + 1) * FETCH_SITE_BASE_RETRY_DELAY_IN_MILLIS) // +1 -> starts from 0
            }
        }
    }

    private fun startPreloadingWebView() {
        _preloadPreview.postValue(UrlUtils.addUrlSchemeIfNeeded(nonNullDomain, true))
    }

    fun onUrlLoaded() {
        val urlShort = "$nonNullDomain.wordpress.com"
        val fullUrl = UrlUtils.addUrlSchemeIfNeeded(nonNullDomain, true)
        val subDomainIndices: Pair<Int, Int> = Pair(0, nonNullDomain.length)
        val domainIndices: Pair<Int, Int> = Pair(Math.min(subDomainIndices.second, urlShort.length), urlShort.length)

        updateUiState(SitePreviewContentUiState(SitePreviewData(fullUrl, urlShort, subDomainIndices, domainIndices)))
        _hideGetStartedBar.call()
    }

    private fun updateUiState(uiState: SitePreviewUiState) {
        _uiState.value = uiState
    }

    private fun updateUiStateAsync(uiState: SitePreviewUiState) {
        _uiState.postValue(uiState)
    }

    sealed class SitePreviewUiState(
        val fullscreenProgressLayoutVisibility: Boolean,
        val contentLayoutVisibility: Boolean,
        val fullscreenErrorLayoutVisibility: Boolean
    ) {
        data class SitePreviewContentUiState(val data: SitePreviewData) : SitePreviewUiState(
                fullscreenProgressLayoutVisibility = false,
                contentLayoutVisibility = true,
                fullscreenErrorLayoutVisibility = false
        )

        object SitePreviewFullscreenProgressUiState : SitePreviewUiState(
                fullscreenProgressLayoutVisibility = true,
                contentLayoutVisibility = false,
                fullscreenErrorLayoutVisibility = false
        ) {
            const val loadingTextResId = R.string.notification_new_site_creation_creating_site_subtitle
        }

        sealed class SitePreviewFullscreenErrorUiState constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null,
            open val onContactSupportTapped: (() -> Unit)? = null
        ) : SitePreviewUiState(
                fullscreenProgressLayoutVisibility = false,
                contentLayoutVisibility = false,
                fullscreenErrorLayoutVisibility = true
        ) {
            data class SitePreviewGenericErrorUiState(override val onContactSupportTapped: (() -> Unit)) :
                    SitePreviewFullscreenErrorUiState(
                            R.string.site_creation_error_generic_title,
                            R.string.site_creation_error_generic_subtitle,
                            onContactSupportTapped
                    )

            object SitePreviewConnectionErrorUiState : SitePreviewFullscreenErrorUiState(
                    R.string.no_network_message
            )
        }
    }

    data class SitePreviewData(
        val fullUrl: String,
        val shortUrl: String,
        val domainIndices: Pair<Int, Int>,
        val subDomainIndices: Pair<Int, Int>
    )

    data class SitePreviewStartServiceData(
        val serviceData: NewSiteCreationServiceData,
        val previousState: NewSiteCreationServiceState?
    )
}
