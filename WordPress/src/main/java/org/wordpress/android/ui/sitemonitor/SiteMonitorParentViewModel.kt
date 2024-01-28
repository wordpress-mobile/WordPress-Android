package org.wordpress.android.ui.sitemonitor

import android.text.TextUtils
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

    private val _uiStates = MutableStateFlow<Map<SiteMonitorType, SiteMonitorUiState>>(emptyMap())
    val uiStates: StateFlow<Map<SiteMonitorType, SiteMonitorUiState>> = _uiStates


    fun start(site: SiteModel) {
        this.site = site
        siteMonitorUtils.trackActivityLaunched()

        loadViews()
    }

    private fun loadViews(siteMonitorType: SiteMonitorType? = null) {
        if (siteMonitorType != null) {
            loadIndividualView(siteMonitorType)
            return
        }

        SiteMonitorType.entries.forEach { type -> loadIndividualView(type) }
    }

    private fun loadIndividualView(siteMonitorType: SiteMonitorType) {
            postUiState(siteMonitorType, SiteMonitorUiState.Preparing)

            if (!checkForInternetConnectivityAndPostErrorIfNeeded(siteMonitorType)) return

            if (!validateAndPostErrorIfNeeded(siteMonitorType)) return

            assembleAndShowSiteMonitor(siteMonitorType)
    }

    private fun assembleAndShowSiteMonitor(type: SiteMonitorType) {
        val sanitizedUrl = siteMonitorUtils.sanitizeSiteUrl(site.url)
        val url = when (type) {
            SiteMonitorType.METRICS -> metricUrlTemplate
            SiteMonitorType.PHP_LOGS -> phpLogsUrlTemplate
            SiteMonitorType.WEB_SERVER_LOGS -> webServerLogsUrlTemplate
        }.replace("{blog}", sanitizedUrl)

        val addressToLoad = prepareAddressToLoad(url)
        val uiState = mapper.toPrepared(url, addressToLoad, type)
        postUiState(type, uiState)
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
    private fun checkForInternetConnectivityAndPostErrorIfNeeded(type: SiteMonitorType) : Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return true
        postUiState(type, mapper.toNoNetworkError(this@SiteMonitorParentViewModel::loadViews))
        return false
    }

    private fun validateAndPostErrorIfNeeded(type: SiteMonitorType): Boolean {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postUiState(type, mapper.toGenericError(this@SiteMonitorParentViewModel::loadViews))
            return false
        }
        return true
    }

    private fun postUiState(type: SiteMonitorType, uiState: SiteMonitorUiState) {
        launch {
            _uiStates.value = _uiStates.value.toMutableMap().apply {
                this[type] = uiState
            }
        }
    }

    fun onUrlLoaded(url: String) {
        val type = siteMonitorUtils.urlToType(url)
        postUiState(type, SiteMonitorUiState.Loaded)
    }

    fun onWebViewError(url: String) {
        val type = siteMonitorUtils.urlToType(url)
        postUiState(type, mapper.toGenericError(this@SiteMonitorParentViewModel::loadViews))
    }

    fun onTabSelected(siteMonitorType: SiteMonitorType?) {
        loadViews(siteMonitorType)
    }

    companion object {
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"
    }
}
