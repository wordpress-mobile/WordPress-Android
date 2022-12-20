package org.wordpress.android.viewmodel.history

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsPayload
import org.wordpress.android.viewmodel.history.HistoryViewModel.HistoryListStatus

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HistoryViewModelTest : BaseUnitTest() {
    private val defaultPost = PostModel().apply {
        setId(1_569)
    }
    private val postStore = mock<PostStore> {
        on { getPostByLocalPostId(eq(defaultPost.id)) } doReturn defaultPost
    }
    private val dispatcher = mock<Dispatcher>()

    @Test
    fun `when started, it will load the post and fetch the revisions`() {
        // Arrange
        val viewModel = createHistoryVieWModel()

        assertThat(viewModel.post.value).isNull()

        // Act
        viewModel.create(localPostId = defaultPost.id, site = SiteModel())

        // Assert
        assertThat(viewModel.post.value).isEqualTo(defaultPost)

        verify(postStore, times(1)).getPostByLocalPostId(eq(defaultPost.id))
        verify(dispatcher, times(1)).dispatch(argWhere {
            it.type == PostAction.FETCH_REVISIONS &&
                    it.payload is FetchRevisionsPayload &&
                    (it.payload as FetchRevisionsPayload).post === defaultPost
        })

        assertThat(viewModel.listStatus.value).isEqualTo(HistoryListStatus.FETCHING)
    }

    @Test
    fun `when the post cannot be loaded, it will show an empty list`() {
        // Arrange
        val viewModel = createHistoryVieWModel()
        val invalidPostLocalId = 9_014_134

        // Act
        viewModel.create(localPostId = invalidPostLocalId, site = SiteModel())

        // Assert
        verify(postStore, times(1)).getPostByLocalPostId(eq(invalidPostLocalId))

        assertThat(viewModel.post.value).isNull()
        assertThat(viewModel.revisions.value).isEmpty()
        assertThat(viewModel.listStatus.value).isEqualTo(HistoryListStatus.DONE)
    }

    private fun createHistoryVieWModel() = HistoryViewModel(
            dispatcher = dispatcher,
            resourceProvider = mock(),
            networkUtils = mock(),
            postStore = postStore,
            uiDispatcher = testDispatcher(),
            bgDispatcher = testDispatcher(),
            connectionStatus = mock()
    )
}
