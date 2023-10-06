package org.wordpress.android.ui.sitecreation.plans

import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class SiteCreationPlansViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ViewModel() {
    private val _uiState = MutableStateFlow<SiteCreationPlansUiState>(SiteCreationPlansUiState.Preparing)
    val uiState = _uiState as StateFlow<SiteCreationPlansUiState>

    private val _actionEvents = Channel<SiteCreationPlansActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private lateinit var domainName: String

    fun start(siteCreationState: SiteCreationState) {
        domainName = requireNotNull(siteCreationState.domain).domainName
        showPlans()
    }

    fun onPlanSelected(url: String) {
        AppLog.d(AppLog.T.PLANS, url)
    }

    fun onCheckoutSuccess() {
        //
    }
    fun onRedirectToExternalBrowser(url: String) {
        postActionEvent(SiteCreationPlansActionEvent.LaunchExternalBrowser(url))
    }

    fun onUrlLoaded() {
        postUiState(SiteCreationPlansUiState.Loaded)
    }

    fun onWebViewError() {
        postUiState(SiteCreationPlansUiState.GenericError(this@SiteCreationPlansViewModel::launchPlans))
    }

    private fun showPlans() {
        postUiState(SiteCreationPlansUiState.Preparing)
        if (!checkForInternetConnectivityAndPostErrorIfNeeded()) return
        if (!validateAndPostErrorIfNeeded()) return
        launchPlans()
    }

    private fun launchPlans() {
        val url = createURL()
        AppLog.d(AppLog.T.PLANS, url)

        val addressToLoad = prepareAddressToLoad(url)
        postUiState(SiteCreationPlansUiState.Prepared(
            SiteCreationPlansModel(
                enableJavascript = true,
                enableDomStorage = true,
                userAgent = WordPress.getUserAgent(),
                enableChromeClient = true,
                url = url,
                addressToLoad = addressToLoad
            )
        ))
    }

    private fun createURL(): String {
        // Temporarily using freed domain with annual plan url, till Calypso PR is merged
        return Uri.Builder().apply {
            scheme("https")
            authority("wordpress.com")
            appendPath(PLANS_PATH)
            appendPath(PLANS_FREQUENCY_PATH)
            appendPath(extractAndSanitizeSiteUrl())
            appendQueryParameter(PLANS_PACKAGE, "true")
            appendQueryParameter(PLANS_JETPACK_APP, "true")
            appendQueryParameter("redirect_to", REDIRECT_TO )
            build()
        }.toString()


//        return Uri.Builder().apply {
//            scheme("https")
//            authority("container-jolly-cerf.calypso.live")
//            appendPath("jetpack-app-plans")
//            appendQueryParameter(PAID_DOMAIN_NAME, domainName)
//            appendQueryParameter(REDIRECT_TO, REDIRECT_SCHEME)
//            build()
//        }.toString()
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
        return WPWebViewActivity.getAuthenticationPostData(
            WPCOM_LOGIN_URL,
            url, // addressToLoad,
            username,
            "",
            accessToken?:""
        )
    }

    private fun validateAndPostErrorIfNeeded(): Boolean {
        if (accountStore.account.userName.isNullOrEmpty() || accountStore.accessToken.isNullOrEmpty()) {
            postUiState(SiteCreationPlansUiState.GenericError(this@SiteCreationPlansViewModel::showPlans))
            return false
        }
        return true
    }

    private fun checkForInternetConnectivityAndPostErrorIfNeeded(): Boolean {
        if (networkUtilsWrapper.isNetworkAvailable()) return true
        postUiState(SiteCreationPlansUiState.NoNetworkError(this@SiteCreationPlansViewModel::showPlans))
        return false
    }

    private fun postUiState(state: SiteCreationPlansUiState) {
        viewModelScope.launch {
            _uiState.value = state
        }
    }

    private fun postActionEvent(actionEvent: SiteCreationPlansActionEvent) {
        viewModelScope.launch {
            _actionEvents.send(actionEvent)
        }
    }

    companion object {
        const val HTTP_PATTERN = "(https?://)"
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"

        const val WP_HOST = "https://wordpress.com"
        const val PLANS_PATH = "plans" // "jetpack-app-plans"
        const val REDIRECT_TO = "redirect_to"
        const val REDIRECT_SCHEME = "jetpackapp://"
        const val PAID_DOMAIN_NAME = "paid_domain_name"
        const val PLANS_FREQUENCY_PATH = "yearly" // not required
        const val PLANS_JETPACK_APP = "jetpackAppPlans" // not required
        const val PLANS_PACKAGE = "domainAndPlanPackage" // not required
    }
}

sealed class SiteCreationPlansActionEvent {
    object FinishActivity : SiteCreationPlansActionEvent()
    data class LaunchExternalBrowser(val url: String) : SiteCreationPlansActionEvent()
}
