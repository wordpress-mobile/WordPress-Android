package org.wordpress.android.ui.accounts.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.encryption.EncryptionUtils
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEST_URL = "http://test.com"
private const val TEST_USER = "testuser"
private const val TEST_PASSWORD = "testpassword"
private const val ENCRYPTED = "encrypted"
private const val IV = "iv"

@ExperimentalCoroutinesApi
class ApplicationPasswordLoginHelperTest : BaseUnitTest() {
     @Mock
     lateinit var siteSqlUtils: SiteSqlUtils

     @Mock
     lateinit var uriLoginWrapper: ApplicationPasswordLoginHelper.UriLoginWrapper

     @Mock
     lateinit var buildConfigWrapper: BuildConfigWrapper

     @Mock
     lateinit var encryptionUtils: EncryptionUtils

    private lateinit var helper: ApplicationPasswordLoginHelper

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        helper = ApplicationPasswordLoginHelper(
            testDispatcher(),
            siteSqlUtils,
            uriLoginWrapper,
            buildConfigWrapper,
            encryptionUtils
        )
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, TEST_USER, TEST_PASSWORD)
            )
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with empty data returns false`() = runTest {
        val result = helper.storeApplicationPasswordCredentialsFrom("")
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with same data returns false`() = runTest {
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser&password=testpassword"
        helper.storeApplicationPasswordCredentialsFrom(data)
        val result = helper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null user name returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, null, TEST_PASSWORD)
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&password=testpasswordr"
        val result = helper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing user name returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, "", TEST_PASSWORD)
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&password=testpasswordr"
        val result = helper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with null password returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, TEST_USER, null)
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser"
        val result = helper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with missing password returns false`() = runTest {
        whenever(uriLoginWrapper.parseUriLogin(any()))
            .thenReturn(
                ApplicationPasswordLoginHelper.UriLogin(TEST_URL, TEST_USER, "")
            )
        val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser"
        val result = helper.storeApplicationPasswordCredentialsFrom(data)
        assertFalse(result)
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data stores credentials`() = runTest {
            val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser&password=testpassword"
            val siteModel = SiteModel().apply {
                url = TEST_URL
                apiRestUsername = ENCRYPTED
                apiRestUsernameIV = IV
                apiRestPassword = ENCRYPTED
                apiRestPasswordIV = IV
            }
        whenever(siteSqlUtils.getSites()).thenReturn(listOf(siteModel))
        whenever(encryptionUtils.encrypt(any()))
            .thenReturn(
                Pair(
                    ENCRYPTED,
                    IV
                )
            )

        val result = helper.storeApplicationPasswordCredentialsFrom(data)

        assertTrue(result)
        verify(siteSqlUtils).getSites()
        verify(siteSqlUtils).insertOrUpdateSite(eq(siteModel))
    }

    @Test
    fun `storeApplicationPasswordCredentialsFrom with valid data but not matching site does not store credentials`() =
        runTest {
            val data = "jetpack://app-pass-authorize?site_url=http://test.com&user_login=testuser&password=testpassword"
            whenever(siteSqlUtils.getSites()).thenReturn(listOf())

            val result = helper.storeApplicationPasswordCredentialsFrom(data)

            assertFalse(result)
            verify(siteSqlUtils).getSites()
            verify(siteSqlUtils, times(0)).insertOrUpdateSite(any())
        }

    @Test
    fun `appendParamsToRestAuthorizationUrl with null authorizationUrl returns empty string`() {
        val result = helper.appendParamsToRestAuthorizationUrl(null)
        assertEquals("", result)
    }

    @Test
    fun `appendParamsToRestAuthorizationUrl with empty authorizationUrl returns empty string`() {
        val result = helper.appendParamsToRestAuthorizationUrl("")
        assertEquals("", result)
    }
 }
