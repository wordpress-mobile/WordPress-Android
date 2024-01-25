package org.wordpress.android.ui.sitemonitor

import android.text.TextUtils
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteMonitorParentViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val mapper: SiteMonitorMapper,
    private val siteMonitorUtils: SiteMonitorUtils,
    private val siteStore: SiteStore,
) : ScopedViewModel(bgDispatcher) {
    private lateinit var site: SiteModel

    private val metricUrlTemplate = "https://wordpress.com/site-monitoring/{blog}"
    private val phpLogsUrlTemplate = "https://wordpress.com/site-monitoring/{blog}/php"
    private val webServerLogsUrlTemplate = "https://wordpress.com/site-monitoring/{blog}/web"

    private val _uiState = MutableStateFlow<SiteMonitorUiState>(SiteMonitorUiState.Preparing)
    val uiState = _uiState as StateFlow<SiteMonitorUiState>

    fun start(site: SiteModel) {
        this.site = site
        siteMonitorUtils.trackActivityLaunched()

        loadViews()
    }

    private fun loadViews() {
        Log.i(javaClass.simpleName, "***=> loadViews")
        postUiState(SiteMonitorUiState.Preparing)

        if (!checkForInternetConnectivityAndPostErrorIfNeeded()) return

        if (!validateAndPostErrorIfNeeded()) return

        assembleAndShowSiteMonitor()
    }

    private fun assembleAndShowSiteMonitor() {
        val sanitizedUrl = siteMonitorUtils.sanitizeSiteUrl(site.url)

        val siteMonitorUrls = mutableListOf<SiteMonitorUrl>().apply {
            add(
                createSiteMonitorUrl(
                    metricUrlTemplate.replace("{blog}", sanitizedUrl), SiteMonitorUrl.SiteMonitorType.METRICS)
            )
            add(
                createSiteMonitorUrl(
                    phpLogsUrlTemplate.replace("{blog}", sanitizedUrl),
                    SiteMonitorUrl.SiteMonitorType.PHP_LOGS
                )
            )
            add(
                createSiteMonitorUrl(
                    webServerLogsUrlTemplate.replace("{blog}", sanitizedUrl),
                    SiteMonitorUrl.SiteMonitorType.WEB_SERVER_LOGS
                )
            )
        }
        postUiState(mapper.toPrepared(siteMonitorUrls))
    }

    private fun createSiteMonitorUrl(url: String, type: SiteMonitorUrl.SiteMonitorType): SiteMonitorUrl {
        return SiteMonitorUrl(
            type = type,
            url = url,
            addressToLoad = prepareAddressToLoad(url)
        )
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
        return siteMonitorUtils.getAuthenticationPostData(
            WPCOM_LOGIN_URL,
            addressToLoad,
            username,
            "",
            accessToken?:""
        )
    }
    private fun checkForInternetConnectivityAndPostErrorIfNeeded(): Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return true
        postUiState(mapper.toNoNetworkError(this@SiteMonitorParentViewModel::loadViews))
        return false
    }

    private fun validateAndPostErrorIfNeeded(): Boolean {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postUiState(mapper.toGenericError(this@SiteMonitorParentViewModel::loadViews))
            return false
        }
        return true
    }

    private fun postUiState(uiState: SiteMonitorUiState) {
        launch {
            _uiState.value = uiState
        }
    }

    fun onUrlLoaded() {
        postUiState(SiteMonitorUiState.Loaded)
    }

    fun onWebViewError() {
        postUiState(mapper.toGenericError(this@SiteMonitorParentViewModel::loadViews))
    }

    companion object {
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"
    }
}
