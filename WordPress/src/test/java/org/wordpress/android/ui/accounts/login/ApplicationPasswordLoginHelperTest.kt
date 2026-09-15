package org.wordpress.android.ui.accounts.login

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.StoreCredentialsResult
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptFailure
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.KnownAuthenticationBlockingPlugin
import uniffi.wp_api.OAuth2Endpoints
import uniffi.wp_api.ParseUrlException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private const val TEST_URL = "http://test.com"
private const val TEST_USER = "testuser"
private const val TEST_PASSWORD = "testpassword"
private const val TEST_API_ROOT_URL = "http://test.com/json"

private const val TEST_URL_AUTH = "https://www.test.com/auth"
private const val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=callback://callback"

@ExperimentalCoroutinesApi
class ApplicationPasswordLoginHelperTest : BaseUnitTest() {
    val testUriLogin = UriLogin(TEST_URL, TEST_USER, TEST_PASSWORD, TEST_API_ROOT_URL)
     @Mock
     lateinit var context: Context

     @Mock
     lateinit var dispatcherWrapper: ApplicationPasswordLoginHelper.DispatcherWrapper

     @Mock
     lateinit var siteStore: SiteStore

     @Mock
     lateinit var uriLoginWrapper: ApplicationPasswordLoginHelper.UriLoginWrapper

     @Mock
     lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var wpLoginClient: WpLoginClient

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var discoverSuccessWrapper: ApplicationPasswordLoginHelper.DiscoverSuccessWrapper

    @Mock
    lateinit var apiRootUrlCache: ApiRootUrlCache

