package org.wordpress.android.ui.sitemonitor

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
class SiteMonitorTabViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    private val mapper: SiteMonitorMapper,
    private val siteMonitorUtils: SiteMonitorUtils,
    private val siteStore: SiteStore,
) : ScopedViewModel(bgDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var siteMonitorType: SiteMonitorType
    private lateinit var urlTemplate: String

    private val _uiState = MutableStateFlow<SiteMonitorUiState>(SiteMonitorUiState.Preparing)
    val uiState: StateFlow<SiteMonitorUiState> = _uiState

    // todo: annmarie - change this to a single live event - eventually, so we don't leak stuff here
    private val _webViewState = MutableLiveData<SiteMonitorWebViewState?>(null)
    val webViewState: LiveData<SiteMonitorWebViewState?> = _webViewState

    fun start(type: SiteMonitorType, urlTemplate: String, site: SiteModel, hasWebViewState: Boolean = false) {
        this.siteMonitorType = type
        this.urlTemplate = urlTemplate
        this.site = site

        if (hasWebViewState) {
            Log.i(javaClass.simpleName, "***=> start: hasWebViewState is not null")
            postUiState(mapper.toLoaded())
            return
        } else {
            Log.i(javaClass.simpleName, "***=> start: hasWebViewState is null")
            loadView()
            return
        }
    }

    private fun loadView() {
        postUiState(SiteMonitorUiState.Preparing)

        if (!checkForInternetConnectivityAndPostErrorIfNeeded()) return

        if (!validateAndPostErrorIfNeeded()) return

        assembleAndShowSiteMonitor()
    }

    private fun checkForInternetConnectivityAndPostErrorIfNeeded() : Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return true
        postUiState(mapper.toNoNetworkError(this@SiteMonitorTabViewModel::loadView))
        return false
    }

    private fun validateAndPostErrorIfNeeded(): Boolean {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postUiState(mapper.toGenericError(this@SiteMonitorTabViewModel::loadView))
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
        launch {
            _uiState.value = state
        }
    }

    fun onUrlLoaded() {
        Log.i(javaClass.simpleName, "***=> getWebViewState: onUrlLoaded")
        postUiState(mapper.toLoaded())
    }

    fun onWebViewError() {
        postUiState(mapper.toGenericError(this@SiteMonitorTabViewModel::loadView))
    }

    fun saveWebViewState(state: SiteMonitorWebViewState) {
        Log.i(javaClass.simpleName, "***=> saveWebViewState: $state")
        launch { _webViewState.postValue(state) }
    }

    // todo: annmarie - need to get the webstate in a single shot - not waiting for a notification and such, unless
    // this is a blocking call
    fun getWebViewState(): SiteMonitorWebViewState? {
        val state = _webViewState.value
        Log.i(javaClass.simpleName, "***=> getWebViewState: $state")
        return state
    }

    companion object {
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"
    }
}
