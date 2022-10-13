package org.wordpress.android.ui.reader.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.ReaderEvents.RelatedPostsUpdated
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Failed
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderFetchRelatedPostsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    lateinit var useCase: ReaderFetchRelatedPostsUseCase
    @Mock lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var readerPost: ReaderPost

    @Before
    fun setup() {
        useCase = ReaderFetchRelatedPostsUseCase(networkUtilsWrapper, readerPostActionsWrapper)

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when related posts are fetched, then no network is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isEqualTo(Failed.NoNetwork)
    }

    @Test
    fun `given related posts fetch succeeds, when related posts are fetched, then success is returned`() = test {
        val successEvent = RelatedPostsUpdated(readerPost, mock(), mock(), true)
        whenever(readerPostActionsWrapper.requestRelatedPosts(readerPost))
                .then { useCase.onRelatedPostUpdated(successEvent) }

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isInstanceOf(FetchRelatedPostsState.Success::class.java)
    }

    @Test
    fun `given related posts fetch fails, when related posts are fetched, then request failed is returned`() = test {
        val failedEvent = RelatedPostsUpdated(readerPost, mock(), mock(), false)
        whenever(readerPostActionsWrapper.requestRelatedPosts(readerPost))
                .then { useCase.onRelatedPostUpdated(failedEvent) }

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isEqualTo(Failed.RequestFailed)
    }
}
