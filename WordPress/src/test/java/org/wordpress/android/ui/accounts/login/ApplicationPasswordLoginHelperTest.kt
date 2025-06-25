package org.wordpress.android.ui.accounts.login

import com.sun.jna.Pointer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.BuildConfigWrapper
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.ParseUrlException
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiDetails
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEST_URL = "http://test.com"
private const val TEST_USER = "testuser"
private const val TEST_PASSWORD = "testpassword"

private const val TEST_URL_AUTH = "https://www.test.com/auth"
private const val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=callback://callback"

@ExperimentalCoroutinesApi
class ApplicationPasswordLoginHelperTest : BaseUnitTest() {
     @Mock
     lateinit var siteSqlUtils: SiteSqlUtils

     @Mock
     lateinit var uriLoginWrapper: ApplicationPasswordLoginHelper.UriLoginWrapper

     @Mock
     lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var wpLoginClient: WpLoginClient

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var wpApiDetails: WpApiDetails

    @Mock
    lateinit var authParsedUrl: ParsedUrl

    @Mock
    lateinit var emptyAuthParsedUrl: ParsedUrl


    private lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        applicationPasswordLoginHelper = ApplicationPasswordLoginHelper(
            testDispatcher(),
            siteSqlUtils,
            uriLoginWrapper,
            buildConfigWrapper,
            wpLoginClient,
            appLogWrapper
        )
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, TEST_USER, TEST_PASSWORD)
            )
        whenever(uriLoginWrapper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX")

        whenever(authParsedUrl.url()).thenReturn(TEST_URL_AUTH)
        whenever(emptyAuthParsedUrl.url()).thenReturn("")
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with empty data returns false`() = runTest {
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom("")
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with same data returns false`() = runTest {
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser&password=testpassword"
        applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null user name returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, null, TEST_PASSWORD)
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&password=testpasswordr"
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing user name returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, "", TEST_PASSWORD)
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&password=testpasswordr"
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null password returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, TEST_USER, null)
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser"
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing password returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, TEST_USER, "")
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser"
        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data stores credentials`() = runTest {
            val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser&password=testpassword"
            val siteModel = SiteModel().apply {
                url = TEST_URL
                apiRestUsernameEncrypted = TEST_USER
                apiRestPasswordEncrypted = TEST_PASSWORD
            }
        whenever(siteSqlUtils.getSites()).thenReturn(listOf(siteModel))

        val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)

        assertTrue(result)
        verify(siteSqlUtils).getSites()
        verify(siteSqlUtils).insertOrUpdateSite(eq(siteModel))
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data but not matching site does not store credentials`() =
        runTest {
            val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser&password=testpassword"
            whenever(siteSqlUtils.getSites()).thenReturn(listOf())

            val result = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(data)

            assertFalse(result)
            verify(siteSqlUtils).getSites()
            verify(siteSqlUtils, times(0)).insertOrUpdateSite(any())
        }

    @Test
    fun `appendParamsToRestAuthorizationUrl with null authorizationUrl returns empty string`() {
        val result = ApplicationPasswordLoginHelper.UriLoginWrapper().appendParamsToRestAuthorizationUrl(null)
        assertEquals("", result)
    }

    @Test
    fun `appendParamsToRestAuthorizationUrl with empty authorizationUrl returns empty string`() {
        val result = ApplicationPasswordLoginHelper.UriLoginWrapper().appendParamsToRestAuthorizationUrl("")
        assertEquals("", result)
    }

    @Test
    fun `given proper site, when api discovery is success, then return discovery url`() = runTest {
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.Success(
                    AutoDiscoveryAttemptSuccess(
                        ParsedUrl(Pointer.createConstant(1)),
                        ParsedUrl(Pointer.createConstant(1)),
                        wpApiDetails,
                        authParsedUrl
                    )
                )
            )

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
        whenever(wpLoginClient.apiDiscovery(eq(TEST_URL)))
            .thenReturn(
                ApiDiscoveryResult.Success(
                    AutoDiscoveryAttemptSuccess(
                        ParsedUrl(Pointer.createConstant(1)),
                        ParsedUrl(Pointer.createConstant(1)),
                        wpApiDetails,
                        emptyAuthParsedUrl
                    )
                )
            )
        whenever(uriLoginWrapper.appendParamsToRestAuthorizationUrl(any()))
            .thenReturn("")

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
        whenever(siteSqlUtils.getSites()).thenReturn(emptyList())

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(siteSqlUtils).getSites()
        verify(siteSqlUtils, times(0)).insertOrUpdateSite(any())
    }

    @Test
    fun `removeAllApplicationPasswordCredentials clears all password fields for single site`() = runTest {
        val site = SiteModel().apply {
            id = 1
            url = TEST_URL
            apiRestUsernamePlain = TEST_USER
            apiRestPasswordPlain = TEST_PASSWORD
            apiRestUsernameEncrypted = "encrypted_user"
            apiRestPasswordEncrypted = "encrypted_password"
            apiRestUsernameIV = "user_iv"
            apiRestPasswordIV = "password_iv"
        }
        whenever(siteSqlUtils.getSites()).thenReturn(listOf(site))

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(siteSqlUtils).getSites()
        verify(siteSqlUtils).insertOrUpdateSite(eq(site))

        // Verify all password fields are cleared
        assertEquals("", site.apiRestUsernamePlain)
        assertEquals("", site.apiRestPasswordPlain)
        assertEquals("", site.apiRestUsernameEncrypted)
        assertEquals("", site.apiRestPasswordEncrypted)
        assertEquals("", site.apiRestUsernameIV)
        assertEquals("", site.apiRestPasswordIV)
    }

    @Test
    fun `removeAllApplicationPasswordCredentials clears password fields for multiple sites`() = runTest {
        val site1 = SiteModel().apply {
            id = 1
            url = "http://site1.com"
            apiRestUsernamePlain = "user1"
            apiRestPasswordPlain = "password1"
            apiRestUsernameEncrypted = "encrypted_user1"
            apiRestPasswordEncrypted = "encrypted_password1"
            apiRestUsernameIV = "user_iv1"
            apiRestPasswordIV = "password_iv1"
        }
        val site2 = SiteModel().apply {
            id = 2
            url = "http://site2.com"
            apiRestUsernamePlain = "user2"
            apiRestPasswordPlain = "password2"
            apiRestUsernameEncrypted = "encrypted_user2"
            apiRestPasswordEncrypted = "encrypted_password2"
            apiRestUsernameIV = "user_iv2"
            apiRestPasswordIV = "password_iv2"
        }
        val site3 = SiteModel().apply {
            id = 3
            url = "http://site3.com"
            // This site has no credentials set
        }
        whenever(siteSqlUtils.getSites()).thenReturn(listOf(site1, site2, site3))

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(siteSqlUtils).getSites()
        verify(siteSqlUtils).insertOrUpdateSite(eq(site1))
        verify(siteSqlUtils).insertOrUpdateSite(eq(site2))
        verify(siteSqlUtils).insertOrUpdateSite(eq(site3))

        // Verify all password fields are cleared for all sites
        listOf(site1, site2, site3).forEach { site ->
            assertEquals("", site.apiRestUsernamePlain)
            assertEquals("", site.apiRestPasswordPlain)
            assertEquals("", site.apiRestUsernameEncrypted)
            assertEquals("", site.apiRestPasswordEncrypted)
            assertEquals("", site.apiRestUsernameIV)
            assertEquals("", site.apiRestPasswordIV)
        }
    }

    @Test
    fun `removeAllApplicationPasswordCredentials preserves other site fields`() = runTest {
        val site = SiteModel().apply {
            id = 1
            url = TEST_URL
            name = "Test Site"
            description = "Test Description"
            siteId = 12345L
            apiRestUsernamePlain = TEST_USER
            apiRestPasswordPlain = TEST_PASSWORD
        }
        whenever(siteSqlUtils.getSites()).thenReturn(listOf(site))

        applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()

        verify(siteSqlUtils).insertOrUpdateSite(eq(site))

        // Verify non-password fields are preserved
        assertEquals(1, site.id)
        assertEquals(TEST_URL, site.url)
        assertEquals("Test Site", site.name)
        assertEquals("Test Description", site.description)
        assertEquals(12345L, site.siteId)

        // Verify password fields are cleared
        assertEquals("", site.apiRestUsernamePlain)
        assertEquals("", site.apiRestPasswordPlain)
    }
 }
