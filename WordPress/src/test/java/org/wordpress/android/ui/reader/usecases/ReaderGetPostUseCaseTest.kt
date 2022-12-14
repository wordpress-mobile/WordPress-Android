package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType

@ExperimentalCoroutinesApi
class ReaderGetPostUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    private val readerPostId = 1L
    private val readerBlogId = 2L
    private val discoverPostId = 3L
    private val discoverBlogId = 4L

    private val readerPost = ReaderPost().apply {
        postId = readerPostId
        blogId = readerBlogId
    }

    private val readerDiscoverSourcePost = ReaderPost().apply {
        postId = discoverPostId
        blogId = discoverBlogId
    }

    private lateinit var useCase: ReaderGetPostUseCase

    @Before
    fun setUp() {
        useCase = ReaderGetPostUseCase(
                coroutinesTestRule.testDispatcher,
                readerPostTableWrapper
        )
    }

    @Test
    fun `given feed, when reader post is retrieved, feed post is returned`() = test {
        whenever(
                readerPostTableWrapper.getFeedPost(
                        blogId = readerBlogId,
                        postId = readerPostId,
                        excludeTextColumn = false
                )
        )
                .thenReturn(readerPost)

        val result = useCase.get(blogId = readerBlogId, postId = readerPostId, isFeed = true)

        assertThat(result).isEqualTo(Pair<ReaderPost?, Boolean>(readerPost, true))
    }

    @Test
    fun `given blog, when reader post is retrieved, blog post is returned`() = test {
        whenever(
                readerPostTableWrapper.getBlogPost(
                        blogId = readerBlogId,
                        postId = readerPostId,
                        excludeTextColumn = false
                )
        )
                .thenReturn(readerPost)

        val result = useCase.get(blogId = readerBlogId, postId = readerPostId, isFeed = false)

        assertThat(result).isEqualTo(Pair<ReaderPost?, Boolean>(readerPost, false))
    }

    @Test
    fun `given editor pick discover post is found, when reader post is retrieved, discover source post returned`() =
            test {
                val readerPost = createPost(discoverType = DiscoverType.EDITOR_PICK)
                whenever(
                        readerPostTableWrapper.getBlogPost(
                                blogId = readerBlogId,
                                postId = readerPostId,
                                excludeTextColumn = false
                        )
                ).thenReturn(readerPost)
                whenever(
                        readerPostTableWrapper.getBlogPost(
                                blogId = readerDiscoverSourcePost.blogId,
                                postId = readerDiscoverSourcePost.postId,
                                excludeTextColumn = false
                        )
                ).thenReturn(readerDiscoverSourcePost)

                val result = useCase.get(blogId = readerBlogId, postId = readerPostId, isFeed = false)

                assertThat(result).isEqualTo(Pair<ReaderPost?, Boolean>(readerDiscoverSourcePost, false))
            }

    @Test
    fun `given non editor pick reader post is found, when reader post is retrieved, reader post is returned`() =
            test {
                val readerPost = createPost(discoverType = DiscoverType.SITE_PICK)
                whenever(
                        readerPostTableWrapper.getBlogPost(
                                blogId = readerBlogId,
                                postId = readerPostId,
                                excludeTextColumn = false
                        )
                ).thenReturn(readerPost)

                val result = useCase.get(blogId = readerBlogId, postId = readerPostId, isFeed = false)

                assertThat(result).isEqualTo(Pair<ReaderPost?, Boolean>(readerPost, false))
            }

    private fun createPost(discoverType: DiscoverType = DiscoverType.OTHER): ReaderPost {
        val post = spy(readerPost)
        // The ReaderPost contains business logic and accesses static classes. Using spy() allows us to use it in tests.
        whenever(post.isDiscoverPost).thenReturn(true)
        val mockedDiscoverData: ReaderPostDiscoverData = mock()
        whenever(post.discoverData).thenReturn(mockedDiscoverData)
        whenever(mockedDiscoverData.discoverType).thenReturn(discoverType)
        whenever(mockedDiscoverData.postId).thenReturn(readerDiscoverSourcePost.postId)
        whenever(mockedDiscoverData.blogId).thenReturn(readerDiscoverSourcePost.blogId)
        return post
    }
}
