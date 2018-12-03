package org.wordpress.android.viewmodel.giphy

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class GiphyPickerViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val viewModel = GiphyPickerViewModel(dataSourceFactory = mock())

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

    private fun createGiphyMediaViewModel() = MutableGiphyMediaViewModel(
            id = UUID.randomUUID().toString(),
            thumbnailUri = mock(),
            previewImageUri = mock(),
            title = UUID.randomUUID().toString()
    )
}
