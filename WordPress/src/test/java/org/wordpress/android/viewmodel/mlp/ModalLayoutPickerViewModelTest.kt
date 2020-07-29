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
        viewModel.init()
        assertThat(viewModel.listItems.value!!.size).isGreaterThan(0)
    }

    @Test
    fun `the modal layout picker contains the title as a first item when initialized`() {
        viewModel.init()
        assertThat(viewModel.listItems.value!!.first().type).isEqualTo(TITLE)
    }

    @Test
    fun `the modal layout picker contains the subtitle as a second item when initialized`() {
        viewModel.init()
        assertThat(viewModel.listItems.value!!.get(1).type).isEqualTo(SUBTITLE)
    }

    @Test
    fun `modal layout picker is shown when triggered`() {
        viewModel.init()
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
    fun `modal layout picker header is visible when the user scrolls up`() {
        viewModel.init()
        viewModel.setHeaderTitleVisibility(true)
        assertThat(viewModel.isHeaderVisible.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `modal layout picker header is gone when the user scrolls down`() {
        viewModel.init()
        viewModel.setHeaderTitleVisibility(false)
        assertThat(viewModel.isHeaderVisible.value!!.peekContent()).isEqualTo(false)
    }

    @Test
    fun `when the create page is triggered the modal layout picker is dismissed`() {
        viewModel.init()
        viewModel.createPage()
        assertThat(viewModel.isModalLayoutPickerShowing.value!!.peekContent()).isEqualTo(false)
    }

    @Test
    fun `when the create page is triggered the page creation flow starts`() {
        viewModel.init()
        viewModel.createPage()
        verify(onCreateNewPageRequestedObserver).onChanged(anyOrNull())
    }
}
