package org.wordpress.android.ui.accounts.login

import android.content.Context
import androidx.annotation.VisibleForTesting
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
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
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
import java.net.URI
import javax.inject.Inject
import javax.inject.Named

private const val URL_TAG = "url"
private const val SUCCESS_TAG = "success"
private const val REASON_TAG = "reason"
private const val SOURCE_TAG = "source"
private const val ERROR_TAG = "error"

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
    private val crashLogging: CrashLogging,
    private val wpApiClientProvider: WpApiClientProvider,
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

    sealed class StoreCredentialsResult {
        object Success : StoreCredentialsResult()
        object SiteNotFound : StoreCredentialsResult()
        object BadData : StoreCredentialsResult()
    }

    @Suppress("ComplexCondition")
    suspend fun storeApplicationPasswordCredentialsFrom(
        urlLogin: UriLogin,
        creationSource: String = ""
    ): StoreCredentialsResult {
        // The apiRootUrl is normally populated from the in-memory ApiRootUrlCache during the
        // initial discovery step. It can be missing here if the process was killed between
        // discovery and the auth callback, or if discovery never ran for this exact URL.
        // In that case, fall back to running discovery again from the callback siteUrl so a
        // recoverable cache miss doesn't fail the login.
        val effectiveUrlLogin = if (urlLogin.apiRootUrl.isNullOrEmpty() && !urlLogin.siteUrl.isNullOrEmpty()) {
            val recovered = recoverApiRootUrl(urlLogin.siteUrl)
            if (recovered != null) urlLogin.copy(apiRootUrl = recovered) else urlLogin
        } else {
            urlLogin
        }

        if (effectiveUrlLogin.apiRootUrl.isNullOrEmpty() ||
            effectiveUrlLogin.user.isNullOrEmpty() ||
            effectiveUrlLogin.password.isNullOrEmpty() ||
            effectiveUrlLogin.siteUrl == null ||
            effectiveUrlLogin.siteUrl == processedAppPasswordData
        ) {
            logAndReportBadData(effectiveUrlLogin, creationSource)
            return StoreCredentialsResult.BadData
        }

        return withContext(bgDispatcher) {
            val normalizedUrl = UrlUtils.normalizeUrl(effectiveUrlLogin.siteUrl)
            val sites = siteStore.sites
            val site = findSiteByUrl(normalizedUrl, sites)
            if (site != null) {
                site.apply {
                    apiRestUsernameEncrypted = ""
                    apiRestPasswordEncrypted = ""
                    apiRestUsernameIV = ""
                    apiRestPasswordIV = ""
                    apiRestUsernamePlain = effectiveUrlLogin.user
                    apiRestPasswordPlain = effectiveUrlLogin.password
                    wpApiRestUrl = effectiveUrlLogin.apiRootUrl
                }
                wpApiClientProvider.clearSelfHostedClient(site.id)
                dispatcherWrapper.updateApplicationPassword(site)
                trackSuccessful(effectiveUrlLogin.siteUrl)
                trackCreated(creationSource, success = true)
                processedAppPasswordData = effectiveUrlLogin.siteUrl
                StoreCredentialsResult.Success
            } else {
                logSiteNotFound(effectiveUrlLogin.siteUrl, normalizedUrl, sites)
                StoreCredentialsResult.SiteNotFound
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun recoverApiRootUrl(siteUrl: String): String? = withContext(bgDispatcher) {
        try {
            val result = wpLoginClient.apiDiscovery(siteUrl)
            if (result is ApiDiscoveryResult.Success) {
                val apiRootUrl = discoverSuccessWrapper.getApiRootUrl(result)
                if (apiRootUrl.isNotEmpty()) {
                    apiRootUrlCache.put(UrlUtils.normalizeUrl(siteUrl).orEmpty(), apiRootUrl)
                    appLogWrapper.d(
                        AppLog.T.API,
                        "A_P: Recovered apiRootUrl via fallback discovery for $siteUrl"
                    )
                    apiRootUrl
                } else {
                    null
                }
            } else {
                null
            }
        } catch (throwable: Throwable) {
            appLogWrapper.e(
                AppLog.T.API,
                "A_P: Fallback discovery failed for $siteUrl - ${throwable.message}"
            )
            null
        }
    }

    fun trackStoringFailed(
        siteUrl: String?,
        reason: String,
        creationSource: String = ""
    ) {
        val properties: MutableMap<String, String?> = HashMap()
        properties[URL_TAG] = maskUrl(siteUrl.orEmpty())
        properties[REASON_TAG] = reason
        AnalyticsTracker.track(
            Stat.APPLICATION_PASSWORD_STORING_FAILED,
            properties
        )
        trackCreated(creationSource, success = false, error = reason)
    }

    private fun trackCreated(
        creationSource: String,
        success: Boolean,
        error: String? = null
    ) {
        if (creationSource.isEmpty()) return
        val properties = mutableMapOf<String, String>(
            SOURCE_TAG to creationSource,
            SUCCESS_TAG to success.toString()
        )
        if (!success && !error.isNullOrEmpty()) {
            properties[ERROR_TAG] = error
        }
        AnalyticsTracker.track(
            Stat.APPLICATION_PASSWORD_CREATED,
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

    private fun logAndReportBadData(
        urlLogin: UriLogin,
        creationSource: String
    ) {
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
        trackStoringFailed(urlLogin.siteUrl, "bad_data", creationSource)
        reportStoringFailedToSentry("bad_data", detail)
    }

    private fun logSiteNotFound(
        siteUrl: String?,
        normalizedUrl: String?,
        sites: List<SiteModel>
    ) {
        val availableSiteUrls = sites.joinToString { it.url }
        val logDetail = "$siteUrl (normalized: $normalizedUrl)" +
            " — ${sites.size} sites available: [$availableSiteUrls]"
        appLogWrapper.d(
            AppLog.T.DB,
            "A_P: Site not found locally, will fetch:" +
                " $logDetail"
        )
    }

    private fun trackSuccessful(siteUrl: String) {
        val properties: MutableMap<String, String?> = HashMap()
        properties[URL_TAG] = maskUrl(siteUrl)
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

    private fun findSiteByUrl(
        normalizedUrl: String?,
        sites: List<SiteModel>
    ): SiteModel? {
        if (normalizedUrl.isNullOrEmpty()) return null

        // Exact match first, fallback: compare ignoring scheme and www prefix
        val strippedUrl = normalizedUrl.stripSchemeAndWww()
        return sites.firstOrNull {
            UrlUtils.normalizeUrl(it.url) == normalizedUrl
        } ?: sites.firstOrNull {
            UrlUtils.normalizeUrl(it.url)?.stripSchemeAndWww() == strippedUrl
        }
    }

    private fun String.stripSchemeAndWww(): String {
        return removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
    }

    @Suppress("ReturnCount")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun maskUrl(url: String): String {
        val host = try {
            URI(url).host
        } catch (_: Exception) {
            null
        } ?: return url
        val dotIndex = host.lastIndexOf('.')
        if (dotIndex <= 0) return url
        val domain = host.substring(0, dotIndex)
        val tld = host.substring(dotIndex)
        val maskedDomain = when {
            domain.length <= 2 -> "x".repeat(domain.length)
            else -> domain.first() +
                "x".repeat(domain.length - 2) +
                domain.last()
        }
        return url.replaceFirst(host, maskedDomain + tld)
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
