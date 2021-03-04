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
import org.wordpress.android.R
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.models.ReaderSimplePost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailUiStateBuilder
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailUiStateBuilderTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var builder: ReaderPostDetailUiStateBuilder
    @Mock lateinit var postUiStateBuilder: ReaderPostUiStateBuilder
    @Mock lateinit var headerViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var readerSimplePost: ReaderSimplePost
    private lateinit var dummyRelatedPosts: ReaderSimplePostList

    private var dummySourceReaderPost = ReaderPost().apply {
        this.blogId = 1L
        this.feedId = 2L
        this.blogName = "blog name"
    }
    private val dummyOnRelatedPostItemClicked: (Long, Long, Boolean) -> Unit = { _, _, _ -> }

    @Before
    fun setUp() = test {
        whenever(readerSimplePost.title).thenReturn("")
        whenever(readerSimplePost.featuredImageUrl).thenReturn("")
        dummyRelatedPosts = ReaderSimplePostList().apply { add(readerSimplePost) }

        builder = ReaderPostDetailUiStateBuilder(
                headerViewUiStateBuilder,
                postUiStateBuilder,
                resourceProvider
        )
    }

    @Test
    fun `when local related posts ui is built, then source post site name exists in header label`() = test {
        val relatedPostsUiState = init(isGlobal = false)

        assertThat(relatedPostsUiState.headerLabel).isEqualTo(
                UiStringResWithParams(
                        R.string.reader_label_local_related_posts,
                        listOf(UiStringText(dummySourceReaderPost.blogName))
                )
        )
    }

    @Test
    fun `when global related posts ui is built, then global related posts header label exists`() = test {
        val relatedPostsUiState = init(isGlobal = true)

        assertThat(relatedPostsUiState.headerLabel).isEqualTo(UiStringRes(R.string.reader_label_global_related_posts))
    }

    @Test
    fun `given empty related posts, when related posts ui is built, then related post cards are empty`() = test {
        val relatedPostsUiState = init(relatedPosts = ReaderSimplePostList())

        assertThat(relatedPostsUiState.cards).isEmpty()
    }

    @Test
    fun `given related posts, when related posts ui is built, then related post cards exist`() = test {
        val relatedPostsUiState = init()

        assertThat(relatedPostsUiState.cards).isNotEmpty
    }

    @Test
    fun `given related post with title, when related posts ui is built, then related post title exists`() = test {
        val relatedPostsUiState = init()

        assertThat(relatedPostsUiState.cards?.first()?.title).isEqualTo(UiStringText(readerSimplePost.title))
    }

    @Test
    fun `given related post without title, when related posts ui is built, then related post title does not exists`() =
            test {
                whenever(readerSimplePost.title).thenReturn(null)

                val relatedPostsUiState = init()

                assertThat(relatedPostsUiState.cards?.first()?.title).isNull()
            }

    @Test
    fun `given related post with featured image url, when related posts ui is built, then featured image exists`() =
            test {
                val relatedPostsUiState = init()

                assertThat(relatedPostsUiState.cards?.first()?.featuredImageUrl)
                        .isEqualTo(readerSimplePost.featuredImageUrl)
            }

    private fun init(
        relatedPosts: ReaderSimplePostList = dummyRelatedPosts,
        isGlobal: Boolean = false
    ) = builder.mapRelatedPostsToUiState(
            sourcePost = dummySourceReaderPost,
            relatedPosts = relatedPosts,
            isGlobal = isGlobal,
            onItemClicked = dummyOnRelatedPostItemClicked
    )
}
