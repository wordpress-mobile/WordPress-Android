package org.wordpress.android.ui.sitecreation.previews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.SiteCreationResult
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotInLocalDb
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.ui.sitecreation.usecases.isWordPressComSubDomain
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class SitePreviewViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val fetchWpComSiteUseCase: FetchWpComSiteUseCase,
    private val urlUtils: UrlUtilsWrapper,
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    init {
        dispatcher.register(fetchWpComSiteUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchWpComSiteUseCase)
        job.cancel()
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false
    private var webviewFullyLoadedTracked = false

    private var siteTitle: String? = null
    private var siteDesign: String? = null
    private var remoteSiteId: Long = 0
    private var urlWithoutScheme: String? = null

    private lateinit var result: SiteCreationResult

    private val _uiState: MutableLiveData<SitePreviewUiState> = MutableLiveData()
    val uiState: LiveData<SitePreviewUiState> = _uiState

    private val _preloadPreview: MutableLiveData<String> = MutableLiveData()
    val preloadPreview: LiveData<String> = _preloadPreview

    private val _onOkButtonClicked = SingleLiveEvent<SiteCreationResult>()
    val onOkButtonClicked: LiveData<SiteCreationResult> = _onOkButtonClicked

    fun start(siteCreationState: SiteCreationState) {
        if (isStarted) return
        isStarted = true
        siteDesign = siteCreationState.siteDesign
        urlWithoutScheme = requireNotNull(siteCreationState.domain) { "domain required for preview" }.domainName
        siteTitle = siteCreationState.siteName
        remoteSiteId = requireNotNull(siteCreationState.remoteSiteId) { "remoteSiteId required for preview" }
        launch {
            result = fetchNewlyCreatedSiteModel(remoteSiteId)
        }
        startPreLoadingWebView()
    }

    fun onOkButtonClicked() {
        tracker.trackPreviewOkButtonTapped()
        _onOkButtonClicked.postValue(result)
    }

    private fun startPreLoadingWebView() {
        tracker.trackPreviewLoading(siteDesign)
        launch {
            /**
             * If the webview is still not loaded after some delay, we'll show the loading shimmer animation instead
             * of the full screen progress, so the user is not blocked for taking actions.
             */
            withContext(mainDispatcher) {
                if (uiState.value !is SitePreviewContentUiState) {
                    tracker.trackPreviewWebviewShown(siteDesign)
                    updateUiState(SitePreviewLoadingShimmerState(createSitePreviewData()))
                }
            }
        }
        // Load the newly created site in the webview
        urlWithoutScheme?.let { url ->
            val urlToLoad = urlUtils.addUrlSchemeIfNeeded(
                url = url,
                addHttps = isWordPressComSubDomain(url)
            )
            AppLog.v(T.SITE_CREATION, "Site preview will load for url: $urlToLoad")
            _preloadPreview.postValue(urlToLoad)
        }
    }

    /**
     * Fetch newly created site model - supports retry with linear backoff.
     */
    private suspend fun fetchNewlyCreatedSiteModel(remoteSiteId: Long): SiteCreationResult {
        val onSiteFetched = fetchWpComSiteUseCase.fetchSiteWithRetry(remoteSiteId)
        val isSiteTitleTaskComplete = !siteTitle.isNullOrBlank()
        return if (!onSiteFetched.isError) {
            val siteBySiteId = requireNotNull(siteStore.getSiteBySiteId(remoteSiteId)) {
                "Site successfully fetched but has not been found in the local db."
            }
            Completed(siteBySiteId.id, isSiteTitleTaskComplete, siteBySiteId.url)
        } else {
            NotInLocalDb(remoteSiteId, isSiteTitleTaskComplete)
        }
    }

    fun onUrlLoaded() {
        if (!webviewFullyLoadedTracked) {
            webviewFullyLoadedTracked = true
            tracker.trackPreviewWebviewFullyLoaded(siteDesign)
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
        val url = urlWithoutScheme ?: ""
        val subDomain = urlUtils.extractSubDomain(url)
        val fullUrl = urlUtils.addUrlSchemeIfNeeded(url, true)
        val subDomainIndices = 0 to subDomain.length
        val domainIndices = subDomainIndices.second.coerceAtMost(url.length) to url.length
        return SitePreviewData(
            fullUrl,
            url,
            subDomainIndices,
            domainIndices
        )
    }

    private fun updateUiState(uiState: SitePreviewUiState) {
        _uiState.value = uiState
    }

    sealed class SitePreviewUiState(
        val webViewVisibility: Boolean = false,
        val webViewErrorVisibility: Boolean = false,
        val shimmerVisibility: Boolean = false,
    ) {
        data class SitePreviewContentUiState(val data: SitePreviewData) : SitePreviewUiState(
            webViewVisibility = true,
            webViewErrorVisibility = false
        )

        data class SitePreviewWebErrorUiState(val data: SitePreviewData) : SitePreviewUiState(
            webViewVisibility = false,
            webViewErrorVisibility = true
        )

        data class SitePreviewLoadingShimmerState(val data: SitePreviewData) : SitePreviewUiState(
            shimmerVisibility = true
        )
    }

    data class SitePreviewData(
        val fullUrl: String,
        val shortUrl: String,
        val domainIndices: Pair<Int, Int>,
        val subDomainIndices: Pair<Int, Int>
    )
}
