package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.util.BuildConfigWrapper
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.ParseUrlException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            discoverSuccessWrapper
        )
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with empty data returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
            UriLogin("", "", "", "")
        )
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with same data returns false`() = runTest {
        applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null user name returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing user name returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null password returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing password returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with empty api root url returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)
        assertFalse(result)
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

        assertTrue(result)
        verify(siteStore).sites
        verify(dispatcherWrapper).updateApplicationPassword(eq(siteModel))
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data but not matching site does not store credentials`() =
        runTest {
            whenever(siteStore.sites).thenReturn(listOf())

            val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(testUriLogin)

            assertFalse(result)
            verify(siteStore).sites
            verify(dispatcherWrapper, times(0)).updateApplicationPassword(any())
            verify(dispatcherWrapper, times(0)).removeApplicationPassword(any())
        }

    @Test
    fun `appendParamsToRestAuthorizationUrl with null authorizationUrl returns empty string`() {
        val result = ApplicationPasswordLoginHelper.UriLoginWrapper(apiRootUrlCache, buildConfigWrapper)
            .appendParamsToRestAuthorizationUrl(null)
        assertEquals("", result)
    }

    @Test
    fun `appendParamsToRestAuthorizationUrl with empty authorizationUrl returns empty string`() {
        val result = ApplicationPasswordLoginHelper.UriLoginWrapper(apiRootUrlCache, buildConfigWrapper)
            .appendParamsToRestAuthorizationUrl("")
        assertEquals("", result)
    }

    @Test
    fun `given proper site, when api discovery is success, then return discovery url`() = runTest {
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(mock(), mock(), mock(), mock())
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

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL)

        assertEquals("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX", result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery fails, then return emtpy`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).doThrow(RuntimeException("API discovery failed"))

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL)

        assertEquals("", result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `given login scenario, when api discovery is empty, then return empty`() = runTest {
        val autoDiscoveryAttemptSuccess = AutoDiscoveryAttemptSuccess(mock(), mock(), mock(), mock())
        val apiDiscoveryResult = ApiDiscoveryResult.Success(autoDiscoveryAttemptSuccess)
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL))).thenReturn(apiDiscoveryResult)
        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL)

        assertEquals("", result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }


    @Test
    fun `given login scenario, when api discovery is failed, then return empty`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.FailureParseSiteUrl(
                    ParseUrlException.Generic("")
                )
            )

        val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(TEST_URL)

        assertEquals("", result)
        verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
    }

    @Test
    fun `removeAllApplicationPasswordCredentials with no sites completes without errors`() = runTest {
        whenever(siteStore.sites).thenReturn(emptyList())

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(siteStore).sites
        verify(dispatcherWrapper, times(0)).updateApplicationPassword(any())
        verify(dispatcherWrapper, times(0)).removeApplicationPassword(any())
    }

    @Test
    fun `removeAllApplicationPasswordCredentials clears all password fields for site with regular credentials`() =
        runTest {
            val site = SiteModel().apply {
                id = 1
                url = TEST_URL
                username = "regular_user"
                password = "regular_password"
                apiRestUsernamePlain = TEST_USER
                apiRestPasswordPlain = TEST_PASSWORD
                apiRestUsernameEncrypted = "encrypted_user"
                apiRestPasswordEncrypted = "encrypted_password"
                apiRestUsernameIV = "user_iv"
                apiRestPasswordIV = "password_iv"
            }
            whenever(siteStore.sites).thenReturn(listOf(site))

            applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

            verify(siteStore).sites
            verify(dispatcherWrapper).removeApplicationPassword(eq(site))

            // Verify all password fields are cleared
            assertEquals("", site.apiRestUsernamePlain)
            assertEquals("", site.apiRestPasswordPlain)
            assertEquals("", site.apiRestUsernameEncrypted)
            assertEquals("", site.apiRestPasswordEncrypted)
            assertEquals("", site.apiRestUsernameIV)
            assertEquals("", site.apiRestPasswordIV)
        }

    @Test
    fun `removeAllApplicationPasswordCredentials only resets sites with regular credentials`() = runTest {
        val siteWithRegularCredentials = SiteModel().apply {
            id = 1
            url = "http://site1.com"
            username = "regular_user1"
            password = "regular_password1"
            apiRestUsernamePlain = "user1"
            apiRestPasswordPlain = "password1"
            apiRestUsernameEncrypted = "encrypted_user1"
            apiRestPasswordEncrypted = "encrypted_password1"
            apiRestUsernameIV = "user_iv1"
            apiRestPasswordIV = "password_iv1"
        }
        val siteWithoutRegularCredentials = SiteModel().apply {
            id = 2
            url = "http://site2.com"
            username = ""
            password = ""
            apiRestUsernamePlain = "user2"
            apiRestPasswordPlain = "password2"
            apiRestUsernameEncrypted = "encrypted_user2"
            apiRestPasswordEncrypted = "encrypted_password2"
            apiRestUsernameIV = "user_iv2"
            apiRestPasswordIV = "password_iv2"
        }
        val siteWithNoAppPassword = SiteModel().apply {
            id = 3
            url = "http://site3.com"
            username = "regular_user3"
            password = "regular_password3"
            // This site has no Application Password credentials set
        }
        whenever(siteStore.sites).thenReturn(
            listOf(siteWithRegularCredentials, siteWithoutRegularCredentials, siteWithNoAppPassword)
        )

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(siteStore).sites
        // Only the site with regular credentials AND app password should be reset
        verify(dispatcherWrapper).removeApplicationPassword(eq(siteWithRegularCredentials))
        // Site without regular credentials should NOT be reset
        verify(dispatcherWrapper, times(0)).removeApplicationPassword(eq(siteWithoutRegularCredentials))
        // Site with no app password encrypted should NOT be reset
        verify(dispatcherWrapper, times(0)).removeApplicationPassword(eq(siteWithNoAppPassword))

        // Verify password fields are cleared only for site with regular credentials
        assertEquals("", siteWithRegularCredentials.apiRestUsernamePlain)
        assertEquals("", siteWithRegularCredentials.apiRestUsernameEncrypted)

        // Verify password fields are preserved for site without regular credentials
        assertEquals("user2", siteWithoutRegularCredentials.apiRestUsernamePlain)
        assertEquals("encrypted_user2", siteWithoutRegularCredentials.apiRestUsernameEncrypted)
    }

    @Test
    fun `removeAllApplicationPasswordCredentials preserves other site fields`() = runTest {
        val site = SiteModel().apply {
            id = 1
            url = TEST_URL
            name = "Test Site"
            description = "Test Description"
            siteId = 12345L
            username = "regular_user"
            password = "regular_password"
            apiRestUsernamePlain = TEST_USER
            apiRestPasswordPlain = TEST_PASSWORD
            apiRestUsernameEncrypted = "encrypted_user"
        }
        whenever(siteStore.sites).thenReturn(listOf(site))

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(dispatcherWrapper).removeApplicationPassword(eq(site))

        // Verify non-password fields are preserved
        assertEquals(1, site.id)
        assertEquals(TEST_URL, site.url)
        assertEquals("Test Site", site.name)
        assertEquals("Test Description", site.description)
        assertEquals(12345L, site.siteId)

        // Verify Application Password fields are cleared
        assertEquals("", site.apiRestUsernamePlain)
        assertEquals("", site.apiRestPasswordPlain)

        // Verify regular credentials are preserved
        assertEquals("regular_user", site.username)
        assertEquals("regular_password", site.password)
    }

    @Test
    fun `removeAllApplicationPasswordCredentials does not reset site with only username`() = runTest {
        val site = SiteModel().apply {
            id = 1
            url = TEST_URL
            username = "regular_user"
            password = "" // No password
            apiRestUsernameEncrypted = "encrypted_user"
        }
        whenever(siteStore.sites).thenReturn(listOf(site))

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(dispatcherWrapper, times(0)).removeApplicationPassword(any())
        assertEquals("encrypted_user", site.apiRestUsernameEncrypted)
    }

    @Test
    fun `removeAllApplicationPasswordCredentials does not reset site with only password`() = runTest {
        val site = SiteModel().apply {
            id = 1
            url = TEST_URL
            username = "" // No username
            password = "regular_password"
            apiRestUsernameEncrypted = "encrypted_user"
        }
        whenever(siteStore.sites).thenReturn(listOf(site))

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(dispatcherWrapper, times(0)).removeApplicationPassword(any())
        assertEquals("encrypted_user", site.apiRestUsernameEncrypted)
    }

    @Test
    fun `getResettableApplicationPasswordSitesCount returns count of sites with regular credentials`() {
        val siteWithRegularCredentials = SiteModel().apply {
            id = 1
            username = "user"
            password = "password"
            apiRestUsernameEncrypted = "encrypted"
        }
        val siteWithoutRegularCredentials = SiteModel().apply {
            id = 2
            username = ""
            password = ""
            apiRestUsernameEncrypted = "encrypted"
        }
        val siteWithNoAppPassword = SiteModel().apply {
            id = 3
            username = "user"
            password = "password"
            apiRestUsernameEncrypted = ""
        }
        whenever(siteStore.sites).thenReturn(
            listOf(siteWithRegularCredentials, siteWithoutRegularCredentials, siteWithNoAppPassword)
        )

        val count = applicationPasswordLoginHelper.getResettableApplicationPasswordSitesCount()

        assertEquals(1, count)
    }

    @Test
    fun `getResettableApplicationPasswordSitesCount returns zero when no sites have regular credentials`() {
        val site = SiteModel().apply {
            id = 1
            username = ""
            password = ""
            apiRestUsernameEncrypted = "encrypted"
        }
        whenever(siteStore.sites).thenReturn(listOf(site))

        val count = applicationPasswordLoginHelper.getResettableApplicationPasswordSitesCount()

        assertEquals(0, count)
    }

    @Test
    fun `getResettableApplicationPasswordSitesCount returns zero when no sites have app password`() {
        val site = SiteModel().apply {
            id = 1
            username = "user"
            password = "password"
            apiRestUsernameEncrypted = ""
        }
        whenever(siteStore.sites).thenReturn(listOf(site))

        val count = applicationPasswordLoginHelper.getResettableApplicationPasswordSitesCount()

        assertEquals(0, count)
    }
}
