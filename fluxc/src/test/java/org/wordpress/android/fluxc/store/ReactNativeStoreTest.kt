package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers.Unconfined
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals

class ReactNativeStoreTest {
    private val wpComRestClient = mock<ReactNativeWPComRestClient>()
    private val wpApiRestClient = mock<ReactNativeWPAPIRestClient>()

    private val PATH = "a_url_path"
    private val PARAMS = mapOf("a_key" to "a_value")

    private lateinit var store: ReactNativeStore

    @Before
    fun setup() {
        store = ReactNativeStore(wpComRestClient, wpApiRestClient, Unconfined)
    }

    @Test
    fun `makes call to WPcom`() = test {
        val expectedResponse = mock<ReactNativeFetchResponse>()
        whenever(wpComRestClient.fetch(PATH, PARAMS, ::Success, ::Error)).thenReturn(expectedResponse)
        val actualResponse = store.performWPComRequest(PATH, PARAMS)
        assertEquals(expectedResponse, actualResponse)
    }

    @Test
    fun `makes call to WP api`() = test {
        val expectedResponse = mock<ReactNativeFetchResponse>()
        whenever(wpApiRestClient.fetch(PATH, PARAMS, ::Success, ::Error)).thenReturn(expectedResponse)
        val actualResponse = store.performWPAPIRequest(PATH, PARAMS)
        assertEquals(expectedResponse, actualResponse)
    }
}
