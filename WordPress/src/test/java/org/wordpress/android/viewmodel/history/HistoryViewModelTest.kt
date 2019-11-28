package org.wordpress.android.viewmodel.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsPayload
import org.wordpress.android.viewmodel.history.HistoryViewModel.HistoryListStatus

@RunWith(MockitoJUnitRunner::class)
class HistoryViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()

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

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun createHistoryVieWModel() = HistoryViewModel(
            dispatcher = dispatcher,
            resourceProvider = mock(),
            networkUtils = mock(),
            postStore = postStore,
            uiDispatcher = Dispatchers.Unconfined,
            bgDispatcher = Dispatchers.Unconfined,
            connectionStatus = mock()
    )
}