    @Mock
    lateinit var crashLogging: CrashLogging

    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        applicationPasswordLoginHelper = ApplicationPasswordLoginHelper(
            testDispatcher(),
            dispatcherWrapper,
            siteStore,
            uriLoginWrapper,
            buildConfigWrapper,
            wpLoginClient,
            appLogWrapper,
            apiRootUrlCache,
            discoverSuccessWrapper,
            crashLogging,
            wpApiClientProvider,
            analyticsTracker
        )
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with empty data returns BadData`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
            UriLogin("", "", "", "")
        )
        assertIs<StoreCredentialsResult.BadData>(result)
        verify(wpApiClientProvider, times(0)).clearSelfHostedClient(any())
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with same data returns BadData`() = runTest {
        whenever(siteStore.sites).thenReturn(listOf(
            SiteModel().apply { url = TEST_URL }
        ))
        applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertIs<StoreCredentialsResult.BadData>(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null user name returns BadData`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
            UriLogin(TEST_URL, null, TEST_PASSWORD, TEST_API_ROOT_URL)
        )
        assertIs<StoreCredentialsResult.BadData>(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing user name returns BadData`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
            UriLogin(TEST_URL, "", TEST_PASSWORD, TEST_API_ROOT_URL)
        )
        assertIs<StoreCredentialsResult.BadData>(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null password returns BadData`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
            UriLogin(TEST_URL, TEST_USER, null, TEST_API_ROOT_URL)
        )
        assertIs<StoreCredentialsResult.BadData>(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing password returns BadData`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
            UriLogin(TEST_URL, TEST_USER, "", TEST_API_ROOT_URL)
        )
        assertIs<StoreCredentialsResult.BadData>(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom when apiRootUrl null and fallback discovery fails returns BadData`() =
        runTest {
            whenever(wpLoginClient.apiDiscovery(any())).thenReturn(
                ApiDiscoveryResult.Failure(AutoDiscoveryAttemptFailure.ParseSiteUrl(ParseUrlException.Generic("")))
            )

            val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
                UriLogin(TEST_URL, TEST_USER, TEST_PASSWORD, null)
            )

            assertIs<StoreCredentialsResult.BadData>(result)
        }

    @Test
    fun `storeApplicationPasswordCredentialsFrom recovers missing apiRootUrl via fallback discovery`() = runTest {
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(
            mock(), mock(), mock(), DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
        )
        val apiDiscoveryResult = ApiDiscoveryResult.Success(autoDiscoveryAttemptSuccess)
        whenever(wpLoginClient.apiDiscovery(any())).thenReturn(apiDiscoveryResult)
        whenever(discoverSuccessWrapper.getApiRootUrl(eq(apiDiscoveryResult))).thenReturn(TEST_API_ROOT_URL)
        val siteModel = SiteModel().apply { url = TEST_URL }
        whenever(siteStore.sites).thenReturn(listOf(siteModel))

        val loginWithoutApiRoot = UriLogin(TEST_URL, TEST_USER, TEST_PASSWORD, null)
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(loginWithoutApiRoot)

        assertIs<StoreCredentialsResult.Success>(result)
        verify(wpLoginClient, times(1)).apiDiscovery(any())
        verify(dispatcherWrapper).updateApplicationPassword(eq(siteModel))
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom returns SiteNotFound carrying the recovered apiRootUrl`() = runTest {
        // Given — apiRootUrl is missing and recovered via discovery, but the site is not found
        // locally. The recovered value must be carried on SiteNotFound so the caller can fetch.
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(
            mock(), mock(), mock(), DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
        )
        val apiDiscoveryResult = ApiDiscoveryResult.Success(autoDiscoveryAttemptSuccess)
        whenever(wpLoginClient.apiDiscovery(any())).thenReturn(apiDiscoveryResult)
        whenever(discoverSuccessWrapper.getApiRootUrl(eq(apiDiscoveryResult))).thenReturn(TEST_API_ROOT_URL)
        whenever(siteStore.sites).thenReturn(listOf())

        val loginWithoutApiRoot = UriLogin(TEST_URL, TEST_USER, TEST_PASSWORD, null)
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(loginWithoutApiRoot)

        assertIs<StoreCredentialsResult.SiteNotFound>(result)
        assertEquals(TEST_API_ROOT_URL, result.urlLogin.apiRootUrl)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom does not run discovery when apiRootUrl is present`() = runTest {
        val siteModel = SiteModel().apply { url = TEST_URL }
        whenever(siteStore.sites).thenReturn(listOf(siteModel))

        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)

        assertIs<StoreCredentialsResult.Success>(result)
        verify(wpLoginClient, times(0)).apiDiscovery(any())
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data stores credentials`() = runTest {
            val siteModel = SiteModel().apply {
                url = TEST_URL
                apiRestUsernameEncrypted = TEST_USER
                apiRestPasswordEncrypted = TEST_PASSWORD
            }
        whenever(siteStore.sites).thenReturn(listOf(siteModel))

        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)

        assertIs<StoreCredentialsResult.Success>(result)
        verify(siteStore).sites
        verify(dispatcherWrapper).updateApplicationPassword(eq(siteModel))
        verify(wpApiClientProvider).clearSelfHostedClient(eq(siteModel.id))
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data but not matching site returns SiteNotFound`() =
        runTest {
            whenever(siteStore.sites).thenReturn(listOf())

            val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)

            assertIs<StoreCredentialsResult.SiteNotFound>(result)
            verify(siteStore).sites
            verify(dispatcherWrapper, times(0)).updateApplicationPassword(any())
            verify(wpApiClientProvider, times(0)).clearSelfHostedClient(any())
        }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with empty url does not match site with empty url`() =
        runTest {
            val siteWithEmptyUrl = SiteModel().apply {
                url = ""
            }
            whenever(siteStore.sites).thenReturn(listOf(siteWithEmptyUrl))
            val emptyUrlLogin = UriLogin(
                "", TEST_USER, TEST_PASSWORD, TEST_API_ROOT_URL
            )

            val result = applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(emptyUrlLogin)

            assertIs<StoreCredentialsResult.SiteNotFound>(result)
            verify(dispatcherWrapper, times(0)).updateApplicationPassword(any())
        }

    @Test
    fun `storeApplicationPasswordCredentialsFrom matches site with different scheme`() =
        runTest {
            val siteModel = SiteModel().apply {
                url = "http://test.com"
            }
            whenever(siteStore.sites).thenReturn(listOf(siteModel))
            val httpsLogin = UriLogin(
                "https://test.com", TEST_USER, TEST_PASSWORD, TEST_API_ROOT_URL
            )

            val result = applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(httpsLogin)

            assertIs<StoreCredentialsResult.Success>(result)
            verify(dispatcherWrapper).updateApplicationPassword(eq(siteModel))
        }

    @Test
    fun `storeApplicationPasswordCredentialsFrom matches site with www mismatch`() =
        runTest {
            val siteModel = SiteModel().apply {
                url = "https://www.test.com"
            }
            whenever(siteStore.sites).thenReturn(listOf(siteModel))
            val noWwwLogin = UriLogin(
                "https://test.com", TEST_USER, TEST_PASSWORD, TEST_API_ROOT_URL
            )

            val result = applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(noWwwLogin)

            assertIs<StoreCredentialsResult.Success>(result)
            verify(dispatcherWrapper).updateApplicationPassword(eq(siteModel))
        }

    @Test
    fun `storeApplicationPasswordCredentialsFrom matches site with scheme and www mismatch`() =
        runTest {
            val siteModel = SiteModel().apply {
                url = "http://www.test.com"
            }
            whenever(siteStore.sites).thenReturn(listOf(siteModel))
            val httpsNoWwwLogin = UriLogin(
                "https://test.com", TEST_USER, TEST_PASSWORD, TEST_API_ROOT_URL
            )

            val result = applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(httpsNoWwwLogin)

            assertIs<StoreCredentialsResult.Success>(result)
            verify(dispatcherWrapper).updateApplicationPassword(eq(siteModel))
        }

    @Test
    fun `storeApplicationPasswordCredentialsFrom prefers exact match over fallback`() =
        runTest {
            val exactSite = SiteModel().apply {
                url = TEST_URL
                id = 1
            }
            val fallbackSite = SiteModel().apply {
                url = "https://test.com"
                id = 2
            }
            whenever(siteStore.sites)
                .thenReturn(listOf(fallbackSite, exactSite))

            val result = applicationPasswordLoginHelper
                .storeApplicationPasswordCredentialsFrom(testUriLogin)

            assertIs<StoreCredentialsResult.Success>(result)
            verify(dispatcherWrapper).updateApplicationPassword(eq(exactSite))
        }

    @Test
    fun `appendParamsToRestAuthorizationUrl with null authorizationUrl returns empty string`() {
        val result = ApplicationPasswordLoginHelper.UriLoginWrapper(context, apiRootUrlCache, buildConfigWrapper)
            .appendParamsToRestAuthorizationUrl(null)
        assertEquals("", result)
    }

    @Test
    fun `appendParamsToRestAuthorizationUrl with empty authorizationUrl returns empty string`() {
        val result = ApplicationPasswordLoginHelper.UriLoginWrapper(context, apiRootUrlCache, buildConfigWrapper)
            .appendParamsToRestAuthorizationUrl("")
        assertEquals("", result)
    }

    @Test
    fun `given proper site, when api discovery is success, then return discovery url`() = runTest {
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(
            mock(), mock(), mock(), DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
        )
        whenever(uriLoginWrapper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")
        val apiDiscoveryResult = ApiDiscoveryResult.Success(
            autoDiscoveryAttemptSuccess
        )
        whenever(discoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl(eq(apiDiscoveryResult)))
            .thenReturn(TEST_URL_AUTH)
        whenever(discoverSuccessWrapper.getApiRootUrl(eq(apiDiscoveryResult)))
            .thenReturn(TEST_API_ROOT_URL)
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(apiDiscoveryResult)

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertEquals(
            ApplicationPasswordLoginHelper.DiscoveryResult.Authorized("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX"),
            result
        )
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given a WP_com site, when api discovery returns OAuth2, then return WpComSite`() = runTest {
        val oAuth2 = DiscoveredAuthenticationMechanism.OAuth2(
            OAuth2Endpoints(authorizationUrl = TEST_URL, tokenUrl = TEST_URL)
        )
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(mock(), mock(), mock(), oAuth2)
        val apiDiscoveryResult = ApiDiscoveryResult.Success(autoDiscoveryAttemptSuccess)
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(apiDiscoveryResult)
        whenever(discoverSuccessWrapper.isWpComSite(eq(apiDiscoveryResult))).thenReturn(true)

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertEquals(ApplicationPasswordLoginHelper.DiscoveryResult.WpComSite, result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery throws, then return Failed`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(RuntimeException("API discovery failed"))

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertTrue(result is ApplicationPasswordLoginHelper.DiscoveryResult.Failed)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given Success result but auth URL extraction fails, then return Failed`() = runTest {
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(
            mock(), mock(), mock(), DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
        )
        val apiDiscoveryResult = ApiDiscoveryResult.Success(autoDiscoveryAttemptSuccess)
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(apiDiscoveryResult)
        // DiscoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl returns null when the site
        // advertises no application-passwords URL, which surfaces as Failed(NotSupported).

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertTrue(result is ApplicationPasswordLoginHelper.DiscoveryResult.Failed)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }


    @Test
    fun `given login scenario, when api discovery is failed, then return Failed with wordpress-rs message`() =
        runTest {
            whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
                .thenReturn(
                    ApiDiscoveryResult.Failure(
                        AutoDiscoveryAttemptFailure.ParseSiteUrl(ParseUrlException.Generic(""))
                    )
                )

            val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

            assertTrue(result is ApplicationPasswordLoginHelper.DiscoveryResult.Failed)
            verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
        }

    @Test
    fun `maskUrl with no dot masks the whole value`() {
        // Previously passed through. A single-label host is as likely to be an internal name as it
        // is to be localhost, and the prop only exists to count distinct sites.
        val result = applicationPasswordLoginHelper.maskUrl("https://localhost")
        assertEquals("masked", result)
    }

    @Test
    fun `maskUrl masks a scheme-less address, which is what the login screen passes`() {
        // Regression: URI reports no host without a scheme, so this used to return the raw domain.
        // The scheme is normalised too, so one site doesn't count twice on whether it was typed.
        assertEquals("https://mxxxxxxxxxxxg.com", applicationPasswordLoginHelper.maskUrl("myprivateblog.com"))
        assertEquals("https://mxxxxe.com/xxxx", applicationPasswordLoginHelper.maskUrl("mysite.com/blog"))
        assertEquals(
            "https://wxxxxxxxxxxxxxxxg.com",
            applicationPasswordLoginHelper.maskUrl("www.myprivateblog.com")
        )
        assertEquals(
            "https://mxxxxxxxxxxxg.com:8080",
            applicationPasswordLoginHelper.maskUrl("myprivateblog.com:8080")
        )
    }

    @Test
    fun `maskUrl masks a value it cannot parse rather than passing it through`() {
        assertEquals("masked", applicationPasswordLoginHelper.maskUrl("not a url at all"))
    }

    @Test
    fun `maskUrl leaves an empty value alone`() {
        // user_rejected and empty_raw_data both report an empty URL; a placeholder would read as a site.
        assertEquals("", applicationPasswordLoginHelper.maskUrl(""))
    }

    @Test
    fun `maskUrl with standard domain masks middle characters`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://example.com")
        assertEquals("https://exxxxxe.com", result)
    }

    @Test
    fun `maskUrl with port preserves port`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://test.com:8080")
        assertEquals("https://txxt.com:8080", result)
    }

    @Test
    fun `maskUrl masks the path rather than passing it through`() {
        // Subdirectory installs make the path part of a site's identity, so it is masked per
        // segment rather than dropped — two of them must not collapse into one count.
        val result = applicationPasswordLoginHelper
            .maskUrl("https://example.com/wp-content/image.jpg")
        assertEquals("https://exxxxxe.com/xxxxxxxxxx/xxxxxxxxx", result)
    }

    @Test
    fun `maskUrl keeps distinct subdirectory installs distinct`() {
        val blog = applicationPasswordLoginHelper.maskUrl("https://example.com/blog")
        val shop = applicationPasswordLoginHelper.maskUrl("https://example.com/shopping")

        assertEquals("https://exxxxxe.com/xxxx", blog)
        assertEquals("https://exxxxxe.com/xxxxxxxx", shop)
    }

    @Test
    fun `maskUrl drops the query, which identifies no site`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://example.com?token=secret")
        assertEquals("https://exxxxxe.com", result)
    }

    @Test
    fun `maskUrl with three char domain masks middle character`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://abc.com")
        assertEquals("https://axc.com", result)
    }

    @Test
    fun `maskUrl with single char domain replaces it with x`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://a.com")
        assertEquals("https://x.com", result)
    }

    @Test
    fun `maskUrl with two char domain replaces both with x`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://ab.com")
        assertEquals("https://xx.com", result)
    }

    @Test
    fun `given a failed discovery, when tracking, then both the reason and the login failure are sent`() = runTest {
        givenDiscoveryWithNoAuthenticationUrl()

        applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertEquals(
            mapOf(
                "reason" to "no_app_passwords_url",
                "url" to "http://txxt.com",
                "source" to "login",
            ),
            trackedProperties(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        )
        assertEquals(
            mapOf(
                "url" to "http://txxt.com",
                "success" to "false",
                "error" to "discovery_no_app_passwords_url",
            ),
            trackedProperties(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN)
        )
    }

    @Test
    fun `given a site with a blocking plugin, when no auth URL is advertised, then the plugin is named`() =
        runTest {
            val apiDiscoveryResult = givenDiscoveryWithNoAuthenticationUrl()
            whenever(discoverSuccessWrapper.getBlockingPlugins(eq(apiDiscoveryResult)))
                .thenReturn(listOf(KnownAuthenticationBlockingPlugin("Wordfence", "wordfence/v1", "")))

            applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

            val properties = trackedProperties(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
            assertEquals("app_passwords_blocked_by_plugin", properties["reason"])
            assertEquals("Wordfence", properties["plugin"])
        }

    @Test
    fun `given a WP_com site, when discovery succeeds, then the success is tagged is_wpcom`() = runTest {
        val oAuth2 = DiscoveredAuthenticationMechanism.OAuth2(
            OAuth2Endpoints(authorizationUrl = TEST_URL, tokenUrl = TEST_URL)
        )
        val apiDiscoveryResult = ApiDiscoveryResult.Success(
            AutoDiscoveryAttemptSuccess(mock(), mock(), mock(), oAuth2)
        )
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(apiDiscoveryResult)
        whenever(discoverSuccessWrapper.isWpComSite(eq(apiDiscoveryResult))).thenReturn(true)

        applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertEquals(
            mapOf("url" to "http://txxt.com", "source" to "login", "is_wpcom" to "true"),
            trackedProperties(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
        )
    }

    @Test
    fun `given the My Site card, when discovery fails, then no login failure is tracked`() = runTest {
        givenDiscoveryWithNoAuthenticationUrl()

        applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.MY_SITE_CARD)

        verify(analyticsTracker, times(0))
            .track(eq(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN), any<Map<String, Any?>>())
    }

    @Test
    fun `given a repeat callback, when storing, then no failure contradicts the success already sent`() = runTest {
        // Rotating the device re-runs setupSite() from onCreate against the retained ViewModel,
        // which still holds this helper — so the same callback arrives twice.
        val site = SiteModel().apply { id = 1; url = TEST_URL }
        whenever(siteStore.sites).thenReturn(listOf(site))
        applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin, "login")
        clearInvocations(analyticsTracker)

        val result = applicationPasswordLoginHelper
            .storeApplicationPasswordCredentialsFrom(testUriLogin, "login")

        assertIs<StoreCredentialsResult.BadData>(result)
        verify(analyticsTracker, times(0)).track(any(), any<Map<String, Any?>>())
    }

    @Test
    fun `given storing fails, when tracking, then the login event fires with the same reason`() {
        applicationPasswordLoginHelper.trackStoringFailed(TEST_URL, "user_rejected", "login")

        assertEquals(
            mapOf(
                "url" to "http://txxt.com",
                "success" to "false",
                "error" to "user_rejected",
            ),
            trackedProperties(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN)
        )
    }

    @Test
    fun `given a cancelled discovery, when it unwinds, then it is not reported as a failure`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(CancellationException("cancelled"))

        assertFailsWith<CancellationException> {
            applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)
        }

        verify(analyticsTracker, times(0)).track(any(), any<Map<String, Any?>>())
    }

    @Test
    fun `given a first-time site, when the login completes, then the login event reports success`() {
        applicationPasswordLoginHelper.trackLoginSuccessful(TEST_URL)

        assertEquals(
            mapOf("url" to "http://txxt.com", "success" to "true"),
            trackedProperties(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN)
        )
    }

    /**
     * Discovery that reaches and parses the API root but finds no application-passwords URL.
     *
     * Failure-classification tests use this rather than an [ApiDiscoveryResult.Failure]: the helper
     * asks the failure for its user-facing message via `localizedDescription()`, an FFI call that
     * throws NoClassDefFoundError without the native library, so every such test would only ever
     * exercise the catch-all. Classification of the library's own failure types is covered by
     * DiscoveryAnalyticsTest, which reads their fields rather than calling into the library.
     */
    private suspend fun givenDiscoveryWithNoAuthenticationUrl(): ApiDiscoveryResult.Success {
        val result = ApiDiscoveryResult.Success(
            AutoDiscoveryAttemptSuccess(
                mock(), mock(), mock(), DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
            )
        )
        whenever(discoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl(eq(result))).thenReturn(null)
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(result)
        return result
    }

    private fun trackedProperties(stat: Stat): Map<String, Any?> {
        val captor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsTracker).track(eq(stat), captor.capture())
        return captor.firstValue
    }
}
