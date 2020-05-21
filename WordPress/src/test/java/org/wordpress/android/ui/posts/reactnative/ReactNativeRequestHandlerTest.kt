package org.wordpress.android.ui.posts.reactnative

import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.core.util.Consumer
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.google.gson.JsonElement
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.TestApplication
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.test
import org.wordpress.android.util.NoDelayCoroutineDispatcher

@Config(application = TestApplication::class, sdk = [VERSION_CODES.LOLLIPOP])
@RunWith(RobolectricTestRunner::class)
class ReactNativeRequestHandlerTest {
    private val reactNativeStore = mock<ReactNativeStore>()
    private val site = mock<SiteModel>()
    private val pathWithParams = "/wp/v2/media?context=edit"
    private lateinit var subject: ReactNativeRequestHandler

    @Before
    fun setUp() {
        subject = ReactNativeRequestHandler(
                reactNativeStore,
                NoDelayCoroutineDispatcher()
        )
    }

    @Test
    fun `successful request`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        var calledSuccess = false
        val jsonAsString = "json_as_string"
        val successHandler = Consumer<String> {
            if (it != jsonAsString) { fail("expected json was not returned: $it") }
            calledSuccess = true
        }

        val errorHandler = Consumer<Bundle> {
            fail("Error handler should not be called")
        }

        val successfulResponseJson = mock<JsonElement>()
        whenever(successfulResponseJson.toString()).thenReturn(jsonAsString)

        val fetchResponse = ReactNativeFetchResponse.Success(successfulResponseJson)
        whenever(reactNativeStore.executeRequest(site, pathWithParams)).thenReturn(fetchResponse)

        subject.performGetRequest(pathWithParams, site, successHandler, errorHandler)

        assertTrue(calledSuccess)
    }

    @Test
    fun `failed request`() = test {
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        var calledError = false
        val errorMessage = "error_message"
        val statusCode = 444
        val errorHandler = Consumer<Bundle> {
            assertEquals(statusCode, it.getInt("code"))
            assertEquals(errorMessage, it.getString("message"))
            calledError = true
        }

        val successHandler = Consumer<String> {
            fail("Success handler should not be called")
        }

        val fetchResponse = getFetchResponseError(errorMessage, statusCode)
        whenever(reactNativeStore.executeRequest(site, pathWithParams)).thenReturn(fetchResponse)

        subject.performGetRequest(pathWithParams, site, successHandler, errorHandler)

        assertTrue(calledError)
    }

    private fun getFetchResponseError(errorMessage: String, statusCode: Int): Error {
        val volleyNetworkResponse = NetworkResponse(statusCode, null, false, 0L, null)
        val volleyError = VolleyError(volleyNetworkResponse)

        val baseNetworkError = mock<BaseNetworkError>()
        baseNetworkError.message = errorMessage
        baseNetworkError.volleyError = volleyError

        return Error(baseNetworkError)
    }
}
