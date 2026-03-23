package org.wordpress.android.ui.accounts.login

import android.content.Context
import androidx.core.net.toUri
import com.automattic.android.tracks.crashlogging.CrashLogging
import org.wordpress.android.R
import org.wordpress.android.util.DeviceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.crashlogging.sendReportWithTag
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.applicationPasswordsUrl
import javax.inject.Inject
import javax.inject.Named

private const val URL_TAG = "url"
private const val SUCCESS_TAG = "success"
private const val REASON_TAG = "reason"

class ApplicationPasswordLoginHelper @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcherWrapper: DispatcherWrapper,
    private val siteStore: SiteStore,
    private val uriLoginWrapper: UriLoginWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val wpLoginClient: WpLoginClient,
    private val appLogWrapper: AppLogWrapper,
    private val apiRootUrlCache: ApiRootUrlCache,
    private val discoverSuccessWrapper: DiscoverSuccessWrapper,
    private val crashLogging: CrashLogging
) {
    private var processedAppPasswordData: String? = null

    @Suppress("TooGenericExceptionCaught")
    suspend fun getAuthorizationUrlComplete(siteUrl: String): String =
        try {
            getAuthorizationUrlCompleteInternal(siteUrl)
        } catch (throwable: Throwable) {
            handleAuthenticationDiscoveryError(siteUrl, throwable)
        }

    private suspend fun getAuthorizationUrlCompleteInternal(siteUrl: String): String = withContext(bgDispatcher) {
        when (val urlDiscoveryResult = wpLoginClient.apiDiscovery(siteUrl)) {
            is ApiDiscoveryResult.Success -> {
                val authorizationUrl =
                    discoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl(urlDiscoveryResult)
                val apiRootUrl = discoverSuccessWrapper.getApiRootUrl(urlDiscoveryResult)
                if (apiRootUrl.isNotEmpty()) {
                    // Store the ApiRootUrl for use it after the login
                    apiRootUrlCache.put(UrlUtils.normalizeUrl(siteUrl), apiRootUrl)
                }
                val authorizationUrlComplete =
                    uriLoginWrapper.appendParamsToRestAuthorizationUrl(authorizationUrl)
                appLogWrapper.d(
                    AppLog.T.API,
                    "A_P: Found authorization for $siteUrl URL: $authorizationUrlComplete " +
                            "API_ROOT_URL $apiRootUrl")
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
                authorizationUrlComplete
            }

            is ApiDiscoveryResult.FailureFetchAndParseApiRoot ->
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFetchAndParseApiRoot"))

            is ApiDiscoveryResult.FailureFindApiRoot ->
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFindApiRoot"))

            is ApiDiscoveryResult.FailureParseSiteUrl ->
                handleAuthenticationDiscoveryError(siteUrl, urlDiscoveryResult.error)
        }
    }

    private fun handleAuthenticationDiscoveryError(siteUrl: String, throwable: Throwable): String {
        appLogWrapper.e(AppLog.T.API, "A_P: Error during API discovery for $siteUrl - ${throwable.message}")
        AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        return ""
    }

    @Suppress("ComplexCondition")
    suspend fun storeApplicationPasswordCredentialsFrom(
        urlLogin: UriLogin
    ): Boolean {
        if (urlLogin.apiRootUrl == null ||
            urlLogin.user.isNullOrEmpty() ||
            urlLogin.password.isNullOrEmpty() ||
            urlLogin.siteUrl == null ||
            urlLogin.siteUrl == processedAppPasswordData
        ) {
            logAndReportBadData(urlLogin)
            return false
        }

        return withContext(bgDispatcher) {
            val normalizedUrl = UrlUtils.normalizeUrl(urlLogin.siteUrl)
            val site = siteStore.sites.firstOrNull { UrlUtils.normalizeUrl(it.url) ==  normalizedUrl}
            if (site != null) {
                site.apply {
                    apiRestUsernameEncrypted = ""
                    apiRestPasswordEncrypted = ""
                    apiRestUsernameIV = ""
                    apiRestPasswordIV = ""
                    apiRestUsernamePlain = urlLogin.user
                    apiRestPasswordPlain = urlLogin.password
                    wpApiRestUrl = urlLogin.apiRootUrl
                }
                dispatcherWrapper.updateApplicationPassword(site)
                trackSuccessful(urlLogin.siteUrl)
                processedAppPasswordData = urlLogin.siteUrl // Save locally to avoid duplicated calls
                true
            } else {
                logAndReportSiteNotFound(
                    urlLogin.siteUrl, normalizedUrl
                )
                false
            }
        }
    }

    fun trackStoringFailed(siteUrl: String?, reason: String) {
        val properties: MutableMap<String, String?> = HashMap()
        properties[URL_TAG] = siteUrl
        properties[REASON_TAG] = reason
        AnalyticsTracker.track(
            Stat.APPLICATION_PASSWORD_STORING_FAILED,
            properties
        )
    }

    private fun reportStoringFailedToSentry(
        reason: String,
        detail: String
    ) {
        crashLogging.sendReportWithTag(
            Exception("A_P: $reason — $detail"),
            AppLog.T.DB
        )
    }

    private fun logAndReportBadData(urlLogin: UriLogin) {
        val detail =
            "apiRootUrl isNull=${urlLogin.apiRootUrl == null}" +
                ", user isEmpty=${urlLogin.user.isNullOrEmpty()}" +
                ", password isEmpty=" +
                "${urlLogin.password.isNullOrEmpty()}" +
                ", siteUrl isNull=${urlLogin.siteUrl == null}" +
                ", alreadyProcessed=" +
                "${urlLogin.siteUrl == processedAppPasswordData}"
        appLogWrapper.e(
            AppLog.T.DB,
            "A_P: Cannot save credentials" +
                " for: ${urlLogin.siteUrl} - $detail"
        )
        trackStoringFailed(urlLogin.siteUrl, "bad_data")
        reportStoringFailedToSentry("bad_data", detail)
    }

    private fun logAndReportSiteNotFound(
        siteUrl: String?,
        normalizedUrl: String?
    ) {
        val detail = "$siteUrl (normalized: $normalizedUrl)" +
            " — ${siteStore.sites.size} sites available"
        appLogWrapper.e(
            AppLog.T.DB,
            "A_P: Cannot save credentials" +
                " - site not found: $detail"
        )
        trackStoringFailed(siteUrl, "site_not_found")
        reportStoringFailedToSentry("site_not_found", detail)
    }

    private fun trackSuccessful(siteUrl: String) {
        val properties: MutableMap<String, String?> = HashMap()
        properties[URL_TAG] = siteUrl
        properties[SUCCESS_TAG] = "true"
        AnalyticsTracker.track(
            if (buildConfigWrapper.isJetpackApp) {
                Stat.JP_ANDROID_APPLICATION_PASSWORD_LOGIN
            } else {
                Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN
            },
            properties
        )
        appLogWrapper.d(AppLog.T.DB, "A_P: Saved application password credentials for: $siteUrl")
    }

    fun getSiteUrlLoginFromRawData(url: String): UriLogin {
        return uriLoginWrapper.parseUriLogin(url)
    }

    /**
     * Removes Application Password credentials for sites that have regular credentials as fallback.
     * Sites without regular credentials (username/password) are excluded since they can only
     * authenticate using Application Password.
     * @return the number of sites that were affected
     */
    suspend fun removeAllApplicationPasswordCredentials(): Int {
        return withContext(bgDispatcher) {
            val sites = siteStore.sites
            // Only reset sites that have regular credentials to fall back to
            val sitesToReset = sites.filter {
                !it.apiRestUsernameEncrypted.isNullOrEmpty() && it.hasRegularCredentials()
            }
            sitesToReset.forEach { site ->
                site.apply {
                    apiRestUsernamePlain = ""
                    apiRestPasswordPlain = ""
                    apiRestUsernameEncrypted = ""
                    apiRestPasswordEncrypted = ""
                    apiRestUsernameIV = ""
                    apiRestPasswordIV = ""
                }
                dispatcherWrapper.removeApplicationPassword(site)
            }
            appLogWrapper.d(
                AppLog.T.DB,
                "A_P: Removed application password credentials for: ${sitesToReset.size} sites"
            )
            sitesToReset.size
        }
    }

    private fun SiteModel.hasRegularCredentials(): Boolean {
        return !username.isNullOrEmpty() && !password.isNullOrEmpty()
    }

    /**
     * Returns the count of sites with Application Password credentials that can be reset
     * because of having regular credentials
     */
    fun getResettableApplicationPasswordSitesCount(): Int {
        return siteStore.sites.count {
            !it.apiRestUsernameEncrypted.isNullOrEmpty() && it.hasRegularCredentials()
        }
    }

    fun siteHasBadCredentials(site: SiteModel) =
        site.apiRestUsernamePlain.isNullOrEmpty() || site.apiRestPasswordPlain.isNullOrEmpty()

    /**
     * This class is created to wrap the Uri calls and let us unit test the login helper
     */
    class UriLoginWrapper @Inject constructor(
        private val context: Context,
        private val apiRootUrlCache: ApiRootUrlCache,
        private val buildConfigWrapper: BuildConfigWrapper,
    ) {
        fun parseUriLogin(url: String): UriLogin {
            val uri = url.toUri()
            val siteUrl = UrlUtils.normalizeUrl(uri.getQueryParameter("site_url"))
            val userLogin = uri.getQueryParameter("user_login")
            val password = uri.getQueryParameter("password")
            val apiRootUrl = apiRootUrlCache.get(siteUrl)
            return UriLogin(siteUrl, userLogin, password, apiRootUrl)
        }

        fun appendParamsToRestAuthorizationUrl(authorizationUrl: String?): String {
            return if (authorizationUrl.isNullOrEmpty()) {
                authorizationUrl.orEmpty()
            } else {
                val userDeviceName = DeviceUtils.getInstance().getDeviceName(context)
                val (appName, successUrl) = if (buildConfigWrapper.isJetpackApp) {
                    context.getString(R.string.application_password_app_name_jetpack, userDeviceName) to
                        JETPACK_SUCCESS_URL
                } else {
                    context.getString(R.string.application_password_app_name_wordpress, userDeviceName) to
                        WORDPRESS_SUCCESS_URL
                }

                authorizationUrl.toUri().buildUpon().apply {
                    appendQueryParameter("app_name", appName)
                    appendQueryParameter("success_url", successUrl)
                }.build().toString()
            }
        }
    }

    companion object {
        private const val JETPACK_SUCCESS_URL = "jetpack://app-pass-authorize"
        private const val WORDPRESS_SUCCESS_URL = "wordpress://app-pass-authorize"
    }

    data class UriLogin(
        val siteUrl: String?,
        val user: String?,
        val password: String?,
        val apiRootUrl: String?
    )

    // We need to wrap the dispatcher because tests are failing due to the actions not having a proper equals method
    // so, every action is returning false when compared with the one we want to test
    class DispatcherWrapper @Inject constructor(private val dispatcher: Dispatcher) {
        fun updateApplicationPassword(site: SiteModel) {
            dispatcher.dispatch(
                SiteActionBuilder.newUpdateApplicationPasswordAction(site)
            )
        }

        fun removeApplicationPassword(site: SiteModel) {
            dispatcher.dispatch(
                SiteActionBuilder.newRemoveApplicationPasswordAction(site)
            )
        }
    }

    class DiscoverSuccessWrapper @Inject constructor() {
        fun getApiRootUrl(successObject: ApiDiscoveryResult.Success) = successObject.success.apiRootUrl.url()

        fun getApplicationPasswordsAuthenticationUrl(
            successObject: ApiDiscoveryResult.Success
        ): String = requireNotNull(
            applicationPasswordsUrl(successObject.success.authentication)?.url()
        ) {
            "Application passwords authentication URL is required"
        }
    }
}
