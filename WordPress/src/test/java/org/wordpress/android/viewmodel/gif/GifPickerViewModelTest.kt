package org.wordpress.android.viewmodel.gif

import androidx.paging.PositionalDataSource.LoadInitialCallback
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.gif.GifPickerViewModel.EmptyDisplayMode
import org.wordpress.android.viewmodel.gif.GifPickerViewModel.SelectionBarUiModel
import org.wordpress.android.viewmodel.gif.GifPickerViewModel.State
import java.util.Random
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class GifPickerViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: GifPickerViewModel

    private val dataSourceFactory = mock<GifPickerDataSourceFactory>()
    private val mediaFetcher = mock<GifMediaFetcher>()
    private val analyticsTracker = mock<AnalyticsTrackerWrapper>()

    @Mock private lateinit var networkUtils: NetworkUtilsWrapper

    private val selectionBarUiModelResults = mutableListOf<SelectionBarUiModel>()

    @Before
    fun setUp() {
        selectionBarUiModelResults.clear()
        viewModel = GifPickerViewModel(
                dataSourceFactory = dataSourceFactory,
                networkUtils = networkUtils,
                mediaFetcher = mediaFetcher,
                analyticsTrackerWrapper = analyticsTracker
        )
        viewModel.selectionBarUiModel.observeForever { if (it != null) selectionBarUiModelResults.add(it) }
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    private fun startSingleSelectionViewModel() {
        viewModel.start(site = mock(), isMultiSelectEnabled = false)
    }

    private fun startMultiSelectionViewModel() {
        viewModel.start(site = mock(), isMultiSelectEnabled = true)
    }

    @Test
    fun testMultiSelectionBarUiModel() {
        startMultiSelectionViewModel()

        assertThat(selectionBarUiModelResults).hasSize(1)

        // initial state when starting GIF picker
        val initialUiState = selectionBarUiModelResults[0]

        assertThat(initialUiState.isMultiselectEnabled).isTrue()
        assertThat(initialUiState.isVisible).isFalse()
        assertThat(initialUiState.numberOfSelectedImages).isEqualTo(0)

        // selecting one GIF should show selection bar and bump the number of selected images
        val firstMediaViewModel = createGifMediaViewModel()
        viewModel.toggleSelected(firstMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(2)

        val singleGifSelectedState = selectionBarUiModelResults[1]

        assertThat(singleGifSelectedState.isMultiselectEnabled).isTrue()
        assertThat(singleGifSelectedState.isVisible).isTrue()
        assertThat(singleGifSelectedState.numberOfSelectedImages).isEqualTo(1)

        // selecting another GIF will bump the number of selected images by one in visible selection bar
        val secondMediaViewModel = createGifMediaViewModel()
        viewModel.toggleSelected(secondMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(3)

        val multipleGifsSelectedState = selectionBarUiModelResults[2]

        assertThat(multipleGifsSelectedState.isMultiselectEnabled).isTrue()
        assertThat(multipleGifsSelectedState.isVisible).isTrue()
        assertThat(multipleGifsSelectedState.numberOfSelectedImages).isEqualTo(2)

        // deselecting a GIF will reduce the number of selected images but will not hide the bar
        viewModel.toggleSelected(firstMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(4)

        val deselectedGifState = selectionBarUiModelResults[3]

        assertThat(deselectedGifState.isMultiselectEnabled).isTrue()
        assertThat(deselectedGifState.isVisible).isTrue()
        assertThat(deselectedGifState.numberOfSelectedImages).isEqualTo(1)

        // deselecting the last GIF should hide the bar and reduce the number of selected images to 0
        viewModel.toggleSelected(secondMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(5)

        val nothingSelectedState = selectionBarUiModelResults[4]

        assertThat(nothingSelectedState.isMultiselectEnabled).isTrue()
        assertThat(nothingSelectedState.isVisible).isFalse()
        assertThat(nothingSelectedState.numberOfSelectedImages).isEqualTo(0)
    }

    @Test
    fun `when setting a mediaViewModel as selected, it adds that to the selected list`() {
        startMultiSelectionViewModel()
        val mediaViewModel = createGifMediaViewModel()

        viewModel.toggleSelected(mediaViewModel)

        with(viewModel.selectedMediaViewModelList) {
            assertThat(value).hasSize(1)
            assertThat(value).containsValue(mediaViewModel)
        }
    }

    @Test
    fun `when setting a mediaViewModel as selected, it updates the isSelected and selectedNumber`() {
        startMultiSelectionViewModel()
        val mediaViewModel = createGifMediaViewModel()

        viewModel.toggleSelected(mediaViewModel)

        assertThat(mediaViewModel.isSelected.value).isTrue()
        assertThat(mediaViewModel.selectionNumber.value).isEqualTo(1)
    }

    @Test
    fun `when toggling an already selected mediaViewModel, it gets deselected and removed from the selected list`() {
        startMultiSelectionViewModel()
        // Arrange
        val mediaViewModel = createGifMediaViewModel()
        viewModel.toggleSelected(mediaViewModel)

        // Act
        viewModel.toggleSelected(mediaViewModel)

        // Assert
        assertThat(mediaViewModel.isSelected.value).isFalse()
        assertThat(mediaViewModel.selectionNumber.value).isNull()

        assertThat(viewModel.selectedMediaViewModelList.value).isEmpty()
    }

    @Test
    fun `when deselecting a mediaViewModel, it rebuilds the selectedNumbers so they are continuous`() {
        startMultiSelectionViewModel()
        // Arrange
        val alpha = createGifMediaViewModel()
        val bravo = createGifMediaViewModel()
        val charlie = createGifMediaViewModel()
        val delta = createGifMediaViewModel()

        listOf(alpha, bravo, charlie, delta).forEach(viewModel::toggleSelected)

        // Make sure the selection numbers have the values tht we expect. These get updated later.
        assertThat(charlie.selectionNumber.value).isEqualTo(3)
        assertThat(delta.selectionNumber.value).isEqualTo(4)

        // Act
        // Deselect the second one in the list
        viewModel.toggleSelected(bravo)

        // Assert
        with(viewModel.selectedMediaViewModelList) {
            assertThat(value).hasSize(3)
            assertThat(value).doesNotContainValue(bravo)
            assertThat(value).containsValues(alpha, charlie, delta)
        }

        // Charlie and Delta should have moved up because Bravo was deselected
        assertThat(charlie.selectionNumber.value).isEqualTo(2)
        assertThat(delta.selectionNumber.value).isEqualTo(3)
    }

    @Test
    fun `when the searchQuery is changed, it clears the selected mediaViewModel list`() {
        startMultiSelectionViewModel()
        // Arrange
        val mediaViewModel = createGifMediaViewModel()
        viewModel.toggleSelected(mediaViewModel)

        // Act
        runBlocking {
            viewModel.search(query = "dummy", immediately = true).join()
        }

        // Assert
        assertThat(viewModel.selectedMediaViewModelList.value).isEmpty()
    }

    @Test
    fun `when searching, the empty view should be immediately set to hidden`() {
        startMultiSelectionViewModel()
        // Arrange
        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.VISIBLE_NO_SEARCH_QUERY)

        // Act
        runBlocking {
            viewModel.search("dummy", immediately = true).join()
        }

        // Assert
        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.HIDDEN)
    }

    @Test
    fun `when search results are empty, the empty view should be visible and says there are no results`() {
        startMultiSelectionViewModel()
        // Arrange
        val dataSource = mock<GifPickerDataSource>()

        whenever(dataSourceFactory.create()).thenReturn(dataSource)
        whenever(dataSourceFactory.searchQuery).thenReturn("dummy")

        val callbackCaptor = argumentCaptor<LoadInitialCallback<GifMediaViewModel>>()
        doNothing().whenever(dataSource).loadInitial(any(), callbackCaptor.capture())

        // Observe mediaViewModelPagedList so the DataSourceFactory will be activated and perform API requests
        viewModel.mediaViewModelPagedList.observeForever { }

        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.VISIBLE_NO_SEARCH_QUERY)

        // Act
        viewModel.search("dummy")
        // Emulate that the API responded with an empty result
        callbackCaptor.lastValue.onResult(emptyList(), 0, 0)

        // Assert
        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.VISIBLE_NO_SEARCH_RESULTS)

        verify(dataSource, times(1)).loadInitial(any(), any())
    }

    @Test
    fun `when the initial load fails, the empty view should show a network error`() {
        startMultiSelectionViewModel()
        // Arrange
        val dataSource = mock<GifPickerDataSource>()

        whenever(dataSourceFactory.create()).thenReturn(dataSource)
        whenever(dataSourceFactory.initialLoadError).thenReturn(mock())

        val callbackCaptor = argumentCaptor<LoadInitialCallback<GifMediaViewModel>>()
        doNothing().whenever(dataSource).loadInitial(any(), callbackCaptor.capture())

        // Observe mediaViewModelPagedList so the DataSourceFactory will be activated and perform API requests
        viewModel.mediaViewModelPagedList.observeForever { }

        // Act
        viewModel.search("dummy")
        // Along with mocking initialLoadError above, this emulate that the API responded with an error
        callbackCaptor.lastValue.onResult(emptyList(), 0, 0)

        // Assert
        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.VISIBLE_NETWORK_ERROR)

        verify(dataSource, times(1)).loadInitial(any(), any())
    }

    @Test
    fun `when download is successful, it posts the saved MediaModel objects`() {
        startMultiSelectionViewModel()
        // Arrange
        val expectedResult = listOf(createMediaModel(), createMediaModel())

        runBlocking {
            whenever(mediaFetcher.fetchAndSave(any(), any())).thenReturn(expectedResult)
        }

        // Act
        runBlocking {
            viewModel.downloadSelected().join()
        }

        // Assert
        assertThat(viewModel.state.value).isEqualTo(State.FINISHED)

        with(checkNotNull(viewModel.downloadResult.value)) {
            assertThat(mediaModels).hasSize(expectedResult.size)
            assertThat(mediaModels).isEqualTo(expectedResult)

            assertThat(errorMessageStringResId).isNull()
        }
    }

    @Test
    fun `when download fails due to network error, it posts an error string resource id`() {
        startMultiSelectionViewModel()
        // Arrange
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)

        // Act
        runBlocking { viewModel.downloadSelected().join() }

        // Assert
        with(checkNotNull(viewModel.downloadResult.value)) {
            assertThat(errorMessageStringResId).isNotNull()
            assertThat(mediaModels).isNull()
        }
    }

    @Test
    fun `when download fails, it posts an error string resource id`() {
        startMultiSelectionViewModel()
        // Arrange
        runBlocking {
            whenever(mediaFetcher.fetchAndSave(any(), any())).then { throw Exception("Oh no!") }
        }

        // Act
        runBlocking {
            viewModel.downloadSelected().join()
        }

        // Assert
        with(checkNotNull(viewModel.downloadResult.value)) {
            assertThat(errorMessageStringResId).isNotNull()
            assertThat(mediaModels).isNull()
        }
    }

    @Test
    fun `when download fails, it allows the user to try again`() {
        startMultiSelectionViewModel()
        // Arrange
        runBlocking {
            whenever(mediaFetcher.fetchAndSave(any(), any())).then { throw Exception("Oh no!") }
        }

        // Act
        runBlocking {
            viewModel.downloadSelected().join()
        }

        // Assert that State is sent back to IDLE because we'll allow the user to try again
        assertThat(viewModel.state.value).isEqualTo(State.IDLE)
    }

    @Test
    fun `when the State is already FINISHED, it no longer allows selecting new items`() {
        startMultiSelectionViewModel()
        // Arrange
        runBlocking {
            whenever(mediaFetcher.fetchAndSave(any(), any())).thenReturn(emptyList())
        }

        // Act
        runBlocking {
            viewModel.downloadSelected().join()
        }
        check(viewModel.state.value == State.FINISHED)

        viewModel.toggleSelected(createGifMediaViewModel())

        // Assert
        assertThat(viewModel.selectedMediaViewModelList.value).isNull()
    }

    @Test
    fun `when the State is already FINISHED, it no longer allows searching`() {
        startMultiSelectionViewModel()
        // Arrange
        runBlocking {
            whenever(mediaFetcher.fetchAndSave(any(), any())).thenReturn(emptyList())
        }

        // Act
        runBlocking {
            viewModel.downloadSelected().join()
        }
        check(viewModel.state.value == State.FINISHED)

        viewModel.search("excalibur")

        // Assert
        assertThat(dataSourceFactory.searchQuery).isBlank()
    }

    // testing single selection picker related logic

    @Test
    fun testSingleSelectionBarUiModel() {
        startSingleSelectionViewModel()

        assertThat(selectionBarUiModelResults).hasSize(1)

        // initial state when starting GIF picker
        val initialUiState = selectionBarUiModelResults[0]

        assertThat(initialUiState.isMultiselectEnabled).isFalse()
        assertThat(initialUiState.isVisible).isFalse()
        assertThat(initialUiState.numberOfSelectedImages).isEqualTo(0)

        // selecting one GIF should show selection bar and bump the number of selected images
        val firstMediaViewModel = createGifMediaViewModel()
        viewModel.toggleSelected(firstMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(2)

        val singleGifSelectedState = selectionBarUiModelResults[1]

        assertThat(singleGifSelectedState.isMultiselectEnabled).isFalse()
        assertThat(singleGifSelectedState.isVisible).isTrue()
        assertThat(singleGifSelectedState.numberOfSelectedImages).isEqualTo(1)

        // selecting another GIF will keep the bar visible but will bot bump the number of selected images
        val secondMediaViewModel = createGifMediaViewModel()
        viewModel.toggleSelected(secondMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(3)

        val multipleGifsSelectedState = selectionBarUiModelResults[2]

        assertThat(multipleGifsSelectedState.isMultiselectEnabled).isFalse()
        assertThat(multipleGifsSelectedState.isVisible).isTrue()
        assertThat(multipleGifsSelectedState.numberOfSelectedImages).isEqualTo(1)

        // deselecting a GIF will reduce the number of selected images to 0 and hide the bar
        viewModel.toggleSelected(secondMediaViewModel)

        assertThat(selectionBarUiModelResults).hasSize(4)

        val deselectedGifState = selectionBarUiModelResults[3]

        assertThat(deselectedGifState.isMultiselectEnabled).isFalse()
        assertThat(deselectedGifState.isVisible).isFalse()
        assertThat(deselectedGifState.numberOfSelectedImages).isEqualTo(0)
    }

    @Test
    fun `when setting a mediaViewModel as selected, previously selected mediaViewModel is unselected`() {
        val firstMediaModel = createGifMediaViewModel()
        val secondMediaModel = createGifMediaViewModel()

        viewModel.toggleSelected(firstMediaModel)

        with(viewModel.selectedMediaViewModelList) {
            assertThat(value).hasSize(1)
            assertThat(value).containsValue(firstMediaModel)
        }

        viewModel.toggleSelected(secondMediaModel)

        with(viewModel.selectedMediaViewModelList) {
            assertThat(value).hasSize(1)
            assertThat(value).containsValue(secondMediaModel)
        }
    }

    private fun createMediaModel() = MediaModel().apply {
        id = Random().nextInt()
    }

    private fun createGifMediaViewModel() = MutableGifMediaViewModel(
            id = UUID.randomUUID().toString(),
            thumbnailUri = mock(),
            largeImageUri = mock(),
            previewImageUri = mock(),
            title = UUID.randomUUID().toString()
    )
}
