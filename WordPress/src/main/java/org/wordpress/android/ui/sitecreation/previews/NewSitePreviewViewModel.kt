package org.wordpress.android.ui.sitecreation.previews

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState.SiteNotInLocalDb
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewConnectionErrorUiState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewGenericErrorUiState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceData
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceState.NewSiteCreationStep.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000L
private const val DELAY_TO_SHOW_WEB_VIEW_LOADING_SHIMMER = 1000L
private const val ERROR_CONTEXT = "site_preview"

class NewSitePreviewViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val fetchWpComSiteUseCase: FetchWpComSiteUseCase,
    private val networkUtils: NetworkUtilsWrapper,
    private val urlUtils: UrlUtilsWrapper,
    private val tracker: NewSiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false
    private var webviewFullyLoadedTracked = false

    private lateinit var siteCreationState: SiteCreationState
    private lateinit var urlWithoutScheme: String
    private var lastReceivedServiceState: NewSiteCreationServiceState? = null
    private var serviceStateForRetry: NewSiteCreationServiceState? = null
    private var createSiteState: CreateSiteState = CreateSiteState.SiteNotCreated

    private val _uiState: MutableLiveData<SitePreviewUiState> = MutableLiveData()
    val uiState: LiveData<SitePreviewUiState> = _uiState

    private val _preloadPreview: MutableLiveData<String> = MutableLiveData()
    val preloadPreview: LiveData<String> = _preloadPreview

    private val _startCreateSiteService: SingleLiveEvent<SitePreviewStartServiceData> = SingleLiveEvent()
    val startCreateSiteService: LiveData<SitePreviewStartServiceData> = _startCreateSiteService

    private val _hideGetStartedBar: SingleLiveEvent<Unit> = SingleLiveEvent()
    val hideGetStartedBar: LiveData<Unit> = _hideGetStartedBar

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    private val _onCancelWizardClicked = SingleLiveEvent<CreateSiteState>()
    val onCancelWizardClicked: LiveData<CreateSiteState> = _onCancelWizardClicked

    private val _onOkButtonClicked = SingleLiveEvent<CreateSiteState>()
    val onOkButtonClicked: LiveData<CreateSiteState> = _onOkButtonClicked

    private val _onSiteCreationCompleted = SingleLiveEvent<CreateSiteState>()
    val onSiteCreationCompleted: LiveData<CreateSiteState> = _onSiteCreationCompleted

    init {
        dispatcher.register(fetchWpComSiteUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchWpComSiteUseCase)
        job.cancel()
    }

    fun start(siteCreationState: SiteCreationState) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.siteCreationState = siteCreationState
        urlWithoutScheme = requireNotNull(siteCreationState.domain)

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
                        urlUtils.extractSubDomain(urlWithoutScheme)
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

    fun onCancelWizardClicked() {
        _onCancelWizardClicked.value = createSiteState
    }

    fun onOkButtonClicked() {
        tracker.trackCreationCompleted()
        _onOkButtonClicked.value = createSiteState
    }

    private fun showFullscreenErrorWithDelay() {
        updateUiState(SitePreviewFullscreenProgressUiState)
        launch {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            tracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR)
            withContext(mainDispatcher) {
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
    @Suppress("unused")
    fun onSiteCreationServiceStateUpdated(event: NewSiteCreationServiceState) {
        if (lastReceivedServiceState == event) return // filter out events which we've already received
        lastReceivedServiceState = event
        when (event.step) {
            IDLE, CREATE_SITE -> {
            } // do nothing
            SUCCESS -> {
                startPreLoadingWebView()
                val remoteSiteId = event.payload as Long
                createSiteState = SiteNotInLocalDb(remoteSiteId)
                fetchNewlyCreatedSiteModel(remoteSiteId)
                _onSiteCreationCompleted.asyncCall()
            }
            FAILURE -> {
                serviceStateForRetry = event.payload as NewSiteCreationServiceState
                tracker.trackErrorShown(
                        ERROR_CONTEXT,
                        UNKNOWN,
                        "NewSiteCreation service failed"
                )
                updateUiStateAsync(SitePreviewGenericErrorUiState)
            }
        }
    }

    /**
     * Fetch newly created site model - supports retry with linear backoff.
     */
    private fun fetchNewlyCreatedSiteModel(remoteSiteId: Long) {
        launch {
            val onSiteFetched = fetchWpComSiteUseCase.fetchSiteWithRetry(remoteSiteId)
            createSiteState = if (!onSiteFetched.isError) {
                val siteBySiteId = requireNotNull(siteStore.getSiteBySiteId(remoteSiteId)) {
                    "Site successfully fetched but has not been found in the local db."
                }
                CreateSiteState.SiteCreationCompleted(siteBySiteId.id)
            } else {
                CreateSiteState.SiteNotInLocalDb(remoteSiteId)
            }
        }
    }

    private fun startPreLoadingWebView() {
        tracker.trackPreviewLoading()
        launch {
            /**
             * Keep showing the full screen loading screen for 1 more second or until the webview is loaded whichever
             * happens first. This will give us some more time to fetch the newly created site.
             */
            delay(DELAY_TO_SHOW_WEB_VIEW_LOADING_SHIMMER)
            /**
             * If the webview is still not loaded after some delay, we'll show the loading shimmer animation instead
             * of the full screen progress, so the user is not blocked for taking actions.
             */
            withContext(mainDispatcher) {
                if (uiState.value !is SitePreviewContentUiState) {
                    tracker.trackPreviewWebviewShown()
                    updateUiState(SitePreviewLoadingShimmerState(createSitePreviewData()))
                }
            }
        }
        // Load the newly created site in the webview
        _preloadPreview.postValue(urlUtils.addUrlSchemeIfNeeded(urlWithoutScheme, true))
    }

    fun onUrlLoaded() {
        _hideGetStartedBar.call()
        if (!webviewFullyLoadedTracked) {
            webviewFullyLoadedTracked = true
            tracker.trackPreviewWebviewFullyLoaded()
        }
        if (uiState.value is SitePreviewFullscreenProgressUiState) {
            tracker.trackPreviewWebviewShown()
        }
        /**
         * Update the ui state if the loading or error screen is being shown.
         * In other words don't update it after a configuration change.
         */
        if (uiState.value !is SitePreviewContentUiState) {
            updateUiState(SitePreviewContentUiState(createSitePreviewData()))
        }
    }

    private fun createSitePreviewData(): SitePreviewData {
        val subDomain = urlUtils.extractSubDomain(urlWithoutScheme)
        val fullUrl = urlUtils.addUrlSchemeIfNeeded(urlWithoutScheme, true)
        val subDomainIndices: Pair<Int, Int> = Pair(0, subDomain.length)
        val domainIndices: Pair<Int, Int> = Pair(
                Math.min(subDomainIndices.second, urlWithoutScheme.length),
                urlWithoutScheme.length
        )
        return SitePreviewData(
                fullUrl,
                urlWithoutScheme,
                subDomainIndices,
                domainIndices
        )
    }

    private fun updateUiState(uiState: SitePreviewUiState) {
        _uiState.value = uiState
    }

    private fun updateUiStateAsync(uiState: SitePreviewUiState) {
        _uiState.postValue(uiState)
    }

    sealed class SitePreviewUiState(
        val fullscreenProgressLayoutVisibility: Boolean = false,
        val contentLayoutVisibility: Boolean = false,
        val webViewVisibility: Boolean = false,
        val shimmerVisibility: Boolean = false,
        val fullscreenErrorLayoutVisibility: Boolean = false
    ) {
        data class SitePreviewContentUiState(val data: SitePreviewData) : SitePreviewUiState(
                contentLayoutVisibility = true,
                webViewVisibility = true
        )

        data class SitePreviewLoadingShimmerState(val data: SitePreviewData) : SitePreviewUiState(
                contentLayoutVisibility = true,
                shimmerVisibility = true
        )

        object SitePreviewFullscreenProgressUiState : SitePreviewUiState(
                fullscreenProgressLayoutVisibility = true
        ) {
            const val loadingTextResId = R.string.notification_new_site_creation_creating_site_subtitle
        }

        sealed class SitePreviewFullscreenErrorUiState constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null,
            val showContactSupport: Boolean = false,
            val showCancelWizardButton: Boolean = true
        ) : SitePreviewUiState(
                fullscreenErrorLayoutVisibility = true
        ) {
            object SitePreviewGenericErrorUiState :
                    SitePreviewFullscreenErrorUiState(
                            R.string.site_creation_error_generic_title,
                            R.string.site_creation_error_generic_subtitle,
                            showContactSupport = true
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

    sealed class CreateSiteState {
        /**
         * CreateSite request haven't finished yet or failed.
         */
        object SiteNotCreated : CreateSiteState()

        /**
         * FetchSite request haven't finished yet or failed.
         * Since we fetch the site without user awareness in background, the user may potentially leave the screen
         * before the request is finished.
         */
        data class SiteNotInLocalDb(val remoteSiteId: Long) : CreateSiteState()

        /**
         * The site has been successfully created and stored into local db.
         */
        data class SiteCreationCompleted(val localSiteId: Int) : CreateSiteState()
    }
}
