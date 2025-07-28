package org.wordpress.android.ui.accounts.applicationpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.login.util.SiteUtils
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Named

class ApplicationPasswordLoginViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    // Dispatcher is the way to dispatch actions to Flux. It will call siteStore.onAction()
    private val dispatcher: Dispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _onFinishedEvent = MutableSharedFlow<NavigationActionData>()
    /**
     * A shared flow that emits the site URL when the setup is finished.
     * It can emit null if the site could not be set up.
     */
    val onFinishedEvent = _onFinishedEvent.asSharedFlow()

    private var currentUrlLogin: UriLogin? = null
    private var oldSitesIDs: ArrayList<Int>? = null

    fun onStart() {
        dispatcher.register(this)
        oldSitesIDs = SiteUtils.getCurrentSiteIds(siteStore, false)
    }

    fun onStop() {
        dispatcher.unregister(this)
    }

    /**
     * This method is called to set up the site with the provided raw data.
     *
     * @param rawData The raw data containing the callback data from the application password login.
     */
    fun setupSite(rawData: String) {
        viewModelScope.launch {
            if (rawData.isEmpty()) {
                appLogWrapper.e(AppLog.T.MAIN, "Cannot store credentials: rawData is empty")
                _onFinishedEvent.emit(
                    NavigationActionData(
                        showSiteSelector = false,
                        showPostSignupInterstitial = false,
                        siteUrl = "",
                        oldSitesIDs = oldSitesIDs,
                        isError = true
                    )
                )
                return@launch
            }
            val urlLogin = applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)
            currentUrlLogin = urlLogin
            // Store credentials if the site already exists
            val credentialsStored = storeCredentials(urlLogin)
            // If the site already exists, we can skip fetching it again
            if (!credentialsStored) {
                fetchSites(
                    urlLogin.user.orEmpty(),
                    urlLogin.password.orEmpty(),
                    urlLogin.siteUrl.orEmpty(),
                    urlLogin.apiRootUrl.orEmpty()
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun storeCredentials(urlLogin: UriLogin): Boolean = withContext(ioDispatcher) {
        try {
            applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(urlLogin)
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.DB, "Error storing credentials: ${e.stackTraceToString()}")
            false
        }
    }

    @Suppress("TooGenericExceptionCaught", "ComplexCondition")
    private suspend fun fetchSites(
        username: String,
        password: String,
        siteUrl: String,
        apiRootUrl: String
        ) = withContext(ioDispatcher) {
        try {
            if (username.isEmpty() || password.isEmpty() || siteUrl.isEmpty() || apiRootUrl.isEmpty()) {
                appLogWrapper.e(AppLog.T.MAIN, "Cannot fetch sites for credential storing: UriLogin is empty")
                emitErrorFetching(siteUrl)
            } else {
                val xmlRpcEndpoint =
                    selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(siteUrl)
                dispatcher.dispatch(
                    SiteActionBuilder.newFetchSitesXmlRpcFromApplicationPasswordAction(
                        SiteStore.RefreshSitesXMLRPCApplicationPasswordCredentialsPayload(
                            username = username,
                            password = password,
                            url = xmlRpcEndpoint,
                            apiRootUrl = apiRootUrl,
                        )
                    )
                )
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "Error fetching sites: ${e.stackTraceToString()}")
            emitErrorFetching(siteUrl)
        }
    }

    private suspend fun emitErrorFetching(siteUrl: String) =  _onFinishedEvent.emit(
        NavigationActionData(
            showSiteSelector = false,
            showPostSignupInterstitial = false,
            siteUrl = siteUrl,
            oldSitesIDs = oldSitesIDs,
            isError = true
        )
    )

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSiteChanged(event: OnSiteChanged) {
        viewModelScope.launch {
            val currentNormalizedUrl = UrlUtils.normalizeUrl(currentUrlLogin?.siteUrl)
            val site = siteStore.sites.firstOrNull { UrlUtils.normalizeUrl(it.url) == currentNormalizedUrl }
            if (event.rowsAffected < 1 || site == null || applicationPasswordLoginHelper.siteHasBadCredentials(site)) {
                appLogWrapper.e(AppLog.T.MAIN, "Site not found or credentials are empty.")
                _onFinishedEvent.emit(
                    NavigationActionData(
                        showSiteSelector = false,
                        showPostSignupInterstitial = false,
                        siteUrl = currentUrlLogin?.siteUrl,
                        oldSitesIDs = oldSitesIDs,
                        isError = true
                    )
                )
            } else {
                _onFinishedEvent.emit(
                    NavigationActionData(
                        showSiteSelector = siteStore.hasSite() &&
                                oldSitesIDs?.contains(site.id) != true, // null or false
                        showPostSignupInterstitial = !siteStore.hasSite()
                                && appPrefsWrapper.shouldShowPostSignupInterstitial,
                        siteUrl = currentUrlLogin?.siteUrl,
                        oldSitesIDs = oldSitesIDs,
                        isError = false
                    )
                )
            }
        }
    }

    data class NavigationActionData(
        val showSiteSelector: Boolean,
        val showPostSignupInterstitial: Boolean,
        val siteUrl: String?,
        val oldSitesIDs: ArrayList<Int>?,
        val isError: Boolean
    )
}
