package org.wordpress.android.fluxc.store.mobile

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigFetchedPayload
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigRestClient
import org.wordpress.android.fluxc.persistence.RemoteConfigDao
import org.wordpress.android.fluxc.store.mobile.RemoteConfigStore.RemoteConfigResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class RemoteConfigStoreTest {
    @Mock private lateinit var restClient: RemoteConfigRestClient
    @Mock private lateinit var remoteConfigDao: RemoteConfigDao
    private lateinit var store: RemoteConfigStore

    private val successResponse = mapOf("jp-deadline" to "2022-10-10")
    private val errorResponse = RemoteConfigError( type = GENERIC_ERROR)
    private val errorResult = RemoteConfigError( type = GENERIC_ERROR)


    @Before
    fun setUp() {
        store = RemoteConfigStore(restClient, remoteConfigDao, initCoroutineEngine())
    }

    @Test
    fun `given success, when fetch remote-config is triggered, then result is returned`() = test {
        whenever(restClient.fetchRemoteConfig()).thenReturn(
            RemoteConfigFetchedPayload(successResponse)
        )

        val response = store.fetchRemoteConfig()

        verify(remoteConfigDao).insert(successResponse)
        assertNotNull(response.remoteConfig)
        assertEquals(RemoteConfigResult(successResponse), response)
    }

    @Test
    fun `given error, when fetch remote-config is triggered, then error result is returned`() = test {
        whenever(restClient.fetchRemoteConfig()).thenReturn(
            RemoteConfigFetchedPayload(errorResponse)
        )

        val response = store.fetchRemoteConfig()

        verifyNoInteractions(remoteConfigDao)
        assertNull(response.remoteConfig)
        assertEquals(RemoteConfigResult(errorResult), response)
    }
}
