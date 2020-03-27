package org.wordpress.android.viewmodel.giphy.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.network.ApiClient
import com.tenor.android.core.network.ApiService.Builder
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.WeakRefCallback
import com.tenor.android.core.response.impl.GifsResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.BuildConfig
import org.wordpress.android.TestApplication
import org.wordpress.android.viewmodel.giphy.provider.GifProvider.GifRequestFailedException
import org.wordpress.android.viewmodel.giphy.provider.TenorProvider.GifRequestTimeoutException
import org.wordpress.android.viewmodel.giphy.provider.TenorProviderTestFixtures.expectedGifMediaViewModelCollection
import org.wordpress.android.viewmodel.giphy.provider.TenorProviderTestFixtures.mockedTenorResult
import retrofit2.Call

@ExperimentalCoroutinesApi
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
        val context = ApplicationProvider.getApplicationContext<Context>()

        Builder(context, IApiClient::class.java).apply {
            apiKey(BuildConfig.TENOR_API_KEY)
            ApiClient.init(context, this)
        }

        whenever(apiClient.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(gifSearchCall)

        val gifResults = mockedTenorResult
        whenever(gifResponse.results).thenReturn(gifResults)
        whenever(gifResponse.next).thenReturn("0")
        whenever(gifSearchCall.cancel()).then {
            whenever(gifSearchCall.isCanceled).thenReturn(true)
        }

        tenorProviderUnderTest = TenorProvider(context, apiClient)
    }

    @Test
    fun `search call should invoke onSuccess with expected GIF list and nextPosition for valid query`() {
        var onSuccessWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { actualViewModelCollection, _ ->
                    onSuccessWasCalled = true
                    assertThat(actualViewModelCollection).isEqualTo(expectedGifMediaViewModelCollection)
                },
                onFailure = {
                    fail("Failure handler should not be called")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), gifResponse)
        assertThat(onSuccessWasCalled).isTrue()
    }

    @Test
    fun `search call should invoke onSuccess with expected nextPosition for a valid query`() {
        var onSuccessWasCalled = false
        val expectedNextPosition = 0

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { _, actualNextPosition ->
                    onSuccessWasCalled = true
                    assertThat(actualNextPosition).isEqualTo(expectedNextPosition)
                },
                onFailure = {
                    fail("Failure handler should not be called")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), gifResponse)
        assertThat(onSuccessWasCalled).isTrue()
    }

    @Test
    fun `search call should invoke onFailure when callback returns failure`() {
        var onFailureWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { _, _ ->
                    fail("Success handler should not be called")
                },
                onFailure = { throwable ->
                    onFailureWasCalled = true
                    assertThat(throwable).isInstanceOf(GifRequestFailedException::class.java)
                    assertThat(throwable.message).isEqualTo("Expected message")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.failure(ApplicationProvider.getApplicationContext(), RuntimeException("Expected message"))
        assertThat(onFailureWasCalled).isTrue()
    }

    @Test
    fun `search call should invoke onFailure when null GifResponse is returned`() {
        var onFailureWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { _, _ ->
                    fail("Success handler should not be called")
                },
                onFailure = { throwable ->
                    onFailureWasCalled = true
                    assertThat(throwable).isInstanceOf(GifRequestFailedException::class.java)
                    assertThat(throwable.message).isEqualTo("No media matching your search")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), null)
        assertThat(onFailureWasCalled).isTrue()
    }

    @Test
    fun `search call must use BASIC as MediaFilter`() {
        val argument = ArgumentCaptor.forClass(String::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                any(),
                any(),
                argument.capture(),
                any()
        )

        val requestedMediaFilter = argument.value
        assertThat(requestedMediaFilter).isEqualTo(MediaFilter.BASIC)
    }

    @Test
    fun `search call without loadSize should use default maximum value`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        val requestedLoadSize = argument.value
        assertThat(requestedLoadSize).isEqualTo(50)
    }

    @Test
    fun `search call with loadSize lower than 50 should be used`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                20,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        val requestedLoadSize = argument.value
        assertThat(requestedLoadSize).isEqualTo(20)
    }

    @Test
    fun `search call with loadSize higher than 50 should be reduced back to default maximum value`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                1500,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        val requestedLoadSize = argument.value
        assertThat(requestedLoadSize).isEqualTo(50)
    }

    @Test
    fun `timeout job should trigger onFailure when nothing is invoked from search`() {
        var onFailureWasCalled = false
        var onSuccessWasCalled = false

        runBlockingTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            tenorProviderUnderTest = TenorProvider(context, apiClient, this)

            tenorProviderUnderTest.search("test",
                    0,
                    onSuccess = { _, _ ->
                        onSuccessWasCalled = true
                    },
                    onFailure = {
                        onFailureWasCalled = true
                    })
        }

        assertThat(onFailureWasCalled).isTrue()
        assertThat(onSuccessWasCalled).isFalse()
        assertThat(gifSearchCall.isCanceled).isFalse()
    }

    @Test
    fun `timeout job should not trigger onFailure when onSuccess is called`() {
        var onFailureWasCalled = false
        var onSuccessWasCalled = false

        runBlockingTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            tenorProviderUnderTest = TenorProvider(context, apiClient, this)

            tenorProviderUnderTest.search("test",
                    0,
                    onSuccess = { _, _ ->
                        onSuccessWasCalled = true
                    },
                    onFailure = {
                        onFailureWasCalled = true
                    })

            verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
            val capturedCallback = callbackCaptor.value
            capturedCallback.success(ApplicationProvider.getApplicationContext(), gifResponse)
        }

        assertThat(onSuccessWasCalled).isTrue()
        assertThat(onFailureWasCalled).isFalse()
        assertThat(gifSearchCall.isCanceled).isTrue()
    }

    @Test
    fun `timeout job should not trigger when onFailure is called from the search`() {
        var onFailureWasCalled = false
        var onSuccessWasCalled = false
        var expectedException: Throwable? = null

        runBlockingTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            tenorProviderUnderTest = TenorProvider(context, apiClient, this)

            tenorProviderUnderTest.search("test",
                    0,
                    onSuccess = { _, _ ->
                        onSuccessWasCalled = true
                    },
                    onFailure = {
                        onFailureWasCalled = true
                        expectedException = it
                    })

            verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
            val capturedCallback = callbackCaptor.value
            capturedCallback.failure(ApplicationProvider.getApplicationContext(), RuntimeException("Expected message"))
        }

        assertThat(onSuccessWasCalled).isFalse()
        assertThat(onFailureWasCalled).isTrue()
        assertThat(expectedException).isNotInstanceOf(GifRequestTimeoutException::class.java)
        assertThat(gifSearchCall.isCanceled).isTrue()
    }
}
