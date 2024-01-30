package org.wordpress.android.ui.sitemonitor

import android.text.TextUtils
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
class SiteMonitorTabViewModelSlice @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val mapper: SiteMonitorMapper,
    private val siteMonitorUtils: SiteMonitorUtils,
    private val siteStore: SiteStore,
){
    private lateinit var scope: CoroutineScope

    private lateinit var site: SiteModel
    private lateinit var siteMonitorType: SiteMonitorType
    private lateinit var urlTemplate: String

    private val _uiState = mutableStateOf<SiteMonitorUiState>(SiteMonitorUiState.Preparing)
    val uiState = _uiState

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun start(type: SiteMonitorType, urlTemplate: String, site: SiteModel) {
        Log.i("Track", "TheViewModel start with $urlTemplate and $type")
        this.siteMonitorType = type
        this.urlTemplate = urlTemplate
        this.site = site

        loadView()
    }

    private fun loadView() {
        postUiState(SiteMonitorUiState.Preparing)

        if (!checkForInternetConnectivityAndPostErrorIfNeeded()) return

        if (!validateAndPostErrorIfNeeded()) return

        assembleAndShowSiteMonitor()
    }

    private fun checkForInternetConnectivityAndPostErrorIfNeeded() : Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return true
        postUiState(mapper.toNoNetworkError(::loadView))
        return false
    }

    private fun validateAndPostErrorIfNeeded(): Boolean {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postUiState(mapper.toGenericError(this::loadView))
            return false
        }
        return true
    }

    private fun assembleAndShowSiteMonitor() {
        val sanitizedUrl = siteMonitorUtils.sanitizeSiteUrl(site.url)
        val url = urlTemplate.replace("{blog}", sanitizedUrl)

        val addressToLoad = prepareAddressToLoad(url)
        postUiState(mapper.toPrepared(url, addressToLoad, siteMonitorType))
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

    private fun postUiState(state: SiteMonitorUiState) {
        scope.launch {
            _uiState.value = state
        }
    }

    fun onUrlLoaded(url: String) {
        Log.i("Track", "TheViewModel onUrlLoaded $url")
        siteMonitorUtils.trackTabLoaded(siteMonitorType)
        if (uiState.value is SiteMonitorUiState.Prepared){
            postUiState(SiteMonitorUiState
                .Loaded((_uiState.value as SiteMonitorUiState.Prepared).model.copy(url = url)))
        }
    }


    fun onWebViewError() {
        postUiState(mapper.toGenericError(::loadView))
    }

    fun onCleared() {
        Log.i("Track", "TheViewModel onCleared")
        scope.cancel()
    }

    companion object {
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"
    }
}
