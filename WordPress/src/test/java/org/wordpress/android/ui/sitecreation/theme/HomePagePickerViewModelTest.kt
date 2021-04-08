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
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesignCategory
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched
import org.wordpress.android.fluxc.store.ThemeStore.ThemeErrorType
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError
import org.wordpress.android.test
import org.wordpress.android.ui.PreviewMode
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Show
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.DesignSelectionAction
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher

private const val mockedDesignSlug = "mockedDesignSlug"
private const val mockedDesignSegmentId = 1L
private const val mockedDesignDemoUrl = "mockedDemoUrl"

@RunWith(MockitoJUnitRunner::class)
class HomePagePickerViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var networkUtils: NetworkUtilsWrapper
    @Mock lateinit var fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase
    @Mock lateinit var uiStateObserver: Observer<LayoutPickerUiState>
    @Mock lateinit var onDesignActionObserver: Observer<DesignSelectionAction>
    @Mock lateinit var onPreviewActionObserver: Observer<DesignPreviewAction>
    @Mock lateinit var previewModeObserver: Observer<PreviewMode>
    @Mock lateinit var analyticsTracker: SiteCreationTracker

    private lateinit var viewModel: HomePagePickerViewModel

    val mockCategory = StarterDesignCategory(
            slug = "about",
            title = "About",
            description = "About pages",
            emoji = "👋"
    )

    @Before
    fun setUp() {
        viewModel = HomePagePickerViewModel(
                networkUtils,
                dispatcher,
                fetchHomePageLayoutsUseCase,
                analyticsTracker,
                NoDelayCoroutineDispatcher(),
                NoDelayCoroutineDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.onDesignActionPressed.observeForever(onDesignActionObserver)
        viewModel.onPreviewActionPressed.observeForever(onPreviewActionObserver)
        viewModel.previewMode.observeForever(previewModeObserver)
    }

    private fun <T> mockResponse(isError: Boolean = false, block: suspend CoroutineScope.() -> T) = test {
        val response = if (isError) OnStarterDesignsFetched(
                emptyList(),
                emptyList(),
                ThemesError(ThemeErrorType.GENERIC_ERROR)
        )
        else OnStarterDesignsFetched(
                listOf(
                        StarterDesign(
                                mockedDesignSlug,
                                "title",
                                mockedDesignSegmentId,
                                listOf(mockCategory),
                                mockedDesignDemoUrl,
                                "theme",
                                "desktopThumbnail",
                                "tabletThumbnail",
                                "mobileThumbnail"
                        )
                ),
                listOf(mockCategory),
                null
        )
        whenever(fetchHomePageLayoutsUseCase.fetchStarterDesigns()).thenReturn(response)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
        block()
    }

    @Test
    fun `when the picker starts the content is loaded`() = mockResponse {
        viewModel.start()
        val captor = ArgumentCaptor.forClass(LayoutPickerUiState::class.java)
        verify(uiStateObserver, times(3)).onChanged(captor.capture())
        assertThat(captor.value is LayoutPickerUiState.Content)
        assertThat((captor.value as LayoutPickerUiState.Content).layoutCategories.size).isGreaterThan(0)
    }

    @Test
    fun `when the picker starts fetch errors are handled`() = mockResponse(isError = true) {
        viewModel.start()
        val captor = ArgumentCaptor.forClass(LayoutPickerUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())
        assertThat(captor.value is LayoutPickerUiState.Error)
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
        assertThat(requireNotNull(viewModel.uiState.value as LayoutPickerUiState.Content).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the user taps on a layout the layout is selected if the thumbnail has loaded`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(mockedDesignSlug)
        viewModel.onLayoutTapped(mockedDesignSlug)
        assertThat(requireNotNull(viewModel.uiState.value as LayoutPickerUiState.Content).selectedLayoutSlug)
                .isEqualTo(mockedDesignSlug)
    }

    @Test
    fun `when the user taps on a layout the layout is not selected if the thumbnail has not loaded`() = mockResponse {
        viewModel.start()
        viewModel.onLayoutTapped(mockedDesignSlug)
        assertThat(requireNotNull(viewModel.uiState.value as LayoutPickerUiState.Content).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the user taps on a selected layout the layout is deselected`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(mockedDesignSlug)
        viewModel.onLayoutTapped(mockedDesignSlug)
        viewModel.onLayoutTapped(mockedDesignSlug)
        assertThat(requireNotNull(viewModel.uiState.value as LayoutPickerUiState.Content).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the picker starts the toolbar is hidden`() = mockResponse {
        viewModel.start()
        assertThat(viewModel.uiState.value?.isToolbarVisible).isEqualTo(false)
    }

    @Test
    fun `when the user selects a design the toolbar is shown`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(mockedDesignSlug)
        viewModel.onLayoutTapped(mockedDesignSlug)
        assertThat(viewModel.uiState.value?.isToolbarVisible).isEqualTo(true)
    }

    @Test
    fun `when the user presses skip the default design is selected`() = mockResponse {
        viewModel.start()
        viewModel.onSkippedTapped()
        val captor = ArgumentCaptor.forClass(DesignSelectionAction::class.java)
        verify(onDesignActionObserver).onChanged(captor.capture())
        assertThat(captor.value.template).isEqualTo(defaultTemplateSlug)
    }

    @Test
    fun `when the user chooses a design the design info is passed to the next step`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(mockedDesignSlug)
        viewModel.onLayoutTapped(mockedDesignSlug)
        viewModel.onChooseTapped()
        val captor = ArgumentCaptor.forClass(DesignSelectionAction::class.java)
        verify(onDesignActionObserver).onChanged(captor.capture())
        assertThat(captor.value.template).isEqualTo(mockedDesignSlug)
    }

    @Test
    fun `when the user selects a design and presses preview the preview flow is triggered`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(mockedDesignSlug)
        viewModel.onLayoutTapped(mockedDesignSlug)
        viewModel.onPreviewTapped()
        val captor = ArgumentCaptor.forClass(DesignPreviewAction::class.java)
        verify(onPreviewActionObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as Show).template).isEqualTo(mockedDesignSlug)
        assertThat(requireNotNull(captor.value as Show).demoUrl).isEqualTo(mockedDesignDemoUrl)
    }

    @Test
    fun `when the picker starts on a phone the mobile thumbnails or preview load by default`() = mockResponse {
        viewModel.start(isTablet = false)
        val captor = ArgumentCaptor.forClass(PreviewMode::class.java)
        verify(previewModeObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as PreviewMode)).isEqualTo(PreviewMode.MOBILE)
    }

    @Test
    fun `when the picker starts on a tablet the tablet thumbnails or preview load by default`() = mockResponse {
        viewModel.start(isTablet = true)
        val captor = ArgumentCaptor.forClass(PreviewMode::class.java)
        verify(previewModeObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as PreviewMode)).isEqualTo(PreviewMode.TABLET)
    }

    @Test
    fun `when the changes the preview mode the thumbnails or preview change`() = mockResponse {
        viewModel.start()
        viewModel.onPreviewModeChanged(PreviewMode.DESKTOP)
        val captor = ArgumentCaptor.forClass(PreviewMode::class.java)
        verify(previewModeObserver, times(2)).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as PreviewMode)).isEqualTo(PreviewMode.DESKTOP)
    }
}
