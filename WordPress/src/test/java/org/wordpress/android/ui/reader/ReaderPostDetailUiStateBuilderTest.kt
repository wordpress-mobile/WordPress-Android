package org.wordpress.android.ui.reader

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderCommentList
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.models.ReaderSimplePost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.utils.FeaturedImageUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.utils.ThreadedCommentsUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.CommentSnippetData
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.Loading
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.ExcerptFooterUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.ReaderPostFeaturedImageUiState
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.BUTTON
import org.wordpress.android.ui.reader.views.uistates.CommentItemType.COMMENT
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.HtmlUtilsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailUiStateBuilderTest : BaseUnitTest() {
    private lateinit var builder: ReaderPostDetailUiStateBuilder
    @Mock lateinit var postUiStateBuilder: ReaderPostUiStateBuilder
    @Mock lateinit var headerViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder
    @Mock lateinit var featuredImageUtils: FeaturedImageUtils
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var context: Context
    @Mock lateinit var resources: Resources
    @Mock lateinit var htmlUtilsWrapper: HtmlUtilsWrapper
    @Mock lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock lateinit var readerSimplePost: ReaderSimplePost
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var gravatarUtilsWrapper: GravatarUtilsWrapper
    @Mock lateinit var threadedCommentsUtils: ThreadedCommentsUtils
    private lateinit var dummyRelatedPosts: ReaderSimplePostList

    private var dummySourceReaderPost = ReaderPost().apply {
        this.blogId = 1L
        this.feedId = 2L
        this.blogName = "blog name"
    }
    private val dummyOnRelatedPostItemClicked: (Long, Long, Boolean) -> Unit = { _, _, _ -> }
    private val dummyonCommentSnippetClicked: (Long, Long) -> Unit = { _, _ -> }
    private val dummyFeaturedImageUrl = "/image/url"
    private val dummyVisitPostLinkText = "visit post"
    private val dummyDisplayPixelHeight = 100

    @Before
    fun setUp() = test {
        dummyRelatedPosts = ReaderSimplePostList().apply { add(readerSimplePost) }

        builder = ReaderPostDetailUiStateBuilder(
                headerViewUiStateBuilder,
                postUiStateBuilder,
                featuredImageUtils,
                readerUtilsWrapper,
                displayUtilsWrapper,
                contextProvider,
                htmlUtilsWrapper,
                htmlMessageUtils,
                dateTimeUtilsWrapper,
                gravatarUtilsWrapper,
                threadedCommentsUtils,
                resourceProvider
        )
    }

    /* READER POST FEATURED IMAGE */
    @Test
    fun `given featured image should be shown, when post ui is built, then featured image exists`() = test {
        val postUiState = buildPostUiState(shouldShowFeaturedImage = true)

        assertThat(postUiState.featuredImageUiState).isEqualTo(
                ReaderPostFeaturedImageUiState(
                        blogId = dummySourceReaderPost.blogId,
                        url = dummyFeaturedImageUrl,
                        height = (dummyDisplayPixelHeight * READER_POST_FEATURED_IMAGE_HEIGHT_PERCENT).toInt()
                )
        )
    }

    @Test
    fun `given featured image should not be shown, when post ui is built, then featured image does not exists`() =
            test {
                val postUiState = buildPostUiState(shouldShowFeaturedImage = false)

                assertThat(postUiState.featuredImageUiState).isNull()
            }

    /* EXCERPT FOOTER */
    @Test
    fun `given excerpt is shown, when post ui is built, then excerpt footer exists`() = test {
        val readerPost = mock<ReaderPost>()
        whenever(readerPost.blogName).thenReturn("blog name")
        whenever(readerPost.url).thenReturn("url")
        whenever(readerPost.shouldShowExcerpt()).thenReturn(true)

        val postUiState = buildPostUiState(readerPost = readerPost)

        assertThat(postUiState.excerptFooterUiState).isEqualTo(
                ExcerptFooterUiState(
                        visitPostExcerptFooterLinkText = UiStringText(dummyVisitPostLinkText),
                        postLink = readerPost.url
                )
        )
    }

    @Test
    fun `given excerpt is not shown, when post ui is built, then excerpt footer does not exists`() = test {
        val readerPost = mock<ReaderPost>()
        whenever(readerPost.shouldShowExcerpt()).thenReturn(false)

        val postUiState = buildPostUiState(readerPost = readerPost)

        assertThat(postUiState.excerptFooterUiState).isNull()
    }

    /* RELATED POSTS */
    @Test
    fun `when local related posts ui is built, then source post site name exists in header label`() = test {
        val relatedPostsUiState = buildRelatedPostsUiState(isGlobal = false)

        assertThat(relatedPostsUiState.headerLabel).isEqualTo(
                UiStringResWithParams(
                        R.string.reader_label_local_related_posts,
                        listOf(UiStringText(dummySourceReaderPost.blogName))
                )
        )
    }

    @Test
    fun `when global related posts ui is built, then global related posts header label exists`() = test {
        val relatedPostsUiState = buildRelatedPostsUiState(isGlobal = true)

        assertThat(relatedPostsUiState.headerLabel).isEqualTo(UiStringRes(R.string.reader_label_global_related_posts))
    }

    @Test
    fun `given empty related posts, when related posts ui is built, then related post cards are empty`() = test {
        val relatedPostsUiState = buildRelatedPostsUiState(relatedPosts = ReaderSimplePostList())

        assertThat(relatedPostsUiState.cards).isEmpty()
    }

    @Test
    fun `given related posts, when related posts ui is built, then related post cards exist`() = test {
        val relatedPostsUiState = buildRelatedPostsUiState()

        assertThat(relatedPostsUiState.cards).isNotEmpty
    }

    @Test
    fun `given related post with title, when related posts ui is built, then related post title exists`() = test {
        val title = "title"
        whenever(readerSimplePost.hasTitle()).thenReturn(true)
        whenever(readerSimplePost.title).thenReturn(title)

        val relatedPostsUiState = buildRelatedPostsUiState()

        assertThat(relatedPostsUiState.cards?.first()?.title).isEqualTo(UiStringText(title))
    }

    @Test
    fun `given related post without title, when related posts ui is built, then related post title does not exists`() =
            test {
                whenever(readerSimplePost.hasTitle()).thenReturn(false)

                val relatedPostsUiState = buildRelatedPostsUiState()

                assertThat(relatedPostsUiState.cards?.first()?.title).isNull()
            }

    @Test
    fun `given related post with excerpt, when related posts ui is built, then excerpt exists`() = test {
        val excerpt = "excerpt"
        whenever(readerSimplePost.hasExcerpt()).thenReturn(true)
        whenever(readerSimplePost.excerpt).thenReturn(excerpt)

        val relatedPostsUiState = buildRelatedPostsUiState()

        assertThat(relatedPostsUiState.cards?.first()?.excerpt).isEqualTo(UiStringText(excerpt))
    }

    @Test
    fun `given related post without excerpt, when related posts ui is built, then excerpt does not exists`() =
            test {
                whenever(readerSimplePost.hasExcerpt()).thenReturn(false)

                val relatedPostsUiState = buildRelatedPostsUiState()

                assertThat(relatedPostsUiState.cards?.first()?.excerpt).isNull()
            }

    @Test
    fun `given related post with featured image url, when related posts ui is built, then featured image exists`() =
            test {
                val url = "/featured/image/url"
                whenever(readerSimplePost.getFeaturedImageForDisplay(any(), any())).thenReturn(url)

                val relatedPostsUiState = buildRelatedPostsUiState()

                assertThat(relatedPostsUiState.cards?.first()?.featuredImageUrl).isEqualTo(url)
            }

    @Test
    fun `given related post without featured image url, when related posts ui is built, then featured image exists`() =
            test {
                whenever(readerSimplePost.getFeaturedImageForDisplay(any(), any())).thenReturn(null)

                val relatedPostsUiState = buildRelatedPostsUiState()

                assertThat(relatedPostsUiState.cards?.first()?.featuredImageUrl).isNull()
            }

    @Test
    fun `follow conversation shown if post isWP and comments are open`() {
        dummySourceReaderPost.apply {
            isExternal = false
            isCommentsOpen = true
        }

        val snippetUiState = builder.buildCommentSnippetUiState(
                Loading,
                dummySourceReaderPost,
                dummyonCommentSnippetClicked
        )

        assertThat(snippetUiState.showFollowConversation).isTrue()
    }

    @Test
    fun `snippet contains comment and button`() {
        dummySourceReaderPost.apply {
            isExternal = false
            isCommentsOpen = true
        }

        whenever(contextProvider.getContext()).thenReturn(context)
        whenever(context.resources).thenReturn(resources)
        whenever(resources.getDimensionPixelSize(anyInt())).thenReturn(10)

        whenever(dateTimeUtilsWrapper.dateFromIso8601(anyString())).thenReturn(Date())
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(anyOrNull())).thenReturn("")

        whenever(gravatarUtilsWrapper.fixGravatarUrl(anyString(), anyInt())).thenReturn("")

        val comment = ReaderComment().apply {
            authorName = ""
            published = "2021-12-08T16:58:20-08:00"
            authorAvatar = "https://avatar"
            text = "test"
        }

        val commentsList = ReaderCommentList().apply {
            add(comment)
        }

        val snippetUiState = builder.buildCommentSnippetUiState(
                CommentSnippetData(comments = commentsList),
                dummySourceReaderPost,
                dummyonCommentSnippetClicked
        )

        assertThat(snippetUiState.snippetItems.first().type).isEqualTo(COMMENT)
        assertThat(snippetUiState.snippetItems[1].type).isEqualTo(BUTTON)
    }

    private fun buildRelatedPostsUiState(
        relatedPosts: ReaderSimplePostList = dummyRelatedPosts,
        isGlobal: Boolean = false
    ) = builder.mapRelatedPostsToUiState(
            sourcePost = dummySourceReaderPost,
            relatedPosts = relatedPosts,
            isGlobal = isGlobal,
            onItemClicked = dummyOnRelatedPostItemClicked
    )

    private fun buildPostUiState(
        readerPost: ReaderPost? = null,
        shouldShowFeaturedImage: Boolean = false
    ): ReaderPostDetailsUiState {
        val post = readerPost ?: dummySourceReaderPost

        if (post.shouldShowExcerpt()) {
            val dummyLinkHexColor = "#FFFFFF"
            whenever(htmlUtilsWrapper.colorResToHtmlColor(anyOrNull(), any())).thenReturn(dummyLinkHexColor)
            whenever(
                    htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                            R.string.reader_excerpt_link,
                            "<font color='" + dummyLinkHexColor + "'>" + post.blogName + "</font>"
                    )
            ).thenReturn(dummyVisitPostLinkText)
        }

        whenever(featuredImageUtils.shouldAddFeaturedImage(any())).thenReturn(shouldShowFeaturedImage)
        whenever(displayUtilsWrapper.getWindowPixelHeight()).thenReturn(dummyDisplayPixelHeight)
        whenever(readerUtilsWrapper.getResizedImageUrl(any(), any(), any(), any(), any()))
                .thenReturn(dummyFeaturedImageUrl)

        whenever(headerViewUiStateBuilder.mapPostToUiState(any(), any(), any(), any())).thenReturn(mock())
        whenever(postUiStateBuilder.mapPostToActions(any(), any())).thenReturn(mock())

        return builder.mapPostToUiState(
                post = post,
                moreMenuItems = null,
                onButtonClicked = mock(),
                onBlogSectionClicked = mock(),
                onFollowClicked = mock(),
                onTagItemClicked = mock()
        )
    }
}
