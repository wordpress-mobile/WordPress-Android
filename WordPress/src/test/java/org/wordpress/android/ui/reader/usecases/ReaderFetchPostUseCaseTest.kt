package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Failed
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import java.net.HttpURLConnection

private const val REQUEST_BLOG_LISTENER_PARAM_POSITION = 2

@InternalCoroutinesApi
class ReaderFetchPostUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper
    private lateinit var useCase: ReaderFetchPostUseCase

    private val postId = 1L
    private val blogId = 2L

    @Before
    fun setUp() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        useCase = ReaderFetchPostUseCase(networkUtilsWrapper, readerPostActionsWrapper)
    }

    @Test
    fun `given no network, when reader post is fetched, then no network is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.NoNetwork)
    }

    @Test
    fun `given feed, when reader post is fetched, then feed post is requested`() = test {
        whenever(readerPostActionsWrapper.requestFeedPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onSuccess(null)
        }

        useCase.fetchPost(postId = postId, blogId = blogId, isFeed = true)

        verify(readerPostActionsWrapper)
                .requestFeedPost(feedId = eq(blogId), postId = eq(postId), requestListener = any())
    }

    @Test
    fun `given blog, when reader post is fetched, then blog post is requested`() = test {
        whenever(readerPostActionsWrapper.requestBlogPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onSuccess(null)
        }

        useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        verify(readerPostActionsWrapper)
                .requestBlogPost(blogId = eq(blogId), postId = eq(postId), requestListener = any())
    }

    @Test
    fun `given success response, when reader post is fetched, then success is returned`() = test {
        whenever(readerPostActionsWrapper.requestBlogPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onSuccess(null)
        }

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Success)
    }

    @Test
    fun `given http not found status code, when reader post is fetched, then post not found is returned`() = test {
        whenever(readerPostActionsWrapper.requestBlogPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onFailure(HttpURLConnection.HTTP_NOT_FOUND)
        }

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.PostNotFound)
    }

    @Test
    fun `given http unauthorised status code, when reader post is fetched, then not authorised is returned`() = test {
        whenever(readerPostActionsWrapper.requestBlogPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onFailure(HttpURLConnection.HTTP_UNAUTHORIZED)
        }

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.NotAuthorised)
    }

    @Test
    fun `given http forbidden status code, when reader post is fetched, then not authorised is returned`() = test {
        whenever(readerPostActionsWrapper.requestBlogPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onFailure(HttpURLConnection.HTTP_FORBIDDEN)
        }

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.NotAuthorised)
    }

    @Test
    fun `given unknown status code, when reader post is fetched, then request failed is returned`() = test {
        whenever(readerPostActionsWrapper.requestBlogPost(anyLong(), anyLong(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                    .onFailure(500)
        }

        val result = useCase.fetchPost(postId = postId, blogId = blogId, isFeed = false)

        assertThat(result).isEqualTo(Failed.RequestFailed)
    }
}
