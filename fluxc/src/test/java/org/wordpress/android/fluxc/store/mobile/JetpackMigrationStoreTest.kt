package org.wordpress.android.fluxc.store.mobile

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.JetpackMigrationRestClient
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload.Success
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload.Error
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class JetpackMigrationStoreTest {
    @Mock private lateinit var restClient: JetpackMigrationRestClient
    private lateinit var store: JetpackMigrationStore

    private val successResponse = Success
    private val errorResponse = Error(BaseNetworkError(SERVER_ERROR))

    @Before
    fun setUp() {
        store = JetpackMigrationStore(restClient, initCoroutineEngine())
    }

    @Test
    fun `given success, a success result is returned`() = test {
        whenever(restClient.migrationComplete(any())).thenReturn(Success)
        val response = store.migrationComplete()
        assertNotNull(response)
        assertEquals(successResponse, response)
    }

    @Test
    fun `when an error occurs, the error is returned`() = test {
        whenever(restClient.migrationComplete(any())).thenReturn(errorResponse)
        val response = store.migrationComplete()
        assertNotNull(response)
        assertEquals(errorResponse, response)
    }
}
