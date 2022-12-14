package org.wordpress.android.ui.reader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition
import org.wordpress.android.viewmodel.Event

@ExperimentalCoroutinesApi
class ReaderCommentListViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ReaderCommentListViewModel

    @Before
    fun setUp() {
        viewModel = ReaderCommentListViewModel(
                testDispatcher()
        )
    }

    @Test
    fun `emits scroll event on scroll`() {
        var scrollEvent: Event<ScrollPosition>? = null
        viewModel.scrollTo.observeForever {
            scrollEvent = it
        }

        val expectedPosition = 10
        val isSmooth = true

        viewModel.scrollToPosition(expectedPosition, isSmooth)

        val scrollPosition = scrollEvent?.getContentIfNotHandled()!!

        assertThat(scrollPosition.isSmooth).isEqualTo(isSmooth)
        assertThat(scrollPosition.position).isEqualTo(expectedPosition)
    }
}
