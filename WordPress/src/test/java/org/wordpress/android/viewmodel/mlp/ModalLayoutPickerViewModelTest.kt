package org.wordpress.android.viewmodel.mlp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ContentUiState

@RunWith(MockitoJUnitRunner::class)
class ModalLayoutPickerViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ModalLayoutPickerViewModel

    @Mock lateinit var onCreateNewPageRequestedObserver: Observer<Unit>

    @Before
    fun setUp() {
        viewModel = ModalLayoutPickerViewModel(
                NoDelayCoroutineDispatcher()
        )
        viewModel.onCreateNewPageRequested.observeForever(
                onCreateNewPageRequestedObserver
        )
    }

    @Test
    fun `when modal layout picker starts the categories are loaded`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).categories.size).isGreaterThan(0)
    }

    @Test
    fun `when modal layout picker starts the layouts are loaded`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).layoutCategories.size).isGreaterThan(0)
    }

    @Test
    fun `modal layout picker is shown when triggered`() {
        viewModel.show()
        assertThat(viewModel.isModalLayoutPickerShowing.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `modal layout picker is dismissed when the user hits the back button`() {
        viewModel.init()
        viewModel.dismiss()
        assertThat(viewModel.isModalLayoutPickerShowing.value!!.peekContent()).isEqualTo(false)
    }

    @Test
    fun `when the create page is triggered the page creation flow starts`() {
        viewModel.init()
        viewModel.createPage()
        verify(onCreateNewPageRequestedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `when modal layout picker starts no layout is selected`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the user taps on a layout the layout is selected`() {
        viewModel.init()
        viewModel.layoutTapped("about-1")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedLayoutSlug).isEqualTo("about-1")
    }

    @Test
    fun `when the user taps on a selected layout the layout is deselected`() {
        viewModel.init()
        viewModel.layoutTapped("about-1")
        viewModel.layoutTapped("about-1")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when the modal layout picker is dismissed the layout is deselected`() {
        viewModel.init()
        viewModel.layoutTapped("about-1")
        viewModel.dismiss()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedLayoutSlug).isNull()
    }

    @Test
    fun `when modal layout picker starts no category is selected`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedCategoriesSlugs).isEmpty()
    }

    @Test
    fun `when the user taps on a category the category is selected`() {
        viewModel.init()
        viewModel.categoryTapped("about")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedCategoriesSlugs)
                .contains("about")
    }

    @Test
    fun `when the user taps on a selected category the category is deselected`() {
        viewModel.init()
        viewModel.categoryTapped("about")
        viewModel.categoryTapped("about")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedCategoriesSlugs)
                .doesNotContain("about")
    }

    @Test
    fun `when the modal layout picker is dismissed the category is deselected`() {
        viewModel.init()
        viewModel.categoryTapped("about")
        viewModel.dismiss()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).selectedCategoriesSlugs).isEmpty()
    }

    fun `when no layout is selected the create blank page button is visible`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).buttonsUiState.createBlankPageVisible)
                .isEqualTo(true)
    }

    @Test
    fun `when a layout is selected the create blank page button is not visible`() {
        viewModel.init()
        viewModel.layoutTapped("about-1")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).buttonsUiState.createBlankPageVisible)
                .isEqualTo(false)
    }

    @Test
    fun `when no layout is selected the create page button is not visible`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).buttonsUiState.createPageVisible)
                .isEqualTo(false)
    }

    @Test
    fun `when a layout is selected the create page button is visible`() {
        viewModel.init()
        viewModel.layoutTapped("about-1")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).buttonsUiState.createPageVisible)
                .isEqualTo(true)
    }

    @Test
    fun `when no layout is selected the preview button is not visible`() {
        viewModel.init()
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).buttonsUiState.previewVisible)
                .isEqualTo(false)
    }

    @Test
    fun `when a layout is selected the preview button is visible`() {
        viewModel.init()
        viewModel.layoutTapped("about-1")
        assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).buttonsUiState.previewVisible)
                .isEqualTo(true)
    }
}
