package org.wordpress.android.ui.accounts.login

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.automattic.android.tracks.crashlogging.CrashLogging
import org.wordpress.android.R
import org.wordpress.android.util.DeviceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.crashlogging.sendReportWithTag
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.AutoDiscoveryAttemptFailure
import uniffi.wp_api.FetchAndParseApiRootFailure
import uniffi.wp_api.KnownAuthenticationBlockingPlugin
import uniffi.wp_api.WpErrorCode
import uniffi.wp_api.applicationPasswordsUrl
import uniffi.wp_api.localizedDescription
import java.net.URI
import javax.inject.Inject
import javax.inject.Named

// WordPress.com returns this error code from the REST root of a site whose Privacy setting is
// Private (or Coming Soon). The gate sits in front of WordPress, so discovery never reaches the
// API — the site's Application Password support is irrelevant to the failure.
private const val PRIVATE_SITE_ERROR_CODE = "private_site"

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
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    private var processedAppPasswordData: String? = null

    sealed class DiscoveryResult {
        data class Authorized(val authorizationUrl: String) : DiscoveryResult()

        /**
         * Discovery couldn't reach or read the site's REST API. [userFacingMessage] is the library's
         * description of what went wrong; [reason] narrows it when we can recognise the cause, so the
         * UI can explain it rather than guess.
         */
        data class Failed(
            val userFacingMessage: String,
            val reason: FailureReason = FailureReason.Unknown,
        ) : DiscoveryResult()

        /**
         * The site is hosted on WordPress.com: API discovery reported OAuth2 as the authentication
         * mechanism, so it can't use Application Passwords and should log in via WordPress.com.
         */
        object WpComSite : DiscoveryResult()

        /** A recognised cause for a [Failed] discovery. */
        enum class FailureReason {
            /** Nothing more specific than the library's message. */
            Unknown,

            /**
             * The site's Privacy setting blocks anonymous requests, so discovery got a 403 instead of
             * the REST root. Nothing is wrong with the site's Application Password support — it just
             * has to be publicly reachable for the login flow to read its API.
             */
            PrivateSite,

            /**
             * Discovery succeeded but the site advertises no application-passwords endpoint, so it
             * genuinely can't be logged into this way.
             */
            NotSupported,
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun getAuthorizationUrlComplete(siteUrl: String, source: DiscoverySource): DiscoveryResult =
        try {
            getAuthorizationUrlCompleteInternal(siteUrl, source)
        } catch (throwable: Throwable) {
            handleAuthenticationDiscoveryError(
                siteUrl = siteUrl,
                source = source,
                message = throwable.message ?: throwable::class.simpleName.orEmpty(),
                failure = throwable.toDiscoveryFailure(),
            )
        }

    private suspend fun getAuthorizationUrlCompleteInternal(
        siteUrl: String,
        source: DiscoverySource,
    ): DiscoveryResult =
        withContext(bgDispatcher) {
            when (val urlDiscoveryResult = wpLoginClient.apiDiscovery(siteUrl)) {
                is ApiDiscoveryResult.Success -> {
                    if (discoverSuccessWrapper.isWpComSite(urlDiscoveryResult)) {
                        // WordPress.com sites report OAuth2 as the authentication mechanism; they
                        // can't use Application Passwords and must log in via WordPress.com.
                        appLogWrapper.d(AppLog.T.API, "A_P: $siteUrl is a WordPress.com site (OAuth2)")
                        trackDiscoverySuccessful(siteUrl, source, isWpCom = true)
                        DiscoveryResult.WpComSite
                    } else {
                        val authorizationUrl =
                            discoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl(urlDiscoveryResult)
                        if (authorizationUrl == null) {
                            // Discovery worked; the site just doesn't offer application passwords. This
                            // is the one case the old blanket "not supported" message was right about.
                            return@withContext handleAuthenticationDiscoveryError(
                                siteUrl = siteUrl,
                                source = source,
                                message = "No application-passwords authentication URL advertised",
                                reason = DiscoveryResult.FailureReason.NotSupported,
                                failure = noAuthenticationUrlFailure(
                                    discoverSuccessWrapper.getBlockingPlugins(urlDiscoveryResult)
                                ),
                            )
                        }
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
                        trackDiscoverySuccessful(siteUrl, source, isWpCom = false)
                        DiscoveryResult.Authorized(authorizationUrlComplete)
                    }
                }

                is ApiDiscoveryResult.Failure ->
                    handleAuthenticationDiscoveryError(
                        siteUrl = siteUrl,
                        source = source,
                        // 0.8.0 replaced userFacingErrorMessage() with localizedDescription(), which
                        // returns a translated sentence. This message is shown to the user on the
                        // login screen, so the raw Throwable message (an internal debug dump of the
                        // discovery attempt) must not be used here.
                        message = urlDiscoveryResult.failure.localizedDescription(),
                        reason = urlDiscoveryResult.failureReason(),
                        failure = urlDiscoveryResult.failure.toDiscoveryFailure(),
                    )
            }
        }

    /**
     * Recognise causes worth naming to the user. A [FetchAndParseApiRootFailure.WpError] means we
     * reached the site and it answered with a REST error envelope, so its `code` is a reliable
     * signal — WordPress.com sends `private_site` from a site whose Privacy setting hides it.
     */
    private fun ApiDiscoveryResult.failureReason(): DiscoveryResult.FailureReason {
        val wpError = ((this as? ApiDiscoveryResult.Failure)?.failure
            as? AutoDiscoveryAttemptFailure.FetchAndParseApiRoot)
            ?.fetchAndParseApiRootFailure as? FetchAndParseApiRootFailure.WpError
            ?: return DiscoveryResult.FailureReason.Unknown
        // `private_site` has no dedicated WpErrorCode, so the library surfaces it as a CustomException
        // carrying the raw code string.
        val rawCode = (wpError.errorCode as? WpErrorCode.CustomException)?.v1
        return if (rawCode == PRIVATE_SITE_ERROR_CODE) {
            DiscoveryResult.FailureReason.PrivateSite
        } else {
            DiscoveryResult.FailureReason.Unknown
        }
    }

    private fun trackDiscoverySuccessful(siteUrl: String, source: DiscoverySource, isWpCom: Boolean) {
        analyticsTracker.track(
            Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL,
            mapOf(
                URL_TAG to maskUrl(siteUrl),
                SOURCE_TAG to source.value,
                // WordPress.com sites are counted as successful discoveries even though they can't
                // use Application Passwords. Without this they're fused with the genuine successes.
                IS_WPCOM_TAG to isWpCom.toString(),
            )
        )
    }

    private fun handleAuthenticationDiscoveryError(
        siteUrl: String,
        source: DiscoverySource,
        message: String,
        failure: DiscoveryFailure,
        reason: DiscoveryResult.FailureReason = DiscoveryResult.FailureReason.Unknown,
    ): DiscoveryResult {
        appLogWrapper.e(
            AppLog.T.API,
            "A_P: Error during API discovery for $siteUrl - $message ($reason, ${failure.reason})"
        )
        analyticsTracker.track(
            Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED,
            failure.props + mapOf(URL_TAG to maskUrl(siteUrl), SOURCE_TAG to source.value)
        )
        // A background card probe isn't a login attempt, so it must stay out of the login
        // event's denominator — otherwise every re-probe of a broken site inflates the failures.
        if (source != DiscoverySource.MY_SITE_CARD) {
            trackLoginFailed(siteUrl, "discovery_${failure.reason}")
        }
        return DiscoveryResult.Failed(message, reason)
    }

    sealed class StoreCredentialsResult {
        object Success : StoreCredentialsResult()
        // Carries the effective login (with any recovered apiRootUrl) so the caller can fetch
        // the site with valid params instead of the original, possibly-incomplete, urlLogin.
        data class SiteNotFound(val urlLogin: UriLogin) : StoreCredentialsResult()
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
                StoreCredentialsResult.SiteNotFound(effectiveUrlLogin)
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
        analyticsTracker.track(
            Stat.APPLICATION_PASSWORD_STORING_FAILED,
            properties
        )
        trackCreated(creationSource, success = false, error = reason)
        // Every post-callback failure funnels through here, so this is the one place that gives the
        // login event a denominator. User cancellation included — it's an outcome, not a non-event.
        trackLoginFailed(siteUrl, reason)
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
        analyticsTracker.track(
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
        analyticsTracker.track(
            applicationPasswordLoginStat(),
            mapOf(
                URL_TAG to maskUrl(siteUrl),
                SUCCESS_TAG to true.toString(),
            )
        )
        appLogWrapper.d(AppLog.T.DB, "A_P: Saved application password credentials for: $siteUrl")
    }

    /**
     * The failure counterpart of [trackSuccessful]. Until this existed the login event only ever
     * fired on success, so failures were invisible and successes had nothing to be a share of.
     */
    private fun trackLoginFailed(siteUrl: String?, error: String) {
        analyticsTracker.track(
            applicationPasswordLoginStat(),
            mapOf(
                URL_TAG to maskUrl(siteUrl.orEmpty()),
                SUCCESS_TAG to false.toString(),
                ERROR_TAG to error,
            )
        )
    }

    private fun applicationPasswordLoginStat() = if (buildConfigWrapper.isJetpackApp) {
        Stat.JP_ANDROID_APPLICATION_PASSWORD_LOGIN
    } else {
        Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN
    }

    fun getSiteUrlLoginFromRawData(url: String): UriLogin {
        return uriLoginWrapper.parseUriLogin(url)
    }

    fun isUserRejectedAuthorization(rawData: String): Boolean {
        return uriLoginWrapper.isUserRejectedAuthorization(rawData)
    }

    internal fun findSiteByUrl(
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

        // WordPress' authorize-application.php redirects to the success_url with `success=false`
        // when the user clicks "No, I do not approve this connection". No credentials are returned.
        fun isUserRejectedAuthorization(rawData: String): Boolean {
            if (rawData.isEmpty()) return false
            return runCatching { rawData.toUri().getQueryParameter("success") }.getOrNull() == "false"
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

        /**
         * WordPress.com sites advertise OAuth2 as their authentication mechanism during API
         * discovery (self-hosted sites advertise Application Passwords). Self-hosted sites can
         * also expose OAuth2 via plugins, so we additionally require the advertised OAuth2
         * authorization endpoint to be hosted on wordpress.com. Otherwise a malicious site could
         * pose as WordPress.com to hijack our login flow.
         */
        fun isWpComSite(successObject: ApiDiscoveryResult.Success): Boolean {
            val authentication = successObject.success.authentication
            return authentication is DiscoveredAuthenticationMechanism.OAuth2 &&
                    WPUrlUtils.isWordPressCom(authentication.endpoints.authorizationUrl)
        }

        /** `null` when the site advertises no application-passwords endpoint. */
        fun getApplicationPasswordsAuthenticationUrl(
            successObject: ApiDiscoveryResult.Success
        ): String? = applicationPasswordsUrl(successObject.success.authentication)?.url()

        /**
         * Plugins whose REST namespaces the site advertises and which are known to block Application
         * Passwords. The matching lives in the library, so this is the same list iOS names.
         */
        fun getBlockingPlugins(
            successObject: ApiDiscoveryResult.Success
        ): List<KnownAuthenticationBlockingPlugin> =
            successObject.success.apiDetails.applicationPasswordBlockingPlugins()
    }
}
