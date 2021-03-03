package org.wordpress.android.ui.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
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
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.models.ReaderSimplePost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailUiStateBuilder
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailUiStateBuilderTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var builder: ReaderPostDetailUiStateBuilder
    @Mock lateinit var postUiStateBuilder: ReaderPostUiStateBuilder
    @Mock lateinit var headerViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder
    @Mock private lateinit var readerSimplePost: ReaderSimplePost
    private lateinit var localRelatedPosts: ReaderSimplePostList
    private lateinit var globalRelatedPosts: ReaderSimplePostList

    private var dummyReaderPost = ReaderPost().apply {
        this.blogId = 1L
        this.feedId = 2L
        this.blogName = "blog name"
    }
    private val dummyOnRelatedPostItemClicked: (Long, Long, Boolean) -> Unit = { _, _, _ -> }
    private val dummyOnRelatedPostFollowClicked: (Long, String) -> Unit = { _, _ -> }

    @Before
    fun setUp() = test {
        whenever(readerSimplePost.title).thenReturn("")
        whenever(readerSimplePost.featuredImageUrl).thenReturn("")
        localRelatedPosts = ReaderSimplePostList().apply { add(readerSimplePost) }
        globalRelatedPosts = ReaderSimplePostList().apply { add(readerSimplePost) }

        builder = ReaderPostDetailUiStateBuilder(
                headerViewUiStateBuilder,
                postUiStateBuilder
        )
    }

    @Test
    fun `when related posts ui is built, then site name exists`() = test {
        val relatedPostsUiState = init(relatedPosts = globalRelatedPosts, isGlobal = true)

        assertThat(relatedPostsUiState.siteName).isNotNull
    }

    @Test
    fun `given empty related posts, when related posts ui is built, then related post cards are empty`() = test {
        val relatedPostsUiState = init(relatedPosts = ReaderSimplePostList())

        assertThat(relatedPostsUiState.cards).isEmpty()
    }

    @Test
    fun `given local related posts, when related posts ui is built, then related post cards exist`() = test {
        val relatedPostsUiState = init(relatedPosts = localRelatedPosts, isGlobal = false)

        assertThat(relatedPostsUiState.cards).isNotEmpty
    }

    @Test
    fun `given global related posts, when related posts ui is built, then related post cards exists`() = test {
        val relatedPostsUiState = init(relatedPosts = globalRelatedPosts, isGlobal = true)

        assertThat(relatedPostsUiState.cards).isNotEmpty
    }

    @Test
    fun `given local related posts, when related posts ui is built, then follow button does not exist`() = test {
        val relatedPostsUiState = init(relatedPosts = localRelatedPosts, isGlobal = false)

        assertThat(relatedPostsUiState.cards?.first()?.followButtonUiState).isNull()
    }

    @Test
    fun `given global related posts, when related posts ui is built, then follow button exists`() = test {
        val relatedPostsUiState = init(relatedPosts = globalRelatedPosts, isGlobal = true)

        assertThat(relatedPostsUiState.cards?.first()?.followButtonUiState).isNotNull
    }

    @Test
    fun `given related post with title, when related posts ui is built, then related post title exists`() = test {
        val relatedPostsUiState = init(relatedPosts = localRelatedPosts, isGlobal = false)

        assertThat(relatedPostsUiState.cards?.first()?.title).isEqualTo(UiStringText(readerSimplePost.title))
    }

    @Test
    fun `given related post without title, when related posts ui is built, then related post title does not exists`() =
            test {
                whenever(readerSimplePost.title).thenReturn(null)

                val relatedPostsUiState = init(relatedPosts = globalRelatedPosts, isGlobal = true)

                assertThat(relatedPostsUiState.cards?.first()?.title).isNull()
            }

    @Test
    fun `given related post with featured image url, when related posts ui is built, then featured image exists`() =
            test {
                val relatedPostsUiState = init(relatedPosts = localRelatedPosts, isGlobal = false)

                assertThat(relatedPostsUiState.cards?.first()?.featuredImageUrl)
                        .isEqualTo(readerSimplePost.featuredImageUrl)
            }

    private fun init(
        relatedPosts: ReaderSimplePostList,
        isGlobal: Boolean = false
    ) = builder.mapRelatedPostsToUiState(
            sourcePost = dummyReaderPost,
            relatedPosts = relatedPosts,
            isGlobal = isGlobal,
            onRelatedPostItemClicked = dummyOnRelatedPostItemClicked,
            onRelatedPostFollowClicked = dummyOnRelatedPostFollowClicked
    )
}
