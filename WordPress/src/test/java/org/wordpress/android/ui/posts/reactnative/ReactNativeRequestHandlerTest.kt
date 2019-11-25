package org.wordpress.android.ui.posts.reactnative

import androidx.core.util.Consumer
import com.google.gson.JsonElement
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.test
import org.wordpress.android.util.NoDelayCoroutineDispatcher

class ReactNativeRequestHandlerTest {
    private val reactNativeStore = mock<ReactNativeStore>()
    private val reactNativeUrlUtil = mock<ReactNativeUrlUtil>()
    private val site = mock<SiteModel>()

    private val pathWithParams = "/wp/v2/media?context=edit"
    private val siteId: Long = 1111
    private val siteUrl = "a_site_url"
    private val parsedPath = "/wp/v2/media"
    private val paramsMap = mapOf("context" to "edit")

    private lateinit var subject: ReactNativeRequestHandler

    @Before
    fun setUp() {
        subject = ReactNativeRequestHandler(
                reactNativeStore,
                reactNativeUrlUtil,
                NoDelayCoroutineDispatcher()
        )

        whenever(site.siteId).thenReturn(siteId)
        whenever(site.url).thenReturn(siteUrl)
    }

    @Test
    fun `WPcom get request that succeeds`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        var calledSuccess = false
        val jsonAsString = "json_as_string"
        val successHandler = Consumer<String> {
            if (it != jsonAsString) { fail("expected json was not returned: $it") }
            calledSuccess = true
        }

        val errorHandler = Consumer<String> {
            fail("Error handler should not be called")
        }

        whenever(reactNativeUrlUtil.parseUrlAndParamsForWPCom(pathWithParams, siteId)).thenReturn(
                Pair(parsedPath, paramsMap)
        )

        val successfulResponseJson = mock<JsonElement>()
        whenever(successfulResponseJson.toString()).thenReturn(jsonAsString)
        val fetchResponse = ReactNativeFetchResponse.Success(successfulResponseJson)
        whenever(reactNativeStore.performWPComRequest(parsedPath, paramsMap)).thenReturn(fetchResponse)

        subject.performGetRequest(pathWithParams, site, successHandler, errorHandler)

        assertTrue(calledSuccess)
    }

    @Test
    fun `WPcom get request that fails`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        var calledError = false
        val errorMessage = "error_message"
        val errorHandler = Consumer<String> {
            if (it != errorMessage) { fail("expected json was not returned: $it") }
            calledError = true
        }

        val successHandler = Consumer<String> {
            fail("Success handler should not be called")
        }

        whenever(reactNativeUrlUtil.parseUrlAndParamsForWPCom(pathWithParams, siteId)).thenReturn(
                Pair(parsedPath, paramsMap)
        )

        val fetchResponse = mock<ReactNativeFetchResponse.Error>()
        whenever(fetchResponse.error).thenReturn(errorMessage)
        whenever(reactNativeStore.performWPComRequest(parsedPath, paramsMap)).thenReturn(fetchResponse)

        subject.performGetRequest(pathWithParams, site, successHandler, errorHandler)

        assertTrue(calledError)
    }

    @Test
    fun `WPorg get request that succeeds`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)

        var calledSuccess = false
        val jsonAsString = "json_as_string"
        val successHandler = Consumer<String> {
            if (it != jsonAsString) { fail("expected json was not returned: $it") }
            calledSuccess = true
        }

        val errorHandler = Consumer<String> {
            fail("Error handler should not be called")
        }

        whenever(reactNativeUrlUtil.parseUrlAndParamsForWPOrg(pathWithParams, siteUrl)).thenReturn(
                Pair(parsedPath, paramsMap)
        )

        val successfulResponseJson = mock<JsonElement>()
        whenever(successfulResponseJson.toString()).thenReturn(jsonAsString)
        val fetchResponse = ReactNativeFetchResponse.Success(successfulResponseJson)
        whenever(reactNativeStore.performWPAPIRequest(parsedPath, paramsMap)).thenReturn(fetchResponse)

        subject.performGetRequest(pathWithParams, site, successHandler, errorHandler)

        assertTrue(calledSuccess)
    }

    @Test
    fun `WPorg get request that fails`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(false)

        var calledError = false
        val errorMessage = "error_message"
        val errorHandler = Consumer<String> {
            if (it != errorMessage) { fail("expected json was not returned: $it") }
            calledError = true
        }

        val successHandler = Consumer<String> {
            fail("Success handler should not be called")
        }

        whenever(reactNativeUrlUtil.parseUrlAndParamsForWPOrg(pathWithParams, siteUrl)).thenReturn(
                Pair(parsedPath, paramsMap)
        )

        val fetchResponse = mock<ReactNativeFetchResponse.Error>()
        whenever(fetchResponse.error).thenReturn(errorMessage)
        whenever(reactNativeStore.performWPAPIRequest(parsedPath, paramsMap)).thenReturn(fetchResponse)

        subject.performGetRequest(pathWithParams, site, successHandler, errorHandler)

        assertTrue(calledError)
    }
}
