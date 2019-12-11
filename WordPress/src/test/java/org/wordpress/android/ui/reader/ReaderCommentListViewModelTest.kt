package org.wordpress.android.ui.reader

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class ReaderCommentListViewModelTest : BaseUnitTest() {
    private val viewModel: ReaderCommentListViewModel = ReaderCommentListViewModel(TEST_DISPATCHER)

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
