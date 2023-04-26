package org.wordpress.android.ui.sitecreation.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Created
import org.wordpress.android.ui.sitecreation.SiteCreationResult.CreatedButNotFetched.InCart
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType.UNKNOWN
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error.CartError
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error.ConnectionError
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Error.GenericError
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel.SiteProgressUiState.Loading
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.IDLE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

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
class SiteCreationProgressViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val tracker: SiteCreationTracker,
    private val createCartUseCase: CreateCartUseCase,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {
    private var loadingAnimationJob: Job? = null

    private lateinit var siteCreationState: SiteCreationState
    private lateinit var domain: DomainModel
    private lateinit var site: SiteModel

    private var lastReceivedServiceState: SiteCreationServiceState? = null
    private var serviceStateForRetry: SiteCreationServiceState? = null

    private val _uiState: MutableLiveData<SiteProgressUiState> = MutableLiveData()
    val uiState: LiveData<SiteProgressUiState> = _uiState

    private val _startCreateSiteService: SingleLiveEvent<StartServiceData> = SingleLiveEvent()
    val startCreateSiteService: LiveData<StartServiceData> = _startCreateSiteService

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    private val _onCancelWizardClicked = SingleLiveEvent<Unit>()
    val onCancelWizardClicked: LiveData<Unit> = _onCancelWizardClicked

    private val _onFreeSiteCreated = SingleLiveEvent<SiteModel>()
    val onFreeSiteCreated: LiveData<SiteModel> = _onFreeSiteCreated

    private val _onCartCreated = SingleLiveEvent<CheckoutDetails>()
    val onCartCreated: LiveData<CheckoutDetails> = _onCartCreated

    override fun onCleared() {
        super.onCleared()
        loadingAnimationJob?.cancel()
        createCartUseCase.clear()
    }

    fun start(siteCreationState: SiteCreationState) {
        if (siteCreationState.result is Created) {
            check(siteCreationState.result !is Completed) { "Unexpected state on progress screen." }
            // reuse the previously blog when returning with the same domain
            if (siteCreationState.domain == domain) {
                site = siteCreationState.result.site
                if (siteCreationState.result is InCart) {
                    createCart()
                }
                return
            }
        }
        this.siteCreationState = siteCreationState
        domain = requireNotNull(siteCreationState.domain) { "domain required to create a site" }

        runLoadingAnimationUi()
        startCreateSiteService()
    }

    private fun startCreateSiteService(previousState: SiteCreationServiceState? = null) {
        if (networkUtils.isNetworkAvailable()) {
            siteCreationState.let { state ->
                // A non-null [segmentId] may invalidate the [siteDesign] selection
                // https://github.com/wordpress-mobile/WordPress-Android/issues/13749
                val segmentIdentifier = state.segmentId.takeIf { state.siteDesign != null }
                val serviceData = SiteCreationServiceData(
                    segmentIdentifier,
                    state.siteDesign,
                    domain.domainName,
                    state.siteName,
                    domain.isFree,
                )
                _startCreateSiteService.value = StartServiceData(serviceData, previousState)
            }
        } else {
            showFullscreenErrorWithDelay()
        }
    }

    fun retry() {
        when (uiState.value) {
            is GenericError,
            is ConnectionError -> startCreateSiteService(serviceStateForRetry)

            is CartError -> createCart()
            else -> error("Unexpected state for retry: ${uiState.value}")
        }
        runLoadingAnimationUi()
    }

    fun onHelpClicked() = _onHelpClicked.call()

    fun onCancelWizardClicked() = _onCancelWizardClicked.call()

    private fun showFullscreenErrorWithDelay() {
        runLoadingAnimationUi()
        launch {
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
                site = mapPayloadToSiteModel(event.payload)
                _onFreeSiteCreated.postValue(site) // MainVM will navigate forward if the domain is free
                if (!domain.isFree) {
                    createCart()
                }
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

    private fun mapPayloadToSiteModel(payload: Any?): SiteModel {
        require(payload is Pair<*, *>) { "Expected Pair in Payload, got: $payload" }
        val (blogId, blogUrl) = payload
        require(blogId is Long) { "Expected the 1st element in the Payload Pair to be a Long, got: $blogId" }
        require(blogUrl is String) { "Expected the 2nd element in the Payload Pair to be a Long, got: $blogUrl" }
        return SiteModel().apply { siteId = blogId; url = blogUrl }
    }

    private fun createCart() = launch {
        AppLog.d(T.SITE_CREATION, "Creating cart: $domain")

        val event = createCartUseCase.execute(
            site,
            domain.productId,
            domain.domainName,
            domain.supportsPrivacy,
            false,
        )

        if (event.isError) {
            AppLog.e(T.SITE_CREATION, "Failed cart creation: ${event.error.message}")
            updateUiStateAsync(CartError)
        } else {
            AppLog.d(T.SITE_CREATION, "Successful cart creation: ${event.cartDetails}")
            _onCartCreated.postValue(CheckoutDetails(site, domain.domainName))
        }
    }

    private fun runLoadingAnimationUi() {
        loadingAnimationJob?.cancel()
        loadingAnimationJob = launch {
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

            object CartError : Error(
                R.string.site_creation_error_generic_title,
                R.string.site_creation_error_cart_subtitle,
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
