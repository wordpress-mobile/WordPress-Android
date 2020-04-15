package org.wordpress.android.ui.sitecreation.previews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteNotInLocalDb
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewConnectionErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewGenericErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.usecases.isWordPressComSubDomain
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE = 1000L
private const val DELAY_TO_SHOW_WEB_VIEW_LOADING_SHIMMER = 1000L
const val LOADING_STATE_TEXT_ANIMATION_DELAY = 2000L
private const val ERROR_CONTEXT = "site_preview"

private val loadingTexts = listOf(
        UiStringRes(R.string.new_site_creation_creating_site_loading_1),
        UiStringRes(R.string.new_site_creation_creating_site_loading_2),
        UiStringRes(R.string.new_site_creation_creating_site_loading_3),
        UiStringRes(R.string.new_site_creation_creating_site_loading_4)
)

class SitePreviewViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val fetchWpComSiteUseCase: FetchWpComSiteUseCase,
    private val networkUtils: NetworkUtilsWrapper,
    private val urlUtils: UrlUtilsWrapper,
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false
    private var webviewFullyLoadedTracked = false
    private var loadingAnimationJob: Job? = null

    private lateinit var siteCreationState: SiteCreationState
    private lateinit var urlWithoutScheme: String
    private var lastReceivedServiceState: SiteCreationServiceState? = null
    private var serviceStateForRetry: SiteCreationServiceState? = null
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
        loadingAnimationJob?.cancel()
    }

    fun start(siteCreationState: SiteCreationState) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.siteCreationState = siteCreationState
        urlWithoutScheme = requireNotNull(siteCreationState.domain)

        showFullscreenProgress()
        startCreateSiteService()
    }

    private fun startCreateSiteService(previousState: SiteCreationServiceState? = null) {
        if (networkUtils.isNetworkAvailable()) {
            siteCreationState.apply {
                val serviceData = SiteCreationServiceData(
                        segmentId,
                        urlWithoutScheme
                )
                _startCreateSiteService.value = SitePreviewStartServiceData(serviceData, previousState)
            }
        } else {
            showFullscreenErrorWithDelay()
        }
    }

    fun retry() {
        showFullscreenProgress()
        startCreateSiteService(serviceStateForRetry)
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    fun onCancelWizardClicked() {
        _onCancelWizardClicked.value = createSiteState
    }

    fun onOkButtonClicked() {
        tracker.trackPreviewOkButtonTapped()
        _onOkButtonClicked.value = createSiteState
    }

    private fun showFullscreenErrorWithDelay() {
        showFullscreenProgress()
        launch(mainDispatcher) {
            // We show the loading indicator for a bit so the user has some feedback when they press retry
            delay(CONNECTION_ERROR_DELAY_TO_SHOW_LOADING_STATE)
            tracker.trackErrorShown(ERROR_CONTEXT, INTERNET_UNAVAILABLE_ERROR)
            updateUiState(SitePreviewConnectionErrorUiState)
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
                serviceStateForRetry = event.payload as SiteCreationServiceState
                tracker.trackErrorShown(
                        ERROR_CONTEXT,
                        UNKNOWN,
                        "SiteCreation service failed"
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
                SiteNotInLocalDb(remoteSiteId)
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
        val urlToLoad = urlUtils.addUrlSchemeIfNeeded(
                url = urlWithoutScheme,
                addHttps = isWordPressComSubDomain(urlWithoutScheme)
        )
        AppLog.v(T.SITE_CREATION, "Site preview will load for url: $urlToLoad")
        _preloadPreview.postValue(urlToLoad)
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

    fun onWebViewError() {
        if (uiState.value !is SitePreviewWebErrorUiState) {
            updateUiState(SitePreviewWebErrorUiState(createSitePreviewData()))
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

    private fun showFullscreenProgress() {
        loadingAnimationJob?.cancel()
        loadingAnimationJob = launch(mainDispatcher) {
            var i = 0
            val listSize = loadingTexts.size
            while (isActive) {
                updateUiState(
                        SitePreviewFullscreenProgressUiState(
                                animate = i != 0, // the first text should appear without an animation
                                loadingTextResId = loadingTexts[i++ % listSize]
                        )
                )
                delay(LOADING_STATE_TEXT_ANIMATION_DELAY)
            }
        }
    }

    private fun updateUiState(uiState: SitePreviewUiState) {
        if (uiState !is SitePreviewFullscreenProgressUiState) {
            loadingAnimationJob?.cancel()
        }
        _uiState.value = uiState
    }

    private fun updateUiStateAsync(uiState: SitePreviewUiState) {
        if (uiState !is SitePreviewFullscreenProgressUiState) {
            loadingAnimationJob?.cancel()
        }
        _uiState.postValue(uiState)
    }

    sealed class SitePreviewUiState(
        val fullscreenProgressLayoutVisibility: Boolean = false,
        val contentLayoutVisibility: Boolean = false,
        val webViewVisibility: Boolean = false,
        val webViewErrorVisibility: Boolean = false,
        val shimmerVisibility: Boolean = false,
        val fullscreenErrorLayoutVisibility: Boolean = false
    ) {
        data class SitePreviewContentUiState(val data: SitePreviewData) : SitePreviewUiState(
                contentLayoutVisibility = true,
                webViewVisibility = true,
                webViewErrorVisibility = false
        )

        data class SitePreviewWebErrorUiState(val data: SitePreviewData) : SitePreviewUiState(
                contentLayoutVisibility = true,
                webViewVisibility = false,
                webViewErrorVisibility = true
        )

        data class SitePreviewLoadingShimmerState(val data: SitePreviewData) : SitePreviewUiState(
                contentLayoutVisibility = true,
                shimmerVisibility = true
        )

        data class SitePreviewFullscreenProgressUiState(val loadingTextResId: UiString, val animate: Boolean) :
                SitePreviewUiState(fullscreenProgressLayoutVisibility = true)

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
        val serviceData: SiteCreationServiceData,
        val previousState: SiteCreationServiceState?
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
