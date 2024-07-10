package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import android.text.TextUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Named

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteStore: SiteStore,
    private val mapper: CampaignDetailMapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private lateinit var pageSource: CampaignDetailPageSource
    private var campaignId: String = ""

    private val _actionEvents = Channel<BlazeActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _uiState = MutableStateFlow<CampaignDetailUiState>(CampaignDetailUiState.Preparing)
    val uiState = _uiState as StateFlow<CampaignDetailUiState>

    fun start(campaignId: String, campaignDetailPageSource: CampaignDetailPageSource) {
        this.campaignId = campaignId
        this.pageSource = campaignDetailPageSource

        blazeFeatureUtils.trackCampaignDetailsOpened(campaignDetailPageSource)

        loadCampaignDetails()
    }

    private fun loadCampaignDetails() {
        postUiState(CampaignDetailUiState.Preparing)

        if (!checkForInternetConnectivityAndPostErrorIfNeeded()) return

        if (!validateAndPostErrorIfNeeded()) return

        assembleAndShowCampaignDetail()
    }

    private fun validateAndPostErrorIfNeeded(): Boolean {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postUiState(mapper.toGenericError(this@CampaignDetailViewModel::loadCampaignDetails))
            return false
        }
        return true
    }

    private fun checkForInternetConnectivityAndPostErrorIfNeeded(): Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return true
        postUiState(mapper.toNoNetworkError(this@CampaignDetailViewModel::loadCampaignDetails))
        return false
    }

    private fun assembleAndShowCampaignDetail() {
        val url = createURL(
            pathComponents = arrayOf(
                ADVERTISING_PATH,
                CAMPAIGNS_PATH,
                campaignId,
                extractAndSanitizeSiteUrl()
            ),
            source = pageSource.trackingName
        )
        val addressToLoad = prepareAddressToLoad(url)
        postUiState(mapper.toPrepared(url, addressToLoad))
    }
    private fun createURL(vararg pathComponents: String, source: String): String {
        val basePath = pathComponents.joinToString("/")
        return "$WP_HOST/$basePath".withQueryParam("source", source)
    }

    private fun extractAndSanitizeSiteUrl(): String {
        return selectedSiteRepository.getSelectedSite()?.url?.replace(Regex(HTTP_PATTERN), "") ?: ""
    }
    private fun prepareAddressToLoad(url: String): String {
        val username = accountStore.account.userName
        val accessToken = accountStore.accessToken

        var addressToLoad = url

        // Custom domains are not properly authenticated due to a server side(?) issue, so this gets around that
        if (!addressToLoad.contains(WPCOM_DOMAIN)) {
            val wpComSites: List<SiteModel> = siteStore.wPComSites
            for (siteModel in wpComSites) {
                // Only replace the url if we know the unmapped url and if it's a custom domain
                if (!TextUtils.isEmpty(siteModel.unmappedUrl)
                    && !siteModel.url.contains(WPCOM_DOMAIN)
                ) {
                    addressToLoad = addressToLoad.replace(siteModel.url, siteModel.unmappedUrl)
                }
            }
        }
        return blazeFeatureUtils.getAuthenticationPostData(
            WPCOM_LOGIN_URL,
            addressToLoad,
            username,
            "",
            accessToken?:""
        )
    }

    private fun postUiState(state: CampaignDetailUiState) {
        launch {
            _uiState.value = state
        }
    }

    private fun postActionEvent(actionEvent: BlazeActionEvent) {
        launch {
            _actionEvents.send(actionEvent)
        }
    }

    fun onRedirectToExternalBrowser(url: String) {
        postActionEvent(BlazeActionEvent.LaunchExternalBrowser(url))
    }

    fun onUrlLoaded() {
        postUiState(CampaignDetailUiState.Loaded)
    }

    fun onWebViewError() {
        postUiState(mapper.toGenericError(this@CampaignDetailViewModel::loadCampaignDetails))
    }

    private fun String.withQueryParam(key: String, value: String) = "$this?$key=$value"
    companion object {
        const val HTTP_PATTERN = "(https?://)"
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"

        const val WP_HOST = "https://wordpress.com"
        const val ADVERTISING_PATH = "advertising"
        const val CAMPAIGNS_PATH = "campaigns"
    }
}

enum class CampaignDetailPageSource(val trackingName: String) {
    DASHBOARD_CARD("dashboard_card"),
    CAMPAIGN_LISTING_PAGE("campaign_listing_page"),
    UNKNOWN("unknown")
}
