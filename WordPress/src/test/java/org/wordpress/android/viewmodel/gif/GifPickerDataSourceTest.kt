package org.wordpress.android.viewmodel.gif

import androidx.paging.PositionalDataSource.LoadInitialCallback
import androidx.paging.PositionalDataSource.LoadInitialParams
import androidx.paging.PositionalDataSource.LoadRangeCallback
import androidx.paging.PositionalDataSource.LoadRangeParams
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.wordpress.android.viewmodel.gif.GifPickerDataSourceFixtures.expectedGifMediaViewModelCollection
import org.wordpress.android.viewmodel.gif.provider.GifProvider
import org.wordpress.android.viewmodel.gif.provider.GifProvider.GifRequestFailedException

class GifPickerDataSourceTest {
    @Mock lateinit var gifProvider: GifProvider

    lateinit var onSuccessCaptor: KArgumentCaptor<(List<GifMediaViewModel>, Int?) -> Unit>

    lateinit var onFailureCaptor: KArgumentCaptor<(Throwable) -> Unit>

    private lateinit var pickerDataSourceUnderTest: GifPickerDataSource

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        pickerDataSourceUnderTest = GifPickerDataSource(gifProvider, "test")
        onSuccessCaptor = argumentCaptor()
        onFailureCaptor = argumentCaptor()
    }

    @Test
    fun `loadInitial should call onResult when search is successful`() {
        var onResultWasCalled = false

        val params = LoadInitialParams(0, 20, 5, false)
        val dataSourceCallback = object : LoadInitialCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int, totalCount: Int) {
                onResultWasCalled = true
                assertThat(data).isEqualTo(expectedGifMediaViewModelCollection)
                assertThat(position).isEqualTo(0)
                assertThat(totalCount).isEqualTo(4)
            }

            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int) {
                fail("Wrong onResult called")
            }
        }
        pickerDataSourceUnderTest.loadInitial(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), onSuccessCaptor.capture(), any())
        val capturedProviderOnSuccess = onSuccessCaptor.firstValue
        capturedProviderOnSuccess(expectedGifMediaViewModelCollection, 4)
        assertThat(onResultWasCalled).isTrue()
    }

    @Test
    fun `loadInitial should call onResult with correct position when search is successful but next position is null`() {
        var onResultWasCalled = false

        val params = LoadInitialParams(0, 20, 5, false)
        val dataSourceCallback = object : LoadInitialCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int, totalCount: Int) {
                onResultWasCalled = true
                assertThat(data).isEqualTo(expectedGifMediaViewModelCollection)
                assertThat(position).isEqualTo(0)
                assertThat(totalCount).isEqualTo(4)
            }

            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int) {
                fail("Wrong onResult called")
            }
        }
        pickerDataSourceUnderTest.loadInitial(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), onSuccessCaptor.capture(), any())
        val capturedProviderOnSuccess = onSuccessCaptor.firstValue
        capturedProviderOnSuccess(expectedGifMediaViewModelCollection, null)
        assertThat(onResultWasCalled).isTrue()
    }

    @Test
    fun `loadInitial should call onResult with position when search is successful but next position is incorrect`() {
        var onResultWasCalled = false

        val params = LoadInitialParams(0, 20, 5, false)
        val dataSourceCallback = object : LoadInitialCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int, totalCount: Int) {
                onResultWasCalled = true
                assertThat(data).isEqualTo(expectedGifMediaViewModelCollection)
                assertThat(position).isEqualTo(0)
                assertThat(totalCount).isEqualTo(4)
            }

            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int) {
                fail("Wrong onResult called")
            }
        }
        pickerDataSourceUnderTest.loadInitial(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), onSuccessCaptor.capture(), any())
        val capturedProviderOnSuccess = onSuccessCaptor.firstValue
        capturedProviderOnSuccess(expectedGifMediaViewModelCollection, 2)
        assertThat(onResultWasCalled).isTrue()
    }

    @Test
    fun `loadInitial should call onResult with emptyList when search query is blank`() {
        var onResultWasCalled = false
        pickerDataSourceUnderTest = GifPickerDataSource(gifProvider, "")

        val params = LoadInitialParams(0, 20, 5, false)
        val dataSourceCallback = object : LoadInitialCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int, totalCount: Int) {
                onResultWasCalled = true
                assertThat(data).isEmpty()
                assertThat(position).isEqualTo(0)
                assertThat(totalCount).isEqualTo(0)
            }

            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int) {
                fail("Wrong onResult called")
            }
        }
        pickerDataSourceUnderTest.loadInitial(params, dataSourceCallback)

        verify(gifProvider, times(0)).search(any(), any(), any(), any(), any())
        assertThat(onResultWasCalled).isTrue()
    }

    @Test
    fun `loadInitial should call onResult when search fails with emptyList`() {
        var onResultWasCalled = false
        val expectedThrowable = GifRequestFailedException("Test throwable")

        val params = LoadInitialParams(0, 20, 5, false)
        val dataSourceCallback = object : LoadInitialCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int, totalCount: Int) {
                onResultWasCalled = true
                assertThat(data).isEmpty()
                assertThat(position).isEqualTo(0)
                assertThat(totalCount).isEqualTo(0)
            }

            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int) {
                fail("Wrong onResult called")
            }
        }
        pickerDataSourceUnderTest.loadInitial(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), any(), onFailureCaptor.capture())
        val capturedProviderOnFailure = onFailureCaptor.firstValue
        capturedProviderOnFailure(expectedThrowable)

        assertThat(onResultWasCalled).isTrue()
    }

    @Test
    fun `loadInitial should set initialLoadError when search fails`() {
        var onResultWasCalled = false
        val expectedThrowable = GifRequestFailedException("Test throwable")

        val params = LoadInitialParams(0, 20, 5, false)
        val dataSourceCallback = object : LoadInitialCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int, totalCount: Int) {
                onResultWasCalled = true
            }

            override fun onResult(data: MutableList<GifMediaViewModel>, position: Int) {
                fail("Wrong onResult called")
            }
        }
        pickerDataSourceUnderTest.loadInitial(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), any(), onFailureCaptor.capture())
        val capturedProviderOnFailure = onFailureCaptor.firstValue
        capturedProviderOnFailure(expectedThrowable)

        assertThat(onResultWasCalled).isTrue()
        assertThat(pickerDataSourceUnderTest.initialLoadError).isNotNull()
        assertThat(pickerDataSourceUnderTest.initialLoadError).isInstanceOf(GifRequestFailedException::class.java)
        assertThat(pickerDataSourceUnderTest.initialLoadError?.message).isEqualTo("Test throwable")
    }

    @Test
    fun `loadRange should call onResult when search is successful`() {
        var onResultWasCalled = false

        val params = LoadRangeParams(0, 20)
        val dataSourceCallback = object : LoadRangeCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>) {
                onResultWasCalled = true
                assertThat(data).isEqualTo(expectedGifMediaViewModelCollection)
            }
        }
        pickerDataSourceUnderTest.loadRange(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), onSuccessCaptor.capture(), any())
        val capturedProviderOnSuccess = onSuccessCaptor.firstValue
        capturedProviderOnSuccess(expectedGifMediaViewModelCollection, 4)
        assertThat(onResultWasCalled).isTrue()
    }

    @Test
    fun `loadRange should call retryAllFailedRangeLoads when search is successful`() {
        val spiedDataSource = spy(pickerDataSourceUnderTest)

        val params = LoadRangeParams(0, 20)
        val dataSourceCallback = object : LoadRangeCallback<GifMediaViewModel>() {
            override fun onResult(data: MutableList<GifMediaViewModel>) {}
        }
        spiedDataSource.loadRange(params, dataSourceCallback)

        verify(gifProvider, times(1)).search(any(), any(), any(), onSuccessCaptor.capture(), any())
        val capturedProviderOnSuccess = onSuccessCaptor.firstValue
        capturedProviderOnSuccess(expectedGifMediaViewModelCollection, 2)

        verify(spiedDataSource, times(1)).retryAllFailedRangeLoads()
    }
}
