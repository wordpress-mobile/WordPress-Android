package org.wordpress.android.viewmodel.gif.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.WeakRefCallback
import com.tenor.android.core.response.impl.GifsResponse
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.TestApplication
import org.wordpress.android.viewmodel.gif.provider.TenorProviderTestUtils.Companion.createGifResultList
import org.wordpress.android.viewmodel.gif.provider.TenorProviderTestUtils.Companion.expectedMediaViewModelCollection
import retrofit2.Call

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class TenorProviderTest {
    @Mock lateinit var apiClient: IApiClient

    @Mock lateinit var gifSearchCall: Call<GifsResponse>

    @Mock lateinit var gifResponse: GifsResponse

    @Captor lateinit var callbackCaptor: ArgumentCaptor<WeakRefCallback<Context, GifsResponse>>

    private lateinit var tenorProviderUnderTest: TenorProvider

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(apiClient.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(gifSearchCall)

        val gifResults = createGifResultList()
        whenever(gifResponse.results).thenReturn(gifResults)

        tenorProviderUnderTest = TenorProvider(ApplicationProvider.getApplicationContext(), apiClient)
    }

    @Test
    fun `search call should invoke onSuccess with expected Gif list for valid query`() {
        var onSuccessWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = {
                    onSuccessWasCalled = true
                    assertEquals(expectedMediaViewModelCollection, it)
                },
                onFailure = {
                    fail("Failure handler should not be called")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), gifResponse)
        assert(onSuccessWasCalled) { "onSuccess should be called" }
    }

    @Test
    fun `search call should invoke onFailure when callback returns failure`() {
        var onFailureWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = {
                    fail("Success handler should not be called")
                },
                onFailure = {
                    onFailureWasCalled = true
                    assertEquals("Expected message", it.message)
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.failure(ApplicationProvider.getApplicationContext(), RuntimeException("Expected message"))
        assert(onFailureWasCalled) { "onFailure should be called" }
    }

    @Test
    fun `search call should invoke onFailure when null GifResponse is returned`() {
        var onFailureWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = {
                    fail("Success handler should not be called")
                },
                onFailure = {
                    onFailureWasCalled = true
                    assertEquals("No gifs matching your search", it.message)
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), null)
        assert(onFailureWasCalled) { "onFailure should be called" }
    }

    @Test
    fun `search call must use BASIC as MediaFilter`() {
        val argument = ArgumentCaptor.forClass(String::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                onSuccess = {},
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                any(),
                any(),
                argument.capture(),
                any()
        )

        assertEquals(MediaFilter.BASIC, argument.value)
    }

    @Test
    fun `search call without loadSize should use default maximum value`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                onSuccess = {},
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        assertEquals(50, argument.value)
    }

    @Test
    fun `search call with loadSize lower than 50 should be used`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                20,
                onSuccess = {},
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        assertEquals(20, argument.value)
    }

    @Test
    fun `search call with loadSize higher than 50 should be reduced back to default maximum value`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                1500,
                onSuccess = {},
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        assertEquals(50, argument.value)
    }
}
