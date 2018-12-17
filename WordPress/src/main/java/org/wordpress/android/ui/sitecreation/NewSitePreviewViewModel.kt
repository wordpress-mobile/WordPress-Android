package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

class NewSitePreviewViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

    private lateinit var siteCreationState: SiteCreationState

    private val slug = "en.blog" // TODO remove and use slug from siteCreationState

    private val _uiState: MutableLiveData<SitePreviewUiState> = MutableLiveData()
    val uiState: LiveData<SitePreviewUiState> = _uiState

    private val _preloadPreview: MutableLiveData<String> = SingleLiveEvent()
    val preloadPreview: LiveData<String> = _preloadPreview

    fun start(siteCreationState: SiteCreationState) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.siteCreationState = siteCreationState

        updateUiState(SitePreviewFullscreenProgressUiState)
        _preloadPreview.value = getFullUrlFromSlug(slug)
    }

    fun onUrlLoaded() {
        val urlShort = getShortUrlFromSlug(slug)
        val fullUrl = getFullUrlFromSlug(slug)
        val subDomainIndices: Pair<Int, Int> = Pair(0, slug.length)
        val domainIndices: Pair<Int, Int> = Pair(Math.min(subDomainIndices.second, urlShort.length), urlShort.length)

        updateUiState(SitePreviewContentUiState(SitePreviewData(fullUrl, urlShort, subDomainIndices, domainIndices)))
    }

    private fun getShortUrlFromSlug(slug: String) = "$slug.wordpress.com"
    private fun getFullUrlFromSlug(slug: String) = "https://${getShortUrlFromSlug(slug)}"

    private fun updateUiState(uiState: SitePreviewUiState) {
        _uiState.value = uiState
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
        )

        sealed class SitePreviewFullscreenErrorUiState constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null
        ) : SitePreviewUiState(
                fullscreenProgressLayoutVisibility = false,
                contentLayoutVisibility = false,
                fullscreenErrorLayoutVisibility = true
        ) {
            object SitePreviewGenericErrorUiState : SitePreviewFullscreenErrorUiState(
                    R.string.site_creation_error_generic_title,
                    R.string.site_creation_error_generic_subtitle
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
}
