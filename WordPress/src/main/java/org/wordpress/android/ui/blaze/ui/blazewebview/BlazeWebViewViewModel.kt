package org.wordpress.android.ui.blaze.ui.blazewebview

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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.WPWebViewActivity.getAuthenticationPostData
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.BlazeWebViewHeaderUiState
import org.wordpress.android.ui.blaze.BlazeWebViewContentUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

@Suppress("ForbiddenComment")
@HiltViewModel
class BlazeWebViewViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteStore: SiteStore
) : ViewModel() {
    // todo: enhance this and add a way to identify start/end steps
    // This may be difficult, if at the end of the creation flow, we are sending the user to browse campaigns
    private val hideCancelSteps = listOf("#step-3", "#step-4")
    private lateinit var blazeFlowSource: BlazeFlowSource

    private val _actionEvents = Channel<BlazeActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private val _blazeHeaderState = MutableStateFlow<BlazeWebViewHeaderUiState>(BlazeWebViewHeaderUiState.ShowAction())
    val blazeHeaderState: StateFlow<BlazeWebViewHeaderUiState> = _blazeHeaderState

    private val _model = MutableStateFlow(BlazeWebViewContentUiState())
    val model: StateFlow<BlazeWebViewContentUiState> = _model

    fun start(promoteScreen: BlazeUiState.PromoteScreen?, source: BlazeFlowSource) {
        blazeFlowSource = source
        val url = buildUrl(promoteScreen)
        postScreenState(model.value.copy(url = url, addressToLoad = prepareUrl(url)))
    }

    // todo: implement "page" flow
    private fun buildUrl(promoteScreen: BlazeUiState.PromoteScreen?): String {
        val siteUrl = extractAndSanitizeSiteUrl()
        if (siteUrl.isEmpty()) {
            postActionEvent(BlazeActionEvent.FinishActivity)
        }

        val url = promoteScreen?.let {
            when (it) {
                is BlazeUiState.PromoteScreen.PromotePost -> {
                    BLAZE_CREATION_FLOW_POST.format(siteUrl, it.postUIModel.postId, blazeFlowSource.trackingName)
                }
                is BlazeUiState.PromoteScreen.Site -> BLAZE_CREATION_FLOW_SITE.format(
                    siteUrl,
                    blazeFlowSource.trackingName
                )
                is BlazeUiState.PromoteScreen.Page -> BLAZE_CREATION_FLOW_SITE.format(
                    siteUrl,
                    blazeFlowSource.trackingName
                )
            }
        } ?: BLAZE_CREATION_FLOW_SITE.format(siteUrl, blazeFlowSource.trackingName)
        return url
    }

    fun onHeaderActionClick() {
        // todo: Track the cancel click blazeFeatureUtils.track()
        // blaze_flow_canceled
        postActionEvent(BlazeActionEvent.FinishActivity)
    }

    // todo: implement validation checks
    private fun prepareUrl(url: String): String {
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
        // Call the public static method in WPWebViewActivity - no need to recreate functionality with a copy/paste
        return getAuthenticationPostData(WPCOM_LOGIN_URL, addressToLoad, username, "", accessToken)
    }

    private fun postHeaderUiState(state: BlazeWebViewHeaderUiState) {
        viewModelScope.launch {
            _blazeHeaderState.value = state
        }
    }

    private fun postScreenState(state: BlazeWebViewContentUiState) {
        viewModelScope.launch {
            _model.value = state
        }
    }

    private fun postActionEvent(actionEvent: BlazeActionEvent) {
        viewModelScope.launch {
            _actionEvents.send(actionEvent)
        }
    }

    private fun extractAndSanitizeSiteUrl(): String {
        return selectedSiteRepository.getSelectedSite()?.url?.replace(Regex(PATTERN), "")?:""
    }

    fun hideOrShowCancelAction(url: String) {
        if (hideCancelSteps.any { url.contains(it) }) {
            postHeaderUiState(BlazeWebViewHeaderUiState.HideAction())
        } else {
            postHeaderUiState(BlazeWebViewHeaderUiState.ShowAction())
        }
    }

    // todo Track blaze_flow_error event with Entry point source (when possible)
    // todo An error was received within the webview - What is the proper action? finish the activity?
    fun onWebViewReceivedError() {
        postActionEvent(BlazeActionEvent.FinishActivity)
    }

    fun onRedirectToExternalBrowser(url: String) {
        postActionEvent(BlazeActionEvent.LaunchExternalBrowser(url))
    }

    companion object {
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val WPCOM_DOMAIN = ".wordpress.com"

        private const val BASE_URL = "https://wordpress.com/advertising/"

        const val BLAZE_CREATION_FLOW_POST = "$BASE_URL%s?blazepress-widget=post-%d&_source=%s"
      // todo: future  const val BLAZE_CREATION_FLOW_PAGE = "$BASE_URL%s?blazepress-widget=page-%d&_source=%s"
        const val BLAZE_CREATION_FLOW_SITE = "$BASE_URL%s?_source=%s"
        const val PATTERN = "(https?://)"
    }
}
