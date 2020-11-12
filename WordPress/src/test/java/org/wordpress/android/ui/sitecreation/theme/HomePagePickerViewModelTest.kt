package org.wordpress.android.ui.sitecreation.theme

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.StarterDesignModel
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched
import org.wordpress.android.fluxc.store.ThemeStore.ThemeErrorType
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.UiState
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher

@RunWith(MockitoJUnitRunner::class)
class HomePagePickerViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var networkUtils: NetworkUtilsWrapper
    @Mock lateinit var fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase
    @Mock lateinit var uiStateObserver: Observer<UiState>

    private lateinit var viewModel: HomePagePickerViewModel

    @Before
    fun setUp() {
        viewModel = HomePagePickerViewModel(
                networkUtils,
                dispatcher,
                fetchHomePageLayoutsUseCase,
                NoDelayCoroutineDispatcher(),
                NoDelayCoroutineDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
    }

    private fun <T> mockResponse(isError: Boolean = false, block: suspend CoroutineScope.() -> T) = test {
        val response = if (isError) OnStarterDesignsFetched(emptyList(), ThemesError(ThemeErrorType.GENERIC_ERROR))
        else OnStarterDesignsFetched(
                listOf(StarterDesignModel(0, "slug", "title", "site", "demo", "theme", null, "image")),
                null
        )
        whenever(fetchHomePageLayoutsUseCase.fetchStarterDesigns()).thenReturn(response)
        block()
    }

    @Test
    fun `when the picker starts the content is loaded`() = mockResponse {
        viewModel.start()
        val captor = ArgumentCaptor.forClass(UiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        assertThat(captor.value is UiState.Content)
        assertThat((captor.value as UiState.Content).layouts.size).isGreaterThan(0)
    }

    @Test
    fun `when the picker starts fetch errors are handled`() = mockResponse(isError = true) {
        viewModel.start()
        val captor = ArgumentCaptor.forClass(UiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        assertThat(captor.value is UiState.Error)
    }

    @Test
    fun `when the user scroll beyond a threshold the title becomes visible`() = mockResponse {
        viewModel.start()
        viewModel.onAppBarOffsetChanged(9, 10)
        assertThat(viewModel.uiState.value?.isHeaderVisible).isEqualTo(true)
    }

    @Test
    fun `when the user scroll below a threshold the title remains hidden`() = mockResponse {
        viewModel.start()
        viewModel.onAppBarOffsetChanged(11, 10)
        assertThat(viewModel.uiState.value?.isHeaderVisible).isEqualTo(false)
    }

    @Test
    fun `when the picker starts no layout is selected`() = mockResponse {
        viewModel.start()
        assertThat(requireNotNull(viewModel.uiState.value as UiState.Content).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the user taps on a layout the layout is selected if the thumbnail has loaded`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady("slug")
        viewModel.onLayoutTapped("slug")
        assertThat(requireNotNull(viewModel.uiState.value as UiState.Content).selectedLayoutSlug).isEqualTo("slug")
    }

    @Test
    fun `when the user taps on a layout the layout is not selected if the thumbnail has not loaded`() = mockResponse {
        viewModel.start()
        viewModel.onLayoutTapped("slug")
        assertThat(requireNotNull(viewModel.uiState.value as UiState.Content).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the user taps on a selected layout the layout is deselected`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady("slug")
        viewModel.onLayoutTapped("slug")
        viewModel.onLayoutTapped("slug")
        assertThat(requireNotNull(viewModel.uiState.value as UiState.Content).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the picker starts the toolbar is hidden`() = mockResponse {
        viewModel.start()
        assertThat(viewModel.uiState.value?.isToolbarVisible).isEqualTo(false)
    }

    @Test
    fun `when the user selects a layout the toolbar is shown`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady("slug")
        viewModel.onLayoutTapped("slug")
        assertThat(viewModel.uiState.value?.isToolbarVisible).isEqualTo(true)
    }
}
