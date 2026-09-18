package org.wordpress.android.ui.accounts.login

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.automattic.android.tracks.crashlogging.CrashLogging
import org.wordpress.android.R
import org.wordpress.android.util.DeviceUtils
import kotlinx.coroutines.CancellationException
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
import uniffi.wp_api.AutoDiscoveryAttemptFailure
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.applicationPasswordsUrl
import uniffi.wp_api.localizedDescription
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Named

private const val URL_TAG = "url"
private const val SUCCESS_TAG = "success"
private const val SOURCE_TAG = "source"
private const val ERROR_TAG = "error"
private const val IS_WPCOM_TAG = "is_wpcom"

private val SCHEME_PREFIX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

class ApplicationPasswordLoginHelper @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcherWrapper: DispatcherWrapper,
    private val siteStore: SiteStore,
    private val uriLoginWrapper: UriLoginWrapper,
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
        } catch (cancellation: CancellationException) {
            // Leaving the login screen or switching sites cancels discovery mid-flight. Counting
            // that as a failure would inflate exactly the rate these events exist to measure.
            throw cancellation
        } catch (throwable: Throwable) {
            handleAuthenticationDiscoveryError(
                siteUrl,
                source,
                throwable.message ?: throwable::class.simpleName.orEmpty(),
                unexpectedDiscoveryFailure(throwable),
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
                                siteUrl,
                                source,
                                "No application-passwords authentication URL advertised",
                                notSupportedFailure(),
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
                        siteUrl,
                        source,
                        // 0.8.0 replaced userFacingErrorMessage() with localizedDescription(), which
                        // returns a translated sentence. This message is shown to the user on the
                        // login screen, so the raw Throwable message (an internal debug dump of the
                        // discovery attempt) must not be used here.
                        discoverSuccessWrapper.localizedDescription(urlDiscoveryResult.failure),
                        urlDiscoveryResult.failure.toDiscoveryFailure(),
                    )
            }
        }

    private fun trackDiscoverySuccessful(siteUrl: String, source: DiscoverySource, isWpCom: Boolean) {
        analyticsTracker.track(
            Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL,
            mapOf(
                URL_TAG to maskUrl(siteUrl),
                SOURCE_TAG to source.value,
                // A WordPress.com site counts as a successful discovery even though it can't use
                // application passwords; without this it's indistinguishable from a real success.
                // A Boolean, like the login screen's own is_wpcom on the site-info event.
                IS_WPCOM_TAG to isWpCom,
            )
        )
    }

    private fun handleAuthenticationDiscoveryError(
        siteUrl: String,
        source: DiscoverySource,
        message: String,
        failure: DiscoveryFailure,
    ): DiscoveryResult {
        appLogWrapper.e(
            AppLog.T.API,
            "A_P: Error during API discovery for $siteUrl - $message (${failure.userFacing}, ${failure.reason})"
        )
        trackDiscoveryFailed(siteUrl, source, failure)
        if (source.isLoginAttempt) {
            trackLogin(siteUrl, source.value, success = false, error = "discovery_${failure.reason}")
        }
        return DiscoveryResult.Failed(message, failure.userFacing)
    }

    private fun trackDiscoveryFailed(siteUrl: String, source: DiscoverySource, failure: DiscoveryFailure) {
        analyticsTracker.track(
            Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED,
            failure.props + mapOf(URL_TAG to maskUrl(siteUrl), SOURCE_TAG to source.value)
        )
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
                trackLogin(effectiveUrlLogin.siteUrl, creationSource, success = true)
                appLogWrapper.d(
                    AppLog.T.DB,
                    "A_P: Saved application password credentials for: ${effectiveUrlLogin.siteUrl}"
                )
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
            when (val result = wpLoginClient.apiDiscovery(siteUrl)) {
                is ApiDiscoveryResult.Success -> {
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
                }
                is ApiDiscoveryResult.Failure -> {
                    // The login goes on to fail as bad_data; this row is what says why.
                    trackDiscoveryFailed(
                        siteUrl,
                        DiscoverySource.API_ROOT_RECOVERY,
                        result.failure.toDiscoveryFailure(),
                    )
                    null
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            appLogWrapper.e(
                AppLog.T.API,
                "A_P: Fallback discovery failed for $siteUrl - ${throwable.message}"
            )
            trackDiscoveryFailed(siteUrl, DiscoverySource.API_ROOT_RECOVERY, unexpectedDiscoveryFailure(throwable))
            null
        }
    }

    fun trackStoringFailed(
        siteUrl: String?,
        reason: String,
        creationSource: String = ""
    ) {
        analyticsTracker.track(
            Stat.APPLICATION_PASSWORD_STORING_FAILED,
            mapOf(URL_TAG to maskUrl(siteUrl.orEmpty()), REASON_TAG to reason)
        )
        trackCreated(creationSource, success = false, error = reason)
        // Every post-callback failure funnels through here, user rejection included, so this is
        // the one place that gives the login event its failure rows.
        trackLogin(siteUrl, creationSource, success = false, error = reason)
    }

    fun trackCreated(
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

    /**
     * The outcome of an application-password login. Two paths complete one: credentials stored
     * against a site we already had (here), and a site fetched for the first time (the login
     * ViewModel). [source] is the flow's `application_password_created` source, or empty when unknown.
     *
     * `success` is reported as `"1"`/`"0"` rather than `"true"`/`"false"` so that this event matches
     * its iOS counterpart and a cross-platform query doesn't have to handle both encodings.
     */
    fun trackLogin(siteUrl: String?, source: String, success: Boolean, error: String? = null) {
        analyticsTracker.track(
            Stat.APPLICATION_PASSWORD_LOGIN,
            buildMap {
                put(URL_TAG, maskUrl(siteUrl.orEmpty()))
                put(SUCCESS_TAG, if (success) "1" else "0")
                if (source.isNotEmpty()) put(SOURCE_TAG, source)
                if (error != null) put(ERROR_TAG, error)
            }
        )
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

    /**
     * A site identifier for analytics: the host, lowercased and with its domain masked, plus any
     * non-default port. Scheme, `www.`, a trailing dot, path, userinfo, query and fragment are all
     * dropped, and an internationalised host is converted to its punycode form, so the address typed
     * on the login screen and the `site_url` the callback carries collapse to one value per site, and
     * nothing typed into the field ships unmasked. A host that isn't a domain name (an IP address,
     * a single label, anything that doesn't parse) yields "" rather than the raw input.
     *
     * Parsed with okhttp's HttpUrl rather than UrlUtils.getHost, which is backed by android.net.Uri
     * and throws under the plain-JUnit runner this class is tested with.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun maskUrl(url: String): String {
        val trimmed = url.trim()
        val parsed = (if (SCHEME_PREFIX.containsMatchIn(trimmed)) trimmed else "https://$trimmed").toHttpUrlOrNull()
        val host = parsed?.host?.removePrefix("www.")?.trimEnd('.').orEmpty()
        val dotIndex = host.lastIndexOf('.')
        val isDomainName = dotIndex > 0 && ':' !in host && !host.substring(dotIndex + 1).all { it.isDigit() }
        if (parsed == null || !isDomainName) return ""
        val domain = host.substring(0, dotIndex)
        val maskedDomain = when {
            domain.length <= 2 -> "x".repeat(domain.length)
            else -> domain.first() +
                "x".repeat(domain.length - 2) +
                domain.last()
        }
        val port = if (parsed.port == HttpUrl.defaultPort(parsed.scheme)) "" else ":${parsed.port}"
        return maskedDomain + host.substring(dotIndex) + port
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
         * The library's translated description of a failure, shown on the login screen. Wrapped
         * because it's a native call, which the helper's plain-JUnit tests can't make.
         */
        fun localizedDescription(failure: AutoDiscoveryAttemptFailure): String = failure.localizedDescription()
    }
}
