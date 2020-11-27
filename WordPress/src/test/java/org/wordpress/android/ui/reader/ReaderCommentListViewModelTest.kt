package org.wordpress.android.ui.reader

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.reader.ReaderCommentListViewModel.ScrollPosition
import org.wordpress.android.util.config.FollowUnfollowCommentsFeatureConfig
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class ReaderCommentListViewModelTest : BaseUnitTest() {
    @Mock lateinit var followCommentsHandler: ReaderFollowCommentsHandler
    @Mock lateinit var followUnfollowCommentsFeatureConfig: FollowUnfollowCommentsFeatureConfig

    private lateinit var viewModel: ReaderCommentListViewModel

    @Before
    fun setUp() {
        viewModel = ReaderCommentListViewModel(
                followCommentsHandler,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                followUnfollowCommentsFeatureConfig
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
