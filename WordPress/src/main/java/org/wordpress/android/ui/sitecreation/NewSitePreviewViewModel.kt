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
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.CreateSiteState.SiteNotInLocalDb
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewConnectionErrorUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState.SitePreviewGenericErrorUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.creation.FetchWpComSiteUseCase
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

class NewSitePreviewViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val fetchWpComSiteUseCase: FetchWpComSiteUseCase,
    private val networkUtils: NetworkUtilsWrapper,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

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
                        UrlUtils.extractSubDomain(urlWithoutScheme)
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
        _onOkButtonClicked.value = createSiteState
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
            } // do nothing
            SUCCESS -> {
                startPreLoadingWebView()
                val remoteSiteId = event.payload as Long
                createSiteState = SiteNotInLocalDb(remoteSiteId)
                fetchNewlyCreatedSiteModel(remoteSiteId)
            }
            FAILURE -> {
                serviceStateForRetry = event.payload as NewSiteCreationServiceState
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
        _preloadPreview.postValue(UrlUtils.addUrlSchemeIfNeeded(urlWithoutScheme, true))
    }

    fun onUrlLoaded() {
        _hideGetStartedBar.call()
        /**
         * Update the ui state if the loading or error screen is being shown.
         * In other words don't update it after a configuration change.
         */
        if (uiState.value !is SitePreviewContentUiState) {
            val subDomain = UrlUtils.extractSubDomain(urlWithoutScheme)
            val fullUrl = UrlUtils.addUrlSchemeIfNeeded(urlWithoutScheme, true)
            val subDomainIndices: Pair<Int, Int> = Pair(0, subDomain.length)
            val domainIndices: Pair<Int, Int> = Pair(
                    Math.min(subDomainIndices.second, urlWithoutScheme.length),
                    urlWithoutScheme.length
            )

            updateUiState(
                    SitePreviewContentUiState(
                            SitePreviewData(
                                    fullUrl,
                                    urlWithoutScheme,
                                    subDomainIndices,
                                    domainIndices
                            )
                    )
            )
        }
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
            val showContactSupport: Boolean = false,
            val showCancelWizardButton: Boolean = true
        ) : SitePreviewUiState(
                fullscreenProgressLayoutVisibility = false,
                contentLayoutVisibility = false,
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
