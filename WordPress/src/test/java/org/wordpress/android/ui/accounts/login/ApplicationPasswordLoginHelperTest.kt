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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
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
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.OAuth2Endpoints
import uniffi.wp_api.AutoDiscoveryAttemptFailure
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
            analyticsTracker,
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
        stubDiscovery(authUrl = TEST_URL_AUTH)

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
        stubDiscovery(authUrl = null)

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
    fun `given login discovery fails, then both events carry the reason and the login event fails`() = runTest {
        stubDiscovery(authUrl = null)

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)

        assertEquals(
            mapOf("reason" to "app_passwords_not_supported", "url" to "txxt.com", "source" to "login"),
            trackedProps(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        )
        assertEquals(
            mapOf("url" to "txxt.com", "success" to "false", "error" to "discovery_app_passwords_not_supported"),
            trackedProps(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN)
        )
        assertEquals(
            ApplicationPasswordLoginHelper.DiscoveryResult.Failed(
                "No application-passwords authentication URL advertised",
                ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason.NotSupported,
            ),
            result
        )
    }

    @Test
    fun `given card discovery fails, then no login event fires`() = runTest {
        stubDiscovery(authUrl = null)

        applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.MY_SITE_CARD)

        verify(analyticsTracker).track(eq(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED), any<Map<String, *>>())
        verify(analyticsTracker, never()).track(eq(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN), any<Map<String, *>>())
    }

    @Test
    fun `given discovery throws, then autodiscovery_failed carries the exception class`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(IllegalStateException("boom"))

        applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.REAUTH_DIALOG)

        assertEquals(
            mapOf(
                "reason" to "exception",
                "error_code" to "IllegalStateException",
                "url" to "txxt.com",
                "source" to "reauth",
            ),
            trackedProps(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        )
    }

    @Test
    fun `given discovery is cancelled, then it propagates and nothing is tracked`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(CancellationException("left the screen"))

        assertFailsWith<CancellationException> {
            applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.LOGIN)
        }

        verify(analyticsTracker, never()).track(any(), any<Map<String, *>>())
    }

    @Test
    fun `given discovery succeeds, then autodiscovery_successful carries url source and is_wpcom`() = runTest {
        stubDiscovery(authUrl = TEST_URL_AUTH)

        applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL, DiscoverySource.MY_SITE_CARD)

        assertEquals(
            mapOf("url" to "txxt.com", "source" to "my_site_card", "is_wpcom" to "false"),
            trackedProps(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
        )
    }

    @Test
    fun `given storing fails, then the login event fires with the storing reason`() {
        applicationPasswordLoginHelper.trackStoringFailed(TEST_URL, "user_rejected", "login")

        assertEquals(
            mapOf("url" to "txxt.com", "success" to "false", "error" to "user_rejected"),
            trackedProps(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN)
        )
    }

    @Test
    fun `given storing succeeds, then the login event fires with success`() = runTest {
        val site = SiteModel().apply { url = TEST_URL }
        whenever(siteStore.sites).thenReturn(listOf(site))

        applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)

        assertEquals(
            mapOf("url" to "txxt.com", "success" to "true"),
            trackedProps(Stat.WP_ANDROID_APPLICATION_PASSWORD_LOGIN)
        )
    }

    @Test
    fun `maskUrl collapses the typed and callback forms of one site to one value`() {
        // The login screen passes the address as typed; the callback carries a normalised site_url.
        // Scheme, www and trailing slash must not split them into two sites.
        assertEquals("mxxxxe.com", applicationPasswordLoginHelper.maskUrl("mysite.com"))
        assertEquals("mxxxxe.com", applicationPasswordLoginHelper.maskUrl("www.mysite.com/"))
        assertEquals("mxxxxe.com", applicationPasswordLoginHelper.maskUrl("http://mysite.com"))
        assertEquals("mxxxxe.com/blog", applicationPasswordLoginHelper.maskUrl("mysite.com/blog"))
        assertEquals("mxxxxe.com//blog", applicationPasswordLoginHelper.maskUrl("mysite.com//blog"))
    }

    @Test
    fun `maskUrl drops userinfo query and fragment`() {
        assertEquals("exxxxxe.com", applicationPasswordLoginHelper.maskUrl("nick@example.com"))
        assertEquals("exxxxxe.com", applicationPasswordLoginHelper.maskUrl("https://user:pass@example.com"))
        assertEquals("txxt.com", applicationPasswordLoginHelper.maskUrl("test.com:x@test.com"))
        assertEquals("exxxxxe.com/wp-admin", applicationPasswordLoginHelper.maskUrl("example.com/wp-admin?next=x#y"))
    }

    @Test
    fun `maskUrl drops a value whose host it cannot mask`() {
        assertEquals("", applicationPasswordLoginHelper.maskUrl("https://localhost"))
        assertEquals("", applicationPasswordLoginHelper.maskUrl("https://my_site.com"))
        assertEquals("", applicationPasswordLoginHelper.maskUrl("not a url"))
    }

    @Test
    fun `maskUrl leaves an empty value alone`() {
        assertEquals("", applicationPasswordLoginHelper.maskUrl(""))
    }

    @Test
    fun `maskUrl with standard domain masks middle characters`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://example.com")
        assertEquals("exxxxxe.com", result)
    }

    @Test
    fun `maskUrl with port preserves port`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://test.com:8080")
        assertEquals("txxt.com:8080", result)
    }

    @Test
    fun `maskUrl with dot in path masks only host`() {
        val result = applicationPasswordLoginHelper
            .maskUrl("https://example.com/wp-content/image.jpg")
        assertEquals("exxxxxe.com/wp-content/image.jpg", result)
    }

    @Test
    fun `maskUrl with three char domain masks middle character`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://abc.com")
        assertEquals("axc.com", result)
    }

    @Test
    fun `maskUrl with single char domain replaces it with x`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://a.com")
        assertEquals("x.com", result)
    }

    @Test
    fun `maskUrl with two char domain replaces both with x`() {
        val result = applicationPasswordLoginHelper.maskUrl("https://ab.com")
        assertEquals("xx.com", result)
    }

    private fun trackedProps(stat: Stat): Map<String, *> {
        val captor = argumentCaptor<Map<String, *>>()
        verify(analyticsTracker).track(eq(stat), captor.capture())
        return captor.firstValue
    }

    /**
     * A successful rs discovery whose site advertises [authUrl], or no application-passwords URL at
     * all when null. The rs failure variants themselves are mapped in DiscoveryFailureAnalyticsTest;
     * through the helper they can't be exercised on the JVM, because localizedDescription() is a
     * native call and throws UnsatisfiedLinkError, so the failure tests here use the null case.
     */
    private suspend fun stubDiscovery(authUrl: String?) {
        val apiDiscoveryResult = ApiDiscoveryResult.Success(
            AutoDiscoveryAttemptSuccess(
                mock(), mock(), mock(), DiscoveredAuthenticationMechanism.ApplicationPasswords(mock())
            )
        )
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(apiDiscoveryResult)
        whenever(discoverSuccessWrapper.getApplicationPasswordsAuthenticationUrl(eq(apiDiscoveryResult)))
            .thenReturn(authUrl)
        if (authUrl != null) {
            whenever(discoverSuccessWrapper.getApiRootUrl(eq(apiDiscoveryResult))).thenReturn(TEST_API_ROOT_URL)
            whenever(uriLoginWrapper.appendParamsToRestAuthorizationUrl(any()))
                .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")
        }
    }
}
