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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class SiteCreationPlansViewModel @Inject constructor(
    private val userAgent: UserAgent,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ViewModel() {
    private val _uiState = MutableStateFlow<SiteCreationPlansUiState>(SiteCreationPlansUiState.Preparing)
    val uiState = _uiState as StateFlow<SiteCreationPlansUiState>

    private val _actionEvents = Channel<SiteCreationPlansActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private lateinit var domain: DomainModel

    fun start(siteCreationState: SiteCreationState) {
        domain = requireNotNull(siteCreationState.domain)
        showPlans()
    }

    fun onPlanSelected(uri: Uri) {
        AppLog.d(AppLog.T.PLANS, uri.toString())

        val planId = uri.getQueryParameter(PLAN_ID_PARAM)?.toInt() ?: 0
        val planSlug = uri.getQueryParameter(PLAN_SLUG_PARAM).orEmpty()
        val domainNameFromRedirectUrl = uri.getQueryParameter(DOMAIN_NAME_PARAM)

        val planModel = PlanModel(productId = planId, productSlug = planSlug)
        postActionEvent(SiteCreationPlansActionEvent.CreateSite(planModel, domainNameFromRedirectUrl))
    }

    fun onUrlLoaded() {
        postUiState(SiteCreationPlansUiState.Loaded)
    }

    fun onWebViewError() {
        postUiState(SiteCreationPlansUiState.GenericError(this@SiteCreationPlansViewModel::launchPlans))
    }

    fun onCalypsoError() {
        postActionEvent(SiteCreationPlansActionEvent.CreateSite(null, domain.domainName))
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
                userAgent = userAgent.toString(),
                enableChromeClient = true,
                url = url,
                addressToLoad = addressToLoad
            )
        ))
    }

    private fun createURL(): String {
        val uriBuilder = Uri.Builder().apply {
            scheme(SCHEME)
            authority(AUTHORITY)
            appendPath(JETPACK_APP_PATH)
            appendPath(PLANS_PATH)
            if (!domain.isFree) {
                appendQueryParameter(PAID_DOMAIN_NAME, domain.domainName)
            }
        }


        return uriBuilder.build().toString()
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
            addressToLoad,
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
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"

        const val SCHEME = "https"
        const val AUTHORITY = "wordpress.com"
        const val JETPACK_APP_PATH = "jetpack-app"
        const val PLANS_PATH = "plans"
        const val PLAN_ID_PARAM = "plan_id"
        const val PLAN_SLUG_PARAM = "plan_slug"
        const val DOMAIN_NAME_PARAM = "domain_name"
        const val PAID_DOMAIN_NAME = "paid_domain_name"
    }
}

sealed class SiteCreationPlansActionEvent {
    data class CreateSite(val planModel: PlanModel?, val domainName: String?) : SiteCreationPlansActionEvent()
}
