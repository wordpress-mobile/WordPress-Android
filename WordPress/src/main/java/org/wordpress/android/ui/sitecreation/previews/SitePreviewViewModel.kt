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
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Created
import org.wordpress.android.ui.sitecreation.SiteCreationResult.CreatedButNotFetched
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SiteNotCreatedErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SiteNotFoundInDbUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.UrlData
import org.wordpress.android.ui.sitecreation.services.FetchWpComSiteUseCase
import org.wordpress.android.ui.sitecreation.usecases.isWordPressComSubDomain
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.StringUtils
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

    private var siteDesign: String? = null
    private var isFree: Boolean = true

    private var result: Created? = null

    private val _uiState: MutableLiveData<SitePreviewUiState> = MutableLiveData()
    val uiState: LiveData<SitePreviewUiState> = _uiState

    private val _preloadPreview: MutableLiveData<String> = MutableLiveData()
    val preloadPreview: LiveData<String> = _preloadPreview

    private val _onOkButtonClicked = SingleLiveEvent<Created?>()
    val onOkButtonClicked: LiveData<Created?> = _onOkButtonClicked

    fun start(siteCreationState: SiteCreationState) {
        if (isStarted) return else isStarted = true
        if (siteCreationState.result !is Created) {
            updateUiState(SiteNotCreatedErrorUiState)
            return
        }
        siteDesign = siteCreationState.siteDesign
        result = siteCreationState.result
        isFree = requireNotNull(siteCreationState.domain).isFree
        startPreLoadingWebView()
        result?.let {
            if (it is CreatedButNotFetched) {
                launch {
                    fetchNewlyCreatedSiteModel(it.site.siteId)?.let {
                        result = Completed(it)
                    }
                }
            }
        }
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
                    updateUiState(SitePreviewLoadingShimmerState(isFree, createSitePreviewData()))
                }
            }
        }
        // Load the newly created site in the webview
        result?.site?.url?.let { url ->
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
    private suspend fun fetchNewlyCreatedSiteModel(remoteSiteId: Long): SiteModel? {
        val onSiteFetched = fetchWpComSiteUseCase.fetchSiteWithRetry(remoteSiteId)
        return if (!onSiteFetched.isError) {
            val site = siteStore.getSiteBySiteId(remoteSiteId)
            if (site == null) {
                withContext(mainDispatcher) {
                    updateUiState(SiteNotFoundInDbUiState)
                }
            }
            site
        } else {
            null
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
            updateUiState(SitePreviewContentUiState(isFree, createSitePreviewData()))
        }
    }

    fun onWebViewError() {
        if (uiState.value !is SitePreviewWebErrorUiState) {
            updateUiState(SitePreviewWebErrorUiState(isFree, createSitePreviewData()))
        }
    }

    private fun getCleanUrl(url: String) = StringUtils.removeTrailingSlash(urlUtils.removeScheme(url))

    private fun createSitePreviewData(): UrlData {
        val url = result?.let { getCleanUrl(it.site.url) ?: "" } ?: ""
        val subDomain = urlUtils.extractSubDomain(url)
        val fullUrl = urlUtils.addUrlSchemeIfNeeded(url, true)
        val subDomainIndices = 0 to subDomain.length
        val domainIndices = subDomainIndices.second.coerceAtMost(url.length) to url.length
        return UrlData(
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
        open val urlData: UrlData,
        val webViewVisibility: Boolean = false,
        val webViewErrorVisibility: Boolean = false,
        val shimmerVisibility: Boolean = false,
        val subtitle: UiString,
        val caption: UiString?,
        val errorTitle: UiString? = null,
    ) {
        data class SitePreviewContentUiState(
            val isFree: Boolean,
            override val urlData: UrlData,
        ) : SitePreviewUiState(
            urlData = urlData,
            webViewVisibility = true,
            webViewErrorVisibility = false,
            subtitle = getSubtitle(isFree),
            caption = getCaption(isFree),
        )

        data class SitePreviewWebErrorUiState(
            val isFree: Boolean,
            override val urlData: UrlData,
        ) : SitePreviewUiState(
            urlData = urlData,
            webViewVisibility = false,
            webViewErrorVisibility = true,
            subtitle = getSubtitle(isFree),
            caption = getCaption(isFree),
        )

        data object SiteNotCreatedErrorUiState : SitePreviewUiState(
            urlData = UrlData("", "", 0 to 0, 0 to 0),
            webViewVisibility = false,
            webViewErrorVisibility = true,
            subtitle = UiStringRes(R.string.site_creation_error_generic_title),
            caption = UiStringRes(R.string.site_creation_error_generic_subtitle),
            errorTitle = UiStringRes(R.string.error),
        )

        data object SiteNotFoundInDbUiState : SitePreviewUiState(
            urlData = UrlData("", "", 0 to 0, 0 to 0),
            webViewVisibility = false,
            webViewErrorVisibility = true,
            subtitle = UiStringRes(R.string.site_creation_error_generic_title),
            caption = UiStringRes(R.string.site_creation_error_generic_subtitle),
        )

        data class SitePreviewLoadingShimmerState(
            val isFree: Boolean,
            override val urlData: UrlData,
        ) : SitePreviewUiState(
            urlData = urlData,
            shimmerVisibility = true,
            subtitle = getSubtitle(isFree),
            caption = getCaption(isFree),
        )

        companion object {
            private fun getSubtitle(isFree: Boolean): UiString {
                return if (isFree) {
                    UiStringRes(R.string.new_site_creation_preview_subtitle)
                } else {
                    UiStringResWithParams(
                        R.string.new_site_creation_preview_subtitle_paid,
                        UiStringRes(R.string.new_site_creation_preview_subtitle),
                    )
                }
            }

            private fun getCaption(isFree: Boolean): UiStringRes? {
                return UiStringRes(R.string.new_site_creation_preview_caption_paid).takeIf { !isFree }
            }
        }

        data class UrlData(
            val fullUrl: String,
            val shortUrl: String,
            val domainIndices: Pair<Int, Int>,
            val subDomainIndices: Pair<Int, Int>
        )
    }
}
