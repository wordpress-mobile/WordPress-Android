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
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import com.automattic.android.tracks.crashlogging.CrashLogging
import org.wordpress.android.util.crashlogging.sendReportWithTag
import javax.inject.Inject
import javax.inject.Named

class ApplicationPasswordLoginViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    // Dispatcher is the way to dispatch actions to Flux. It will call siteStore.onAction()
    private val dispatcher: Dispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteStore: SiteStore,
    private val appLogWrapper: AppLogWrapper,
    private val crashLogging: CrashLogging,
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
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: Cannot store credentials: rawData is empty"
                )
                applicationPasswordLoginHelper.trackStoringFailed("", "empty_raw_data")
                emitError(siteUrl = "", errorMessage = "Callback data was empty")
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
            appLogWrapper.e(
                AppLog.T.DB,
                "A_P: Error storing credentials: ${e.stackTraceToString()}"
            )
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
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: Cannot fetch sites for credential storing" +
                        " - username isEmpty=${username.isEmpty()}" +
                        ", password isEmpty=${password.isEmpty()}" +
                        ", siteUrl isEmpty=${siteUrl.isEmpty()}" +
                        ", apiRootUrl isEmpty=${apiRootUrl.isEmpty()}"
                )
                applicationPasswordLoginHelper.trackStoringFailed(
                    siteUrl, "empty_fetch_params"
                )
                emitError(
                    siteUrl = siteUrl,
                    errorMessage = "Missing login data — " +
                        "username empty: ${username.isEmpty()}, " +
                        "password empty: ${password.isEmpty()}, " +
                        "apiRootUrl empty: ${apiRootUrl.isEmpty()}"
                )
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
            appLogWrapper.e(
                AppLog.T.API,
                "A_P: Error fetching sites: ${e.stackTraceToString()}"
            )
            applicationPasswordLoginHelper.trackStoringFailed(
                siteUrl, "fetch_sites_exception"
            )
            emitError(siteUrl = siteUrl, errorMessage = e.message, cause = e)
        }
    }

    private suspend fun emitError(
        siteUrl: String,
        errorMessage: String? = null,
        cause: Throwable? = null
    ) {
        val exception = cause
            ?: Exception("Application password login failed: $errorMessage")
        crashLogging.sendReportWithTag(exception, AppLog.T.MAIN)
        _onFinishedEvent.emit(
            NavigationActionData(
                showSiteSelector = false,
                siteUrl = siteUrl,
                oldSitesIDs = oldSitesIDs,
                isError = true,
                errorMessage = errorMessage
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSiteChanged(event: OnSiteChanged) {
        viewModelScope.launch {
            val currentNormalizedUrl = UrlUtils.normalizeUrl(currentUrlLogin?.siteUrl)

            if (event.isError) {
                val error = event.error
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: onSiteChanged failed: " +
                        "SiteStore error ${error?.type}: ${error?.message}"
                )
                applicationPasswordLoginHelper.trackStoringFailed(
                    currentUrlLogin?.siteUrl,
                    "site_changed_failed"
                )
                emitError(
                    siteUrl = currentUrlLogin?.siteUrl.orEmpty(),
                    errorMessage = "SiteStore error: " +
                        "${error?.type} — ${error?.message}"
                )
                return@launch
            }

            val site = try {
                siteStore.sites.firstOrNull {
                    UrlUtils.normalizeUrl(it.url) == currentNormalizedUrl
                }
            } catch (e: Exception) {
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: onSiteChanged failed: " +
                        "exception reading sites from DB: " +
                        e.stackTraceToString()
                )
                applicationPasswordLoginHelper.trackStoringFailed(
                    currentUrlLogin?.siteUrl,
                    "site_changed_failed"
                )
                emitError(
                    siteUrl = currentUrlLogin?.siteUrl.orEmpty(),
                    errorMessage = "Failed to read sites: ${e.message}",
                    cause = e
                )
                return@launch
            }

            val errorMessage = when {
                event.rowsAffected < 1 -> {
                    "No rows affected (rowsAffected=${event.rowsAffected})"
                }
                site == null -> {
                    "Site not found for URL: $currentNormalizedUrl"
                }
                applicationPasswordLoginHelper.siteHasBadCredentials(site) -> {
                    "Credentials are empty for site: $currentNormalizedUrl"
                }
                else -> null
            }

            if (errorMessage != null) {
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: onSiteChanged failed: $errorMessage"
                )
                applicationPasswordLoginHelper.trackStoringFailed(
                    currentUrlLogin?.siteUrl,
                    "site_changed_failed"
                )
                emitError(
                    siteUrl = currentUrlLogin?.siteUrl.orEmpty(),
                    errorMessage = errorMessage
                )
            } else {
                val resolvedSite = site ?: return@launch
                _onFinishedEvent.emit(
                    NavigationActionData(
                        showSiteSelector = siteStore.hasSite() &&
                                oldSitesIDs?.contains(resolvedSite.id) != true,
                        siteUrl = currentUrlLogin?.siteUrl,
                        oldSitesIDs = oldSitesIDs,
                        isError = false,
                        newSiteLocalId = resolvedSite.id
                    )
                )
            }
        }
    }

    data class NavigationActionData(
        val showSiteSelector: Boolean,
        val siteUrl: String?,
        val oldSitesIDs: ArrayList<Int>?,
        val isError: Boolean,
        val newSiteLocalId: Int? = null,
        val errorMessage: String? = null
    )
}
