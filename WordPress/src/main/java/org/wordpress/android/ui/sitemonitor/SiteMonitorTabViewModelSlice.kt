package org.wordpress.android.ui.sitemonitor

import android.text.TextUtils
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

const val REFRESH_DELAY = 500L

class SiteMonitorTabViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val mapper: SiteMonitorMapper,
    private val siteMonitorUtils: SiteMonitorUtils,
    private val siteStore: SiteStore,
) {
    private lateinit var scope: CoroutineScope

    private lateinit var site: SiteModel
    private lateinit var siteMonitorType: SiteMonitorType
    private lateinit var urlTemplate: String

    private val _uiState = mutableStateOf<SiteMonitorUiState>(SiteMonitorUiState.Preparing)
    val uiState: State<SiteMonitorUiState> = _uiState

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun start(type: SiteMonitorType, urlTemplate: String, site: SiteModel) {
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

    fun refreshData() {
        scope.launch {
            _isRefreshing.value = true
            // this delay is to prevent the refresh from being too fast
            // so that the user can see the refresh animation
            // also this would fix the unit tests
            delay(REFRESH_DELAY)
            loadView()
            _isRefreshing.value = false
        }
    }

    private fun checkForInternetConnectivityAndPostErrorIfNeeded(): Boolean {
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

        scope.launch(bgDispatcher) {
            val addressToLoad = prepareAddressToLoad(url)
            postUiState(mapper.toPrepared(url, addressToLoad, siteMonitorType))
        }
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
            accessToken ?: ""
        )
    }

    private fun postUiState(state: SiteMonitorUiState) {
        scope.launch {
            _uiState.value = state
        }
    }

    fun onUrlLoaded() {
        if (uiState.value is SiteMonitorUiState.Prepared) {
            postUiState(SiteMonitorUiState.Loaded)
        }
    }

    fun onWebViewError() {
        postUiState(mapper.toGenericError(::loadView))
    }

    fun onCleared() {
        scope.cancel()
    }

    companion object {
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"
    }
}
