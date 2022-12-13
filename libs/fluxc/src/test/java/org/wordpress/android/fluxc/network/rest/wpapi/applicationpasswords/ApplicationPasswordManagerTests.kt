package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ApplicationPasswordManagerTests {
    private val applicationName = "name"
    private val siteDomain = "test-site.com"
    private val testSite = SiteModel().apply {
        url = "http://$siteDomain"
    }
    private val testCredentials = ApplicationPasswordCredentials(
        userName = "username",
        password = "password"
    )
    private val applicationPasswordsStore: ApplicationPasswordsStore = mock()
    private val jetpackApplicationPasswordRestClient: JetpackApplicationPasswordRestClient = mock()
    private val wpApiApplicationPasswordRestClient: WPApiApplicationPasswordRestClient = mock()

    private lateinit var applicationPasswordManager: ApplicationPasswordManager

    @Before
    fun setup() {
        applicationPasswordManager = ApplicationPasswordManager(
            applicationPasswordsStore = applicationPasswordsStore,
            applicationName = applicationName,
            jetpackApplicationPasswordRestClient = jetpackApplicationPasswordRestClient,
            wpApiApplicationPasswordRestClient = wpApiApplicationPasswordRestClient,
            appLogWrapper = mock()
        )
    }

    @Test
    fun `given a local password exists, when we ask for a password, then return it`() = runBlockingTest {
        whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(testCredentials)

        val result = applicationPasswordManager.getApplicationCredentials(
            testSite
        )

        assertEquals(ApplicationPasswordCreationResult.Existing(testCredentials), result)
    }

    @Test
    fun `given no local password is saved, when we ask for a password for a jetpack site, then create it`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(jetpackApplicationPasswordRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(jetpackApplicationPasswordRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(testCredentials.password))

            val result = applicationPasswordManager.getApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
        }

    @Test
    fun `given no local password is saved, when we ask for a password for a non-jetpack site, then create it`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(wpApiApplicationPasswordRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(testCredentials.password))

            val result = applicationPasswordManager.getApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
        }

    @Test
    fun `when a jetpack site returns 404, then return feature not available`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(404, null, true, 0, emptyList())))

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(jetpackApplicationPasswordRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(jetpackApplicationPasswordRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = applicationPasswordManager.getApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when a jetpack site returns application_passwords_disabled, then return feature not available`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }
            val networkError = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR)).apply {
                apiError = "application_passwords_disabled"
            }

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(jetpackApplicationPasswordRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(jetpackApplicationPasswordRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = applicationPasswordManager.getApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when a non-jetpack site returns 404, then return feature not available`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(404, null, true, 0, emptyList())))

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(wpApiApplicationPasswordRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = applicationPasswordManager.getApplicationCredentials(
                testSite
            )

            Assert.assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when a non-jetpack site returns application_passwords_disabled, then return feature not available`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }
            val networkError = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR)).apply {
                apiError = "application_passwords_disabled"
            }

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(wpApiApplicationPasswordRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = applicationPasswordManager.getApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when password delete is requested for a jetpack site, then process it`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }

            whenever(jetpackApplicationPasswordRestClient.deleteApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = applicationPasswordManager.deleteApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordDeletionResult.Success, result)
        }

    @Test
    fun `when password delete is requested for a non-jetpack site, then process it`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }

            whenever(wpApiApplicationPasswordRestClient.deleteApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = applicationPasswordManager.deleteApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordDeletionResult.Success, result)
        }
}
