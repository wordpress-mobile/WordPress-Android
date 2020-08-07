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
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.CATEGORIES
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.SUBTITLE
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.TITLE
import org.wordpress.android.util.NoDelayCoroutineDispatcher

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
    fun `the modal layout picker list is populated when initialized`() {
        viewModel.init(false)
        assertThat(viewModel.listItems.value!!.size).isGreaterThan(0)
    }

    @Test
    fun `the modal layout picker contains the title as a first item when initialized in portrait mode`() {
        viewModel.init(false)
        assertThat(viewModel.listItems.value!!.first().type).isEqualTo(TITLE)
    }

    @Test
    fun `the modal layout picker contains the subtitle as a second item when initialized in portrait mode`() {
        viewModel.init(false)
        assertThat(viewModel.listItems.value!!.get(1).type).isEqualTo(SUBTITLE)
    }

    @Test
    fun `the modal layout picker contains the categories bar as a first item when initialized in landscape mode`() {
        viewModel.init(true)
        assertThat(viewModel.listItems.value!!.first().type).isEqualTo(CATEGORIES)
    }

    @Test
    fun `modal layout picker is shown when triggered`() {
        viewModel.init(false)
        viewModel.show()
        assertThat(viewModel.isModalLayoutPickerShowing.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `modal layout picker is dismissed when the user hits the back button`() {
        viewModel.init(false)
        viewModel.dismiss()
        assertThat(viewModel.isModalLayoutPickerShowing.value!!.peekContent()).isEqualTo(false)
    }

    @Test
    fun `modal layout picker header is visible when the user scrolls up in portrait mode`() {
        viewModel.init(false)
        viewModel.setHeaderTitleVisibility(true)
        assertThat(viewModel.isHeaderVisible.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `modal layout picker header is gone when the user scrolls down in portrait mode`() {
        viewModel.init(false)
        viewModel.setHeaderTitleVisibility(false)
        assertThat(viewModel.isHeaderVisible.value!!.peekContent()).isEqualTo(false)
    }

    @Test
    fun `when the create page is triggered the page creation flow starts`() {
        viewModel.init(false)
        viewModel.createPage()
        verify(onCreateNewPageRequestedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `when modal layout picker starts the layouts are loaded`() {
        viewModel.init(false)
        assertThat(viewModel.listItems.value?.size).isGreaterThan(0)
    }

    @Test
    fun `when modal layout picker starts no layout is selected`() {
        viewModel.init(false)
        assertThat(viewModel.selectedLayoutSlug.value).isNull()
    }

    @Test
    fun `when the user taps on a layout the layout is selected`() {
        viewModel.init(false)
        viewModel.layoutTapped("about-1")
        assertThat(viewModel.selectedLayoutSlug.value).isEqualTo("about-1")
    }

    @Test
    fun `when the user taps on a selected layout the layout is deselected`() {
        viewModel.init(false)
        viewModel.layoutTapped("about-1")
        viewModel.layoutTapped("about-1")
        assertThat(viewModel.selectedLayoutSlug.value).isNull()
    }

    @Test
    fun `when the modal layout picker is dismissed the layout is deselected`() {
        viewModel.init(false)
        viewModel.layoutTapped("about-1")
        viewModel.dismiss()
        assertThat(viewModel.selectedLayoutSlug.value).isNull()
    }
}
