package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import java.util.Optional
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ApplicationPasswordManagerTests {
    private val applicationName = "name"
    private val siteDomain = "test-site.com"
    private val uuid = "uuid"
    private val testSite = SiteModel().apply {
        url = "http://$siteDomain"
    }
    private val testCredentials = ApplicationPasswordCredentials(
        userName = "username",
        password = "password",
        uuid = "uuid"
    )
    private val applicationPasswordsStore: ApplicationPasswordsStore = mock()
    private val mJetpackApplicationPasswordsRestClient: JetpackApplicationPasswordsRestClient = mock()
    private val mWpApiApplicationPasswordsRestClient: WPApiApplicationPasswordsRestClient = mock()

    private val applicationPasswordsConfiguration = ApplicationPasswordsConfiguration(Optional.of(applicationName))

    private lateinit var mApplicationPasswordsManager: ApplicationPasswordsManager

    @Before
    fun setup() {
        mApplicationPasswordsManager = ApplicationPasswordsManager(
            applicationPasswordsStore = applicationPasswordsStore,
            jetpackApplicationPasswordsRestClient = mJetpackApplicationPasswordsRestClient,
            wpApiApplicationPasswordsRestClient = mWpApiApplicationPasswordsRestClient,
            configuration = applicationPasswordsConfiguration,
            appLogWrapper = mock()
        )
    }

    @Test
    fun `given a local password exists, when we ask for a password, then return it`() = runBlockingTest {
        whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(testCredentials)
        val result = mApplicationPasswordsManager.getApplicationCredentials(
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
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(
                mJetpackApplicationPasswordsRestClient.createApplicationPassword(
                    site,
                    applicationName
                )
            )
                .thenReturn(
                    ApplicationPasswordCreationPayload(
                        testCredentials.password,
                        testCredentials.uuid
                    )
                )

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
            verify(applicationPasswordsStore).saveCredentials(siteDomain, testCredentials)
        }

    @Test
    fun `given no local password is saved, when we ask for a password for a non-jetpack site, then create it`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }

            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(
                mWpApiApplicationPasswordsRestClient.createApplicationPassword(
                    site,
                    applicationName
                )
            )
                .thenReturn(
                    ApplicationPasswordCreationPayload(
                        testCredentials.password,
                        testCredentials.uuid
                    )
                )

            val result = mApplicationPasswordsManager.getApplicationCredentials(
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
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(mJetpackApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
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
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(mJetpackApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
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
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
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
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
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

            whenever(applicationPasswordsStore.getUuid(siteDomain)).thenReturn(uuid)
            whenever(mJetpackApplicationPasswordsRestClient.deleteApplicationPassword(site, uuid))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = mApplicationPasswordsManager.deleteApplicationCredentials(
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

            whenever(applicationPasswordsStore.getUuid(siteDomain)).thenReturn(uuid)
            whenever(mWpApiApplicationPasswordsRestClient.deleteApplicationPassword(site, uuid))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = mApplicationPasswordsManager.deleteApplicationCredentials(
                testSite
            )

            assertEquals(ApplicationPasswordDeletionResult.Success, result)
        }

    @Test
    fun `given a duplicate password already exists, when creating a new password, then delete the previous one`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }
            val creationNetworkError = BaseNetworkError(VolleyError(NetworkResponse(409, null, true, 0, emptyList())))
            whenever(applicationPasswordsStore.getCredentials(siteDomain)).thenReturn(null)
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(creationNetworkError))
                .thenReturn(ApplicationPasswordCreationPayload(testCredentials.password, testCredentials.uuid))
            whenever(mWpApiApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName))
                .thenReturn(ApplicationPasswordUUIDFetchPayload(uuid))
            whenever(mWpApiApplicationPasswordsRestClient.deleteApplicationPassword(site, uuid))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = mApplicationPasswordsManager.getApplicationCredentials(site)

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
            verify(mWpApiApplicationPasswordsRestClient).fetchApplicationPasswordUUID(site, applicationName)
            verify(mWpApiApplicationPasswordsRestClient).deleteApplicationPassword(site, uuid)
        }

    @Test
    fun `given uuid doesn't exist locally, when deleting a password, then fetch it`() =
        runBlockingTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }
            whenever(applicationPasswordsStore.getUuid(siteDomain)).thenReturn(null)
            whenever(mWpApiApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName))
                .thenReturn(ApplicationPasswordUUIDFetchPayload(uuid))
            whenever(mWpApiApplicationPasswordsRestClient.deleteApplicationPassword(site, uuid))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = mApplicationPasswordsManager.deleteApplicationCredentials(site)

            assertEquals(ApplicationPasswordDeletionResult.Success, result)
            verify(mWpApiApplicationPasswordsRestClient).fetchApplicationPasswordUUID(site, applicationName)
            verify(mWpApiApplicationPasswordsRestClient).deleteApplicationPassword(site, uuid)
        }
}
