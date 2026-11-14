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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.repository.FetchPostResult
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import java.net.HttpURLConnection

@ExperimentalCoroutinesApi
class ReaderFetchPostUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var readerPostRepository: ReaderPostRepository
    private lateinit var useCase: ReaderFetchPostUseCase

    private val postId = 1L
    private val blogId = 2L

    @Before
    fun setUp() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        useCase = ReaderFetchPostUseCase(networkUtilsWrapper, readerPostRepository)
    }

    @Test
    fun `given no network, when reader post is fetched, then no network is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.NoNetwork)
    }

    @Test
    fun `given feed, when reader post is fetched, then the feed post is fetched`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, true)).thenReturn(FetchPostResult.Success(mock()))

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = true)

        assertThat(result).isEqualTo(Success)
    }

    @Test
    fun `given success response, when reader post is fetched, then success is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false)).thenReturn(FetchPostResult.Success(mock()))

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Success)
    }

    @Test
    fun `given http not found status code, when reader post is fetched, then post not found is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false))
            .thenReturn(FetchPostResult.Failure(HttpURLConnection.HTTP_NOT_FOUND))

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.PostNotFound)
    }

    @Test
    fun `given http unauthorised status code, when reader post is fetched, then not authorised is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false))
            .thenReturn(FetchPostResult.Failure(HttpURLConnection.HTTP_UNAUTHORIZED))

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.NotAuthorised)
    }

    @Test
    fun `given http forbidden status code, when reader post is fetched, then not authorised is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false))
            .thenReturn(FetchPostResult.Failure(HttpURLConnection.HTTP_FORBIDDEN))

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.NotAuthorised)
    }

    @Test
    fun `given unknown status code, when reader post is fetched, then request failed is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false)).thenReturn(FetchPostResult.Failure(500))

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.RequestFailed)
    }

    @Test
    fun `given the request never responds, when reader post is fetched, then request failed is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false)).doSuspendableAnswer { awaitCancellation() }
        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.RequestFailed)
    }

    @Test
    fun `given a request for the same post is running, when fetched again, then already running is returned`() = test {
        whenever(readerPostRepository.fetchPost(blogId, postId, false)).doSuspendableAnswer { awaitCancellation() }
        val firstRequest = launch { useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false) }

        val secondResult = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(secondResult).isEqualTo(AlreadyRunning)
        firstRequest.cancelAndJoin()
    }
}
