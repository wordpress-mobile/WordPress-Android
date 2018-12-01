package org.wordpress.android.viewmodel.giphy

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.viewmodel.giphy.GiphyPickerViewModel.State
import java.util.Random
import java.util.UUID

class GiphyPickerViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: GiphyPickerViewModel

    private val dataSourceFactory = mock<GiphyPickerDataSourceFactory>()
    private val mediaFetcher = mock<GiphyMediaFetcher>()

    @Before
    fun setUp() {
        viewModel = GiphyPickerViewModel(dataSourceFactory = dataSourceFactory, mediaFetcher = mediaFetcher)
        viewModel.setup(site = mock())
    }

    @Test
    fun `when setting a mediaViewModel as selected, it adds that to the selected list`() {
        val mediaViewModel = createGiphyMediaViewModel()

        viewModel.toggleSelected(mediaViewModel)

        with(viewModel.selectedMediaViewModelList) {
            assertThat(value).hasSize(1)
            assertThat(value).containsValue(mediaViewModel)
        }
    }

    @Test
    fun `when setting a mediaViewModel as selected, it updates the isSelected and selectedNumber`() {
        val mediaViewModel = createGiphyMediaViewModel()

        viewModel.toggleSelected(mediaViewModel)

        assertThat(mediaViewModel.isSelected.value).isTrue()
        assertThat(mediaViewModel.selectionNumber.value).isEqualTo(1)
    }

    @Test
    fun `when toggling an already selected mediaViewModel, it gets deselected and removed from the selected list`() {
        // Arrange
        val mediaViewModel = createGiphyMediaViewModel()
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
        val alpha = createGiphyMediaViewModel()
        val bravo = createGiphyMediaViewModel()
        val charlie = createGiphyMediaViewModel()
        val delta = createGiphyMediaViewModel()

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
        val mediaViewModel = createGiphyMediaViewModel()
        viewModel.toggleSelected(mediaViewModel)

        // Act
        viewModel.search("dummy")

        // Assert
        assertThat(viewModel.selectedMediaViewModelList.value).isEmpty()
    }

    @Test
    fun `when download is successful, it posts the saved MediaModel objects`() {
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
            val mediaModels = checkNotNull(first)
            val errorResId = second

            assertThat(mediaModels).hasSize(expectedResult.size)
            assertThat(mediaModels).isEqualTo(expectedResult)

            assertThat(errorResId).isNull()
        }
    }

    @Test
    fun `when download fails, it posts an error string resource id`() {
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
            val mediaModels = first
            val errorResId = second

            assertThat(errorResId).isNotNull()
            assertThat(mediaModels).isNull()
        }
    }

    @Test
    fun `when download fails, it allows the user to try again`() {
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
        // Arrange
        runBlocking {
            whenever(mediaFetcher.fetchAndSave(any(), any())).thenReturn(emptyList())
        }

        // Act
        runBlocking {
            viewModel.downloadSelected().join()
        }
        check(viewModel.state.value == State.FINISHED)

        viewModel.toggleSelected(createGiphyMediaViewModel())

        // Assert
        assertThat(viewModel.selectedMediaViewModelList.value).isNull()
    }

    @Test
    fun `when the State is already FINISHED, it no longer allows searching`() {
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
        verify(dataSourceFactory, never()).setSearchQuery(any())
    }

    private fun createMediaModel() = MediaModel().apply {
        id = Random().nextInt()
    }

    private fun createGiphyMediaViewModel() = MutableGiphyMediaViewModel(
            id = UUID.randomUUID().toString(),
            thumbnailUri = mock(),
            largeImageUri = mock(),
            title = UUID.randomUUID().toString()
    )
}
