package org.wordpress.android.ui.sitecreation.theme

import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesign
import org.wordpress.android.fluxc.network.rest.wpcom.theme.StarterDesignCategory
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched
import org.wordpress.android.fluxc.store.ThemeStore.ThemeErrorType
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError
import org.wordpress.android.ui.PreviewMode
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Show
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.DesignSelectionAction
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.viewmodel.ResourceProvider

private const val MOCKED_DESIGN_SLUG = "mockedDesignSlug"
private const val MOCKED_DESIGN_SEGMENT_ID = 1L
private const val MOCKED_DESIGN_DEMO_URL = "mockedDemoUrl"

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HomePagePickerViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher
    @Mock
    lateinit var networkUtils: NetworkUtilsWrapper
    @Mock
    lateinit var fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase
    @Mock
    lateinit var uiStateObserver: Observer<LayoutPickerUiState>
    @Mock
    lateinit var onDesignActionObserver: Observer<DesignSelectionAction>
    @Mock
    lateinit var onPreviewActionObserver: Observer<DesignPreviewAction>
    @Mock
    lateinit var previewModeObserver: Observer<PreviewMode>
    @Mock
    lateinit var analyticsTracker: SiteCreationTracker
    @Mock
    lateinit var resourceProvider: ResourceProvider

    private lateinit var recommendationProvider: SiteDesignRecommendationProvider
    private lateinit var viewModel: HomePagePickerViewModel

    private val slugsArray = arrayOf("art", "food", "beauty")
    private val verticalArray = arrayOf("Art", "Food", "Beauty")

    private val mockCategory = StarterDesignCategory(
        slug = "blog",
        title = "Blog",
        description = "Blogging designs",
        emoji = "👋"
    )

    @Before
    fun setUp() {
        recommendationProvider = SiteDesignRecommendationProvider(resourceProvider)
        viewModel = HomePagePickerViewModel(
            networkUtils,
            dispatcher,
            fetchHomePageLayoutsUseCase,
            analyticsTracker,
            NoDelayCoroutineDispatcher(),
            NoDelayCoroutineDispatcher(),
            recommendationProvider
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
                    MOCKED_DESIGN_SLUG,
                    "title",
                    MOCKED_DESIGN_SEGMENT_ID,
                    listOf(mockCategory),
                    MOCKED_DESIGN_DEMO_URL,
                    "theme",
                    listOf("stable", "blog"),
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
        whenever(resourceProvider.getString(any())).thenReturn("Blogging")
        whenever(resourceProvider.getString(any(), any())).thenReturn("Best for Blogging")
        whenever(resourceProvider.getStringArray(R.array.site_creation_intents_slugs)).thenReturn(slugsArray)
        whenever(resourceProvider.getStringArray(R.array.site_creation_intents_strings)).thenReturn(verticalArray)
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
    fun `when the user taps on a layout the preview flow is triggered`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(MOCKED_DESIGN_SLUG)
        viewModel.onLayoutTapped(MOCKED_DESIGN_SLUG)
        val captor = ArgumentCaptor.forClass(DesignPreviewAction::class.java)
        verify(onPreviewActionObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as Show).template).isEqualTo(MOCKED_DESIGN_SLUG)
        assertThat(requireNotNull(captor.value as Show).demoUrl).isEqualTo(MOCKED_DESIGN_DEMO_URL)
    }

    @Test
    fun `when the user taps on a thumbnail that has not loaded, the preview flow is not triggered`() = mockResponse {
        viewModel.start()
        viewModel.onLayoutTapped(MOCKED_DESIGN_SLUG)
        verify(onPreviewActionObserver, never()).onChanged(isA(Show::class.java))
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
    fun `when the picker starts on a phone the mobile previews load by default`() = mockResponse {
        viewModel.start(isTablet = false)
        val captor = ArgumentCaptor.forClass(PreviewMode::class.java)
        verify(previewModeObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as PreviewMode)).isEqualTo(PreviewMode.MOBILE)
    }

    @Test
    fun `when the picker starts on a tablet the tablet preview loads by default`() = mockResponse {
        viewModel.start(isTablet = true)
        val captor = ArgumentCaptor.forClass(PreviewMode::class.java)
        verify(previewModeObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as PreviewMode)).isEqualTo(PreviewMode.TABLET)
    }

    @Test
    fun `when the user changes the preview mode the previews change`() = mockResponse {
        viewModel.start()
        viewModel.onPreviewModeChanged(PreviewMode.DESKTOP)
        val captor = ArgumentCaptor.forClass(PreviewMode::class.java)
        verify(previewModeObserver, times(2)).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value as PreviewMode)).isEqualTo(PreviewMode.DESKTOP)
    }

    @Test
    fun `when the user chooses a design the design info is passed to the next step`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(MOCKED_DESIGN_SLUG)
        viewModel.onLayoutTapped(MOCKED_DESIGN_SLUG)
        viewModel.onPreviewChooseTapped()
        val captor = ArgumentCaptor.forClass(DesignSelectionAction::class.java)
        verify(onDesignActionObserver).onChanged(captor.capture())
        assertThat(captor.value.template).isEqualTo(MOCKED_DESIGN_SLUG)
    }

    @Test
    fun `when the user chooses a recommended design the recommended information is emitted`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(MOCKED_DESIGN_SLUG)
        viewModel.onLayoutTapped(MOCKED_DESIGN_SLUG, true)
        viewModel.onPreviewChooseTapped()
        verify(analyticsTracker).trackSiteDesignSelected(MOCKED_DESIGN_SLUG, true)
    }

    @Test
    fun `when the user chooses a design that is not recommended the correct information is emitted`() = mockResponse {
        viewModel.start()
        viewModel.onThumbnailReady(MOCKED_DESIGN_SLUG)
        viewModel.onLayoutTapped(MOCKED_DESIGN_SLUG, false)
        viewModel.onPreviewChooseTapped()
        verify(analyticsTracker).trackSiteDesignSelected(MOCKED_DESIGN_SLUG, false)
    }

    @Test
    fun `when the vertical changes the designs reload`() = mockResponse {
        val expectedUiStateChangesOnEachLoad = 3
        viewModel.start(intent = "Art")
        viewModel.start(intent = "Food")
        val captor = ArgumentCaptor.forClass(LayoutPickerUiState::class.java)
        verify(uiStateObserver, times(expectedUiStateChangesOnEachLoad * 2)).onChanged(captor.capture())
        assertThat(captor.value is LayoutPickerUiState.Content)
    }

    @Test
    fun `when the vertical is the same the designs do not change`() = mockResponse {
        val expectedUiStateChangesOnEachLoad = 3
        viewModel.start(intent = "Art")
        viewModel.start(intent = "Art")
        val captor = ArgumentCaptor.forClass(LayoutPickerUiState::class.java)
        verify(uiStateObserver, times(expectedUiStateChangesOnEachLoad)).onChanged(captor.capture())
        assertThat(captor.value is LayoutPickerUiState.Content)
    }
}
