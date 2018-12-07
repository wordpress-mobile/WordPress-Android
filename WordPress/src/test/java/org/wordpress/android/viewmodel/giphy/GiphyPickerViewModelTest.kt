package org.wordpress.android.viewmodel.giphy

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.paging.PositionalDataSource.LoadInitialCallback
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.viewmodel.giphy.GiphyPickerViewModel.EmptyDisplayMode

class GiphyPickerViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val dataSourceFactory = mock<GiphyPickerDataSourceFactory>()
    private lateinit var viewModel: GiphyPickerViewModel

    @Before
    fun setup() {
        viewModel = GiphyPickerViewModel(dataSourceFactory = dataSourceFactory)
    }

    @Test
    fun `when setting a mediaViewModel as selected, it adds that to the selected list`() {
        val mediaViewModel = MutableGiphyMediaViewModel(id = "01", thumbnailUri = mock(), title = "title")

        viewModel.toggleSelected(mediaViewModel)

        with(viewModel.selectedMediaViewModelList) {
            assertThat(value).hasSize(1)
            assertThat(value).containsValue(mediaViewModel)
        }
    }

    @Test
    fun `when setting a mediaViewModel as selected, it updates the isSelected and selectedNumber`() {
        val mediaViewModel = MutableGiphyMediaViewModel(id = "01", thumbnailUri = mock(), title = "title")

        viewModel.toggleSelected(mediaViewModel)

        assertThat(mediaViewModel.isSelected.value).isTrue()
        assertThat(mediaViewModel.selectionNumber.value).isEqualTo(1)
    }

    @Test
    fun `when toggling an already selected mediaViewModel, it gets deselected and removed from the selected list`() {
        // Arrange
        val mediaViewModel = MutableGiphyMediaViewModel(id = "01", thumbnailUri = mock(), title = "title")
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
        // Arrange
        val alpha = MutableGiphyMediaViewModel(id = "01", thumbnailUri = mock(), title = "alpha")
        val bravo = MutableGiphyMediaViewModel(id = "02", thumbnailUri = mock(), title = "bravo")
        val charlie = MutableGiphyMediaViewModel(id = "03", thumbnailUri = mock(), title = "charlie")
        val delta = MutableGiphyMediaViewModel(id = "04", thumbnailUri = mock(), title = "delta")

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
        // Arrange
        val mediaViewModel = MutableGiphyMediaViewModel(id = "01", thumbnailUri = mock(), title = "title")
        viewModel.toggleSelected(mediaViewModel)

        // Act
        viewModel.search("dummy")

        // Assert
        assertThat(viewModel.selectedMediaViewModelList.value).isEmpty()
    }

    @Test
    fun `when searching, the empty view should be immediately set to hidden`() {
        // Arrange
        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.VISIBLE_NO_SEARCH_QUERY)

        // Act
        viewModel.search("dummy")

        // Assert
        assertThat(viewModel.emptyDisplayMode.value).isEqualTo(EmptyDisplayMode.HIDDEN)
    }

    @Test
    fun `when search results are empty, the empty view should be visible and says there are no results`() {
        // Arrange
        val dataSource = mock<GiphyPickerDataSource>()

        whenever(dataSourceFactory.create()).thenReturn(dataSource)
        whenever(dataSourceFactory.searchQuery).thenReturn("dummy")

        val callbackCaptor = argumentCaptor<LoadInitialCallback<GiphyMediaViewModel>>()
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
        // Arrange
        val dataSource = mock<GiphyPickerDataSource>()

        whenever(dataSourceFactory.create()).thenReturn(dataSource)
        whenever(dataSourceFactory.searchQuery).thenReturn("dummy")
        whenever(dataSourceFactory.initialLoadError).thenReturn(mock())

        val callbackCaptor = argumentCaptor<LoadInitialCallback<GiphyMediaViewModel>>()
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
}
