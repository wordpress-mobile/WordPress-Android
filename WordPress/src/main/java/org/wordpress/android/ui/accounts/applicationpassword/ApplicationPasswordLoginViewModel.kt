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
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.StoreCredentialsResult
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

    private val creationSource =
        ApplicationPasswordCreationTracker.consumePendingCreationSource()
    private var currentUrlLogin: UriLogin? = null
    private var oldSitesIDs: ArrayList<Int>? = null
    @Volatile
    private var waitingForFetchedSite = false

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
                applicationPasswordLoginHelper.trackStoringFailed(
                    "", "empty_raw_data", creationSource
                )
                emitError(siteUrl = "", errorMessage = "empty_raw_data")
                return@launch
            }
            val urlLogin = applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)
            currentUrlLogin = urlLogin
            // Store credentials if the site already exists
            when (storeCredentials(urlLogin)) {
                is StoreCredentialsResult.Success -> {
                    _onFinishedEvent.emit(
                        NavigationActionData(
                            showSiteSelector = false,
                            siteUrl = urlLogin.siteUrl,
                            oldSitesIDs = oldSitesIDs,
                            isError = false,
                        )
                    )
                }
                is StoreCredentialsResult.SiteNotFound -> {
                    waitingForFetchedSite = true
                    fetchSites(
                        urlLogin.user.orEmpty(),
                        urlLogin.password.orEmpty(),
                        urlLogin.siteUrl.orEmpty(),
                        urlLogin.apiRootUrl.orEmpty()
                    )
                }
                is StoreCredentialsResult.BadData -> {
                    // Already tracked inside the helper
                    emitError(
                        siteUrl = urlLogin.siteUrl.orEmpty(),
                        errorMessage = "bad_data"
                    )
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun storeCredentials(
        urlLogin: UriLogin
    ): StoreCredentialsResult = withContext(ioDispatcher) {
        try {
            applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(
                    urlLogin, creationSource
                )
        } catch (e: Exception) {
            appLogWrapper.e(
                AppLog.T.DB,
                "A_P: Error storing credentials:" +
                    " ${e.stackTraceToString()}"
            )
            applicationPasswordLoginHelper.trackStoringFailed(
                urlLogin.siteUrl,
                "store_credentials_exception",
                creationSource
            )
            crashLogging.sendReportWithTag(e, AppLog.T.DB)
            StoreCredentialsResult.BadData
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
            if (username.isEmpty() || password.isEmpty()
                || siteUrl.isEmpty() || apiRootUrl.isEmpty()
            ) {
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: Cannot fetch sites for credential storing" +
                        " - username isEmpty=${username.isEmpty()}" +
                        ", password isEmpty=${password.isEmpty()}" +
                        ", siteUrl isEmpty=${siteUrl.isEmpty()}" +
                        ", apiRootUrl isEmpty=${apiRootUrl.isEmpty()}"
                )
                applicationPasswordLoginHelper.trackStoringFailed(
                    siteUrl, "empty_fetch_params", creationSource
                )
                emitError(
                    siteUrl = siteUrl,
                    errorMessage = "empty_fetch_params"
                )
            } else {
                discoverAndDispatchFetchSite(
                    username, password, siteUrl, apiRootUrl
                )
            }
        } catch (e: Exception) {
            appLogWrapper.e(
                AppLog.T.API,
                "A_P: Error fetching sites: ${e.stackTraceToString()}"
            )
            applicationPasswordLoginHelper.trackStoringFailed(
                siteUrl, "fetch_sites_exception", creationSource
            )
            emitError(siteUrl = siteUrl, errorMessage = e.message, cause = e)
        }
    }

    private suspend fun discoverAndDispatchFetchSite(
        username: String,
        password: String,
        siteUrl: String,
        apiRootUrl: String
    ) {
        val xmlRpcEndpoint = try {
            selfHostedEndpointFinder
                .verifyOrDiscoverXMLRPCEndpoint(siteUrl)
        } catch (e: SelfHostedEndpointFinder.DiscoveryException) {
            appLogWrapper.w(
                AppLog.T.API,
                "A_P: XML-RPC discovery failed" +
                    " (${e.message}). Falling back to" +
                    " WPAPI fetch using" +
                    " apiRootUrl=$apiRootUrl"
            )
            null
        }
        val payload =
            SiteStore.RefreshSitesXMLRPCApplicationPasswordCredentialsPayload(
                username = username,
                password = password,
                url = xmlRpcEndpoint ?: siteUrl,
                apiRootUrl = apiRootUrl,
            )
        if (xmlRpcEndpoint != null) {
            dispatcher.dispatch(
                SiteActionBuilder
                    .newFetchSitesXmlRpcFromApplicationPasswordAction(
                        payload
                    )
            )
        } else {
            dispatcher.dispatch(
                SiteActionBuilder
                    .newFetchSiteWpApiFromApplicationPasswordAction(
                        payload
                    )
            )
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSiteChanged(event: OnSiteChanged) {
        if (!waitingForFetchedSite) return
        viewModelScope.launch {
            if (event.isError) {
                handleSiteChangedError(event)
            } else {
                handleSiteChangedSuccess(event)
            }
        }
    }

    private suspend fun handleSiteChangedError(event: OnSiteChanged) {
        waitingForFetchedSite = false
        val error = event.error
        appLogWrapper.e(
            AppLog.T.MAIN,
            "A_P: onSiteChanged failed: " +
                "SiteStore error ${error?.type}: ${error?.message}"
        )
        applicationPasswordLoginHelper.trackStoringFailed(
            currentUrlLogin?.siteUrl,
            "site_changed_failed",
            creationSource
        )
        emitError(
            siteUrl = currentUrlLogin?.siteUrl.orEmpty(),
            errorMessage = "site_store_error"
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun handleSiteChangedSuccess(event: OnSiteChanged) {
        waitingForFetchedSite = false
        val normalizedUrl =
            UrlUtils.normalizeUrl(currentUrlLogin?.siteUrl)

        val site = try {
            siteStore.sites.firstOrNull {
                UrlUtils.normalizeUrl(it.url) == normalizedUrl
            }
        } catch (e: Exception) {
            logAndEmitSiteChangedError(
                logMessage = "exception reading sites from DB: " +
                    e.stackTraceToString(),
                errorCode = "db_read_exception",
                cause = e
            )
            return
        }

        val validationError = validateSiteChanged(event, site)
        if (validationError != null) {
            logAndEmitSiteChangedError(
                logMessage = validationError.logMessage,
                errorCode = validationError.errorCode
            )
        } else {
            val resolvedSite = site ?: return
            AnalyticsTracker.track(
                Stat.APPLICATION_PASSWORD_CREATED,
                mapOf("source" to creationSource, "success" to "true")
            )
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

    private fun validateSiteChanged(
        event: OnSiteChanged,
        site: SiteModel?
    ): SiteChangedValidationError? = when {
        event.rowsAffected < 1 -> SiteChangedValidationError(
            logMessage = "No rows affected " +
                "(rowsAffected=${event.rowsAffected})",
            errorCode = "no_rows_affected"
        )
        site == null -> SiteChangedValidationError(
            logMessage = "Site not found after update",
            errorCode = "site_not_found"
        )
        applicationPasswordLoginHelper
            .siteHasBadCredentials(site) -> SiteChangedValidationError(
            logMessage = "Credentials are empty after store",
            errorCode = "empty_credentials"
        )
        else -> null
    }

    private data class SiteChangedValidationError(
        val logMessage: String,
        val errorCode: String
    )

    private suspend fun logAndEmitSiteChangedError(
        logMessage: String,
        errorCode: String,
        cause: Throwable? = null
    ) {
        appLogWrapper.e(
            AppLog.T.MAIN,
            "A_P: onSiteChanged failed: $logMessage"
        )
        applicationPasswordLoginHelper.trackStoringFailed(
            currentUrlLogin?.siteUrl,
            "site_changed_failed",
            creationSource
        )
        emitError(
            siteUrl = currentUrlLogin?.siteUrl.orEmpty(),
            errorMessage = errorCode,
            cause = cause
        )
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
