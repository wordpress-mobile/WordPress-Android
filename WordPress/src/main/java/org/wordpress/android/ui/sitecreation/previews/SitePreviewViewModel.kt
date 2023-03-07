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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.CreateSiteState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.usecases.isWordPressComSubDomain
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

const val KEY_CREATE_SITE_STATE = "CREATE_SITE_STATE"
const val LOADING_STATE_TEXT_ANIMATION_DELAY = 2000L

@HiltViewModel
class SitePreviewViewModel @Inject constructor(
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

    private lateinit var siteCreationState: SiteCreationState
    private var urlWithoutScheme: String? = null
    private lateinit var createSiteState: CreateSiteState

    private val _uiState: MutableLiveData<SitePreviewUiState> = MutableLiveData()
    val uiState: LiveData<SitePreviewUiState> = _uiState

    private val _preloadPreview: MutableLiveData<String> = MutableLiveData()
    val preloadPreview: LiveData<String> = _preloadPreview

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    private val _onOkButtonClicked = SingleLiveEvent<CreateSiteState>()
    val onOkButtonClicked: LiveData<CreateSiteState> = _onOkButtonClicked

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun start(siteCreationState: SiteCreationState, createSiteState: CreateSiteState) {
        if (isStarted) return
        isStarted = true
        this.siteCreationState = siteCreationState
        this.urlWithoutScheme = siteCreationState.domain?.domainName
        this.createSiteState = createSiteState
        startPreLoadingWebView()
    }

    fun onHelpClicked() = _onHelpClicked.call()

    fun onOkButtonClicked() {
        tracker.trackPreviewOkButtonTapped()
        _onOkButtonClicked.value = createSiteState
    }

    private fun startPreLoadingWebView() {
        tracker.trackPreviewLoading(siteCreationState.siteDesign)
        launch {
            /**
             * If the webview is still not loaded after some delay, we'll show the loading shimmer animation instead
             * of the full screen progress, so the user is not blocked for taking actions.
             */
            withContext(mainDispatcher) {
                if (uiState.value !is SitePreviewContentUiState) {
                    tracker.trackPreviewWebviewShown(siteCreationState.siteDesign)
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

    fun onUrlLoaded() {
        if (!webviewFullyLoadedTracked) {
            webviewFullyLoadedTracked = true
            tracker.trackPreviewWebviewFullyLoaded(siteCreationState.siteDesign)
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
    }

    data class SitePreviewData(
        val fullUrl: String,
        val shortUrl: String,
        val domainIndices: Pair<Int, Int>,
        val subDomainIndices: Pair<Int, Int>
    )
}
