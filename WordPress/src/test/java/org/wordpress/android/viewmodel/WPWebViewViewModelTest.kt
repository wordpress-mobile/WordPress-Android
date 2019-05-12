package org.wordpress.android.viewmodel

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.test
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewContentUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenErrorUiState
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenProgressUiState
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class WPWebViewViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var networkUtils: NetworkUtilsWrapper
    @Mock private lateinit var mUiStateObserver: Observer<WebPreviewUiState>

    private lateinit var mViewModel: WPWebViewViewModel

    @Before
    fun setUp() {
        mViewModel = WPWebViewViewModel(networkUtils)
        mViewModel.mUiState.observeForever(mUiStateObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `progress shown on start`() = test {
        initViewModel()
        Assertions.assertThat(mViewModel.mUiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    @Test
    fun `error shown on start when internet access not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        initViewModel()
        Assertions.assertThat(mViewModel.mUiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `error shown on error failure`() {
        initViewModel()
        mViewModel.onError()
        Assertions.assertThat(mViewModel.mUiState.value).isInstanceOf(WebPreviewFullscreenErrorUiState::class.java)
    }

    @Test
    fun `show content on UrlLoaded`() {
        initViewModel()
        mViewModel.onUrlLoaded()
        Assertions.assertThat(mViewModel.mUiState.value).isInstanceOf(WebPreviewContentUiState::class.java)
    }

    @Test
    fun `show progress screen on retry clicked`() {
        initViewModel()
        mViewModel.retry()
        Assertions.assertThat(mViewModel.mUiState.value).isInstanceOf(WebPreviewFullscreenProgressUiState::class.java)
    }

    private fun initViewModel() {
        mViewModel.start()
    }
}
