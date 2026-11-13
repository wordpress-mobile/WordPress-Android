package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState.Failed
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class ReaderFetchRelatedPostsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var readerPostRepository: ReaderPostRepository

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private val readerPost = ReaderPost().apply {
        blogId = 1
        postId = 2
    }

    private lateinit var useCase: ReaderFetchRelatedPostsUseCase

    @Before
    fun setup() {
        useCase = ReaderFetchRelatedPostsUseCase(networkUtilsWrapper, readerPostRepository)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `given no network, when related posts are fetched, then no network is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isEqualTo(Failed.NoNetwork)
    }

    @Test
    fun `given related posts fetch succeeds, when related posts are fetched, then they are returned`() = test {
        val local = ReaderSimplePostList()
        val global = ReaderSimplePostList()
        whenever(readerPostRepository.fetchRelatedPosts(readerPost)).thenReturn(local to global)

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isEqualTo(FetchRelatedPostsState.Success(local, global))
    }

    @Test
    fun `given related posts fetch fails, when related posts are fetched, then request failed is returned`() = test {
        whenever(readerPostRepository.fetchRelatedPosts(readerPost)).thenReturn(null)

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isEqualTo(Failed.RequestFailed)
    }

    @Test
    fun `given a fetch for the same post is running, when fetched again, then already running is returned`() = test {
        whenever(readerPostRepository.fetchRelatedPosts(readerPost)).doSuspendableAnswer { awaitCancellation() }
        val firstRequest = launch { useCase.fetchRelatedPosts(readerPost) }
        testScheduler.runCurrent()

        val result = useCase.fetchRelatedPosts(readerPost)

        assertThat(result).isEqualTo(FetchRelatedPostsState.AlreadyRunning)
        firstRequest.cancelAndJoin()
    }
}
