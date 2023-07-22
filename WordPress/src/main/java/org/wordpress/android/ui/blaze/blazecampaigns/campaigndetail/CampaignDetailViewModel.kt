package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteStore: SiteStore
) : ViewModel() {
    private lateinit var pageSource: CampaignDetailPageSource
    private var campaignId: Int = 0

    private val _actionEvents = Channel<BlazeActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _model = MutableStateFlow(CampaignDetailUIModel())
    val model: StateFlow<CampaignDetailUIModel> = _model
    fun start(campaignId: Int, campaignDetailPageSource: CampaignDetailPageSource) {
        this.campaignId = campaignId
        this.pageSource = campaignDetailPageSource

        blazeFeatureUtils.trackCampaignDetailsOpened(campaignDetailPageSource)

        validateAndFinishIfNeeded()
        assembleAndPostUiState()
    }
    private fun validateAndFinishIfNeeded() {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postActionEvent(BlazeActionEvent.FinishActivityWithMessage(R.string.blaze_campaign_detail_error))
        }
    }
    private fun assembleAndPostUiState() {
        val url = createURL(
            pathComponents = arrayOf(
                ADVERTISING_PATH,
                extractAndSanitizeSiteUrl(),
                CAMPAIGNS_PATH,
                campaignId.toString()
            ),
            source = pageSource.trackingName
        )
        val addressToLoad = prepareAddressToLoad(url)
        postScreenState(
            model.value.copy(
                addressToLoad = addressToLoad,
                url = url,
                userAgent = blazeFeatureUtils.getUserAgent()
            )
        )
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

    private fun postScreenState(state: CampaignDetailUIModel) {
        viewModelScope.launch {
            _model.value = state
        }
    }
    private fun postActionEvent(actionEvent: BlazeActionEvent) {
        viewModelScope.launch {
            _actionEvents.send(actionEvent)
        }
    }

    fun onRedirectToExternalBrowser(url: String) {
        postActionEvent(BlazeActionEvent.LaunchExternalBrowser(url))
    }

    fun onUrlLoaded() {
        // no op
    }

    fun onWebViewError() {
        postActionEvent(BlazeActionEvent.FinishActivityWithMessage(R.string.blaze_campaign_detail_error))
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

data class CampaignDetailUIModel(
    val enableJavascript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val userAgent: String = "",
    val enableChromeClient: Boolean = true,
    val url: String = "",
    val addressToLoad: String = ""
)
