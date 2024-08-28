package org.wordpress.android.ui.reader.discover

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderCardType
import org.wordpress.android.models.ReaderCardType.DEFAULT
import org.wordpress.android.models.ReaderCardType.GALLERY
import org.wordpress.android.models.ReaderCardType.PHOTO
import org.wordpress.android.models.ReaderCardType.VIDEO
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.Organization.NO_ORGANIZATION
import org.wordpress.android.ui.Organization.P2
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.BLOG_PREVIEW
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStyleGreen
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStyleOrange
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStylePurple
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStyleYellow
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.models.ReaderImageList
import org.wordpress.android.ui.reader.utils.ReaderImageScanner
import org.wordpress.android.ui.reader.utils.ReaderImageScannerProvider
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.WPAvatarUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import java.util.Date

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostUiStateBuilderTest : BaseUnitTest() {
    // region Set-up
    private lateinit var builder: ReaderPostUiStateBuilder

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var urlUtilsWrapper: UrlUtilsWrapper

    @Mock
    lateinit var avatarUtilsWrapper: WPAvatarUtilsWrapper

    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    lateinit var readerImageScannerProvider: ReaderImageScannerProvider

    @Mock
    lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Before
    fun setUp() = test {
        builder = ReaderPostUiStateBuilder(
            accountStore,
            urlUtilsWrapper,
            avatarUtilsWrapper,
            dateTimeUtilsWrapper,
            readerImageScannerProvider,
            readerUtilsWrapper,
            testDispatcher()
        )
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(anyOrNull())).thenReturn("")
        whenever(avatarUtilsWrapper.rewriteAvatarUrlWithResource(anyOrNull(), anyInt())).thenReturn("")
        val imageScanner: ReaderImageScanner = mock()
        whenever(readerImageScannerProvider.createReaderImageScanner(anyOrNull(), anyBoolean()))
            .thenReturn(imageScanner)
        whenever(imageScanner.getImageList(anyInt(), anyInt())).thenReturn(ReaderImageList(false))
        whenever(accountStore.hasAccessToken()).thenReturn(true)
    }
    // endregion

    // region BLOG HEADER
    @Test
    fun `clicks on blog header are disabled on blog preview`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post, BLOG_PREVIEW)
        // Assert
        assertThat(uiState.blogSection.onClicked).isNull()
    }

    @Test
    fun `clicks on blog header are disabled on blog preview for new Ui`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post, BLOG_PREVIEW)
        // Assert
        assertThat(uiState.blogSection.onClicked).isNull()
    }

    @Test
    fun `clicks on blog header are enabled when not blog preview`() = test {
        // Arrange
        val post = createPost()
        ReaderPostListType.values().filter { it != BLOG_PREVIEW }.forEach {
            // Act
            val uiState = mapPostToUiState(post, it)
            // Assert
            assertThat(uiState.blogSection.onClicked).isNotNull
        }
    }

    @Test
    fun `clicks on blog header are enabled when not blog preview for new UI`() = test {
        // Arrange
        val post = createPost()
        ReaderPostListType.values().filter { it != BLOG_PREVIEW }.forEach {
            // Act
            val uiState = mapPostToUiState(post, it)
            // Assert
            assertThat(uiState.blogSection.onClicked).isNotNull
        }
    }

    @Test
    fun `p2 posts in the feed show author's avatar alongside site icon`() = test {
        // Arrange
        val p2post = createPost(isp2Post = true)
        val nonP2Post = createPost(isp2Post = false)
        // Act
        val p2UiState = mapPostToUiState(p2post)
        val nonP2UiState = mapPostToUiState(nonP2Post)
        // Assert
        assertThat(p2UiState.blogSection.isAuthorAvatarVisible).isTrue
        assertThat(nonP2UiState.blogSection.isAuthorAvatarVisible).isFalse
    }

    @Test
    fun `p2 posts in the feed show author's avatar alongside site icon for new UI`() = test {
        // Arrange
        val p2post = createPost(isp2Post = true)
        val nonP2Post = createPost(isp2Post = false)
        // Act
        val p2UiState = mapPostToUiState(p2post)
        val nonP2UiState = mapPostToUiState(nonP2Post)
        // Assert
        assertThat(p2UiState.blogSection.isAuthorAvatarVisible).isTrue
        assertThat(nonP2UiState.blogSection.isAuthorAvatarVisible).isFalse
    }

    // endregion

    // region VIDEO
    @Test
    fun `videoUrl gets initialized for video cards`() = test {
        // Arrange
        val post = createPost(cardType = VIDEO, featuredVideoUrl = "12345")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.fullVideoUrl).isEqualTo("12345")
    }

    @Test
    fun `videoUrl gets initialized for video cards for new UI`() = test {
        // Arrange
        val post = createPost(cardType = VIDEO, featuredVideoUrl = "12345")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.fullVideoUrl).isEqualTo("12345")
    }

    @Test
    fun `videoUrl does not get initialized for other than video cards`() = test {
        // Arrange
        val types = ReaderCardType.values()
        types.filter { it != VIDEO }.forEach {
            val post = createPost(cardType = it, featuredVideoUrl = "12345")
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.fullVideoUrl).isNull()
        }
    }

    @Test
    fun `videoUrl does not get initialized for other than video cards for new UI`() = test {
        // Arrange
        val types = ReaderCardType.values()
        types.filter { it != VIDEO }.forEach {
            val post = createPost(cardType = it, featuredVideoUrl = "12345")
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.fullVideoUrl).isNull()
        }
    }

    @Test
    fun `video overlay is displayed for video cards`() = test {
        // Arrange
        val post = createPost(cardType = VIDEO)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.videoOverlayVisibility).isTrue
    }

    @Test
    fun `video overlay is displayed for video cards for new UI`() = test {
        // Arrange
        val post = createPost(cardType = VIDEO)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.videoOverlayVisibility).isTrue
    }

    @Test
    fun `video overlay is not displayed for other than video cards`() = test {
        // Arrange
        val types = ReaderCardType.values()
        types.filter { it != VIDEO }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.videoOverlayVisibility).isFalse()
        }
    }

    @Test
    fun `video overlay is not displayed for other than video cards for new UI`() = test {
        // Arrange
        val types = ReaderCardType.values()
        types.filter { it != VIDEO }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.videoOverlayVisibility).isFalse()
        }
    }
    // endregion

    // region THUMBNAIL STRIP
    @Test
    fun `thumbnail strip is not empty for GALLERY`() = test {
        // Arrange
        val post = createPost(cardType = GALLERY)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.thumbnailStripSection).isNotNull
    }

    @Test
    fun `thumbnail strip is not empty for GALLERY for new UI`() = test {
        // Arrange
        val post = createPost(cardType = GALLERY)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.thumbnailStripSection).isNotNull
    }

    @Test
    fun `thumbnail strip is empty for other than GALLERY`() = test {
        // Arrange
        ReaderCardType.values().filter { it != GALLERY }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.thumbnailStripSection).isNull()
        }
    }

    @Test
    fun `thumbnail strip is empty for other than GALLERY for new UI`() = test {
        // Arrange
        ReaderCardType.values().filter { it != GALLERY }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.thumbnailStripSection).isNull()
        }
    }
    // endregion

    // region FEATURED IMAGE
    @Test
    fun `featured image is displayed for photo and default card types`() = test {
        // Arrange
        val dummyUrl = "12345"
        ReaderCardType.values().filter { it == PHOTO || it == DEFAULT }.forEach {
            val post = createPost(cardType = it, hasFeaturedImage = true, featuredImageUrlForDisplay = dummyUrl)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.featuredImageUrl).isEqualTo(dummyUrl)
        }
    }

    @Test
    fun `featured image is displayed for photo and default card types for new UI`() = test {
        // Arrange
        val dummyUrl = "12345"
        ReaderCardType.values().filter { it == PHOTO || it == DEFAULT }.forEach {
            val post = createPost(cardType = it, hasFeaturedImage = true, featuredImageUrlForDisplay = dummyUrl)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.featuredImageUrl).isEqualTo(dummyUrl)
        }
    }

    @Test
    fun `featured image is not displayed for other than photo and default card types`() = test {
        // Arrange
        ReaderCardType.values().filter { it != PHOTO && it != DEFAULT }.forEach {
            val post = createPost(cardType = it, hasFeaturedImage = true)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.featuredImageUrl).isNull()
        }
    }

    @Test
    fun `featured image is not displayed for other than photo and default card types for new UI`() = test {
        // Arrange
        ReaderCardType.values().filter { it != PHOTO && it != DEFAULT }.forEach {
            val post = createPost(cardType = it, hasFeaturedImage = true)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.featuredImageUrl).isNull()
        }
    }

    @Test
    fun `featured image is not displayed when hasFeaturedImage returns false`() = test {
        // Arrange
        val post = createPost(cardType = PHOTO, hasFeaturedImage = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.featuredImageUrl).isNull()
    }

    @Test
    fun `featured image is not displayed when hasFeaturedImage returns false for new UI`() = test {
        // Arrange
        val post = createPost(cardType = PHOTO, hasFeaturedImage = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.featuredImageUrl).isNull()
    }
    // endregion

    // region TITLE & EXCERPT
    @Test
    fun `title is displayed for other than PHOTO card type`() = test {
        // Arrange
        ReaderCardType.values().filter { it != PHOTO }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat((uiState.title as UiStringText).text).isEqualTo(post.title)
        }
    }

    @Test
    fun `title is displayed for all card types for new UI`() = test {
        // Arrange
        ReaderCardType.values().forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat((uiState.title as UiStringText).text).isEqualTo(post.title)
        }
    }

    @Test
    fun `title is not displayed when the post doesn't have a title for new UI`() = test {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasTitle = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.title).isNull()
    }

    @Test
    fun `excerpt is displayed for other than PHOTO card type`() = test {
        // Arrange
        ReaderCardType.values().filter { it != PHOTO }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.excerpt).isNotNull()
        }
    }

    @Test
    fun `excerpt is displayed for all card types for new UI`() = test {
        // Arrange
        ReaderCardType.values().forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.excerpt).isNotNull()
        }
    }

    @Test
    fun `excerpt is not displayed when the post doesn't have an excerpt`() = test {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasExcerpt = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.excerpt).isNull()
    }

    @Test
    fun `excerpt is not displayed when the post doesn't have an excerpt for new UI`() = test {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasExcerpt = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.excerpt).isNull()
    }
    // endregion

    // region BLOG NAME
    @Test
    fun `blog name is displayed for regular post`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogSection.blogName).isNotNull
    }

    @Test
    fun `blog name is displayed for regular post for new UI`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogSection.blogName).isNotNull
    }

    @Test
    fun `default blog name is displayed when the post doesn't have a blog name`() = test {
        // Arrange
        val post = createPost(hasBlogName = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat((uiState.blogSection.blogName as UiStringRes).stringRes).isEqualTo(R.string.untitled_in_parentheses)
    }

    @Test
    fun `default blog name is displayed when the post doesn't have a blog name for new UI`() = test {
        // Arrange
        val post = createPost(hasBlogName = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat((uiState.blogSection.blogName as UiStringRes).stringRes).isEqualTo(R.string.untitled_in_parentheses)
    }

    @Test
    fun `p2 posts in the feed show author's first name (or full name) alongside the blog name`() = test {
        // Arrange
        val postWithFirstName = createPost(
            isp2Post = true,
            blogName = "Fancy Blog",
            authorFirstName = "John",
            authorName = "John Smith"
        )
        val postWithoutFirstName = createPost(
            isp2Post = true,
            blogName = "Fancy Blog",
            authorFirstName = "",
            authorName = "John Smith"
        )
        // Act
        val firstNameUiState = mapPostToUiState(postWithFirstName)
        val fullNameUiState = mapPostToUiState(postWithoutFirstName)
        // Assert
        val firstNameBlog = firstNameUiState.blogSection.blogName as UiStringResWithParams
        assertThat(firstNameBlog.stringRes).isEqualTo(R.string.reader_author_with_blog_name)
        assertThat(firstNameBlog.params.size).isEqualTo(2)
        assertThat((firstNameBlog.params[0] as UiStringText).text).isEqualTo("John")
        assertThat((firstNameBlog.params[1] as UiStringText).text).isEqualTo("Fancy Blog")

        val fullNameBlog = fullNameUiState.blogSection.blogName as UiStringResWithParams
        assertThat(fullNameBlog.stringRes).isEqualTo(R.string.reader_author_with_blog_name)
        assertThat(fullNameBlog.params.size).isEqualTo(2)
        assertThat((fullNameBlog.params[0] as UiStringText).text).isEqualTo("John Smith")
        assertThat((fullNameBlog.params[1] as UiStringText).text).isEqualTo("Fancy Blog")
    }

    @Test
    fun `p2 posts in the feed show author's first name (or full name) alongside the blog name for new UI`() = test {
        // Arrange
        val postWithFirstName = createPost(
            isp2Post = true,
            blogName = "Fancy Blog",
            authorFirstName = "John",
            authorName = "John Smith"
        )
        val postWithoutFirstName = createPost(
            isp2Post = true,
            blogName = "Fancy Blog",
            authorFirstName = "",
            authorName = "John Smith"
        )
        // Act
        val firstNameUiState = mapPostToUiState(postWithFirstName)
        val fullNameUiState = mapPostToUiState(postWithoutFirstName)
        // Assert
        val firstNameBlog = firstNameUiState.blogSection.blogName as UiStringResWithParams
        assertThat(firstNameBlog.stringRes).isEqualTo(R.string.reader_author_with_blog_name)
        assertThat(firstNameBlog.params.size).isEqualTo(2)
        assertThat((firstNameBlog.params[0] as UiStringText).text).isEqualTo("John")
        assertThat((firstNameBlog.params[1] as UiStringText).text).isEqualTo("Fancy Blog")

        val fullNameBlog = fullNameUiState.blogSection.blogName as UiStringResWithParams
        assertThat(fullNameBlog.stringRes).isEqualTo(R.string.reader_author_with_blog_name)
        assertThat(fullNameBlog.params.size).isEqualTo(2)
        assertThat((fullNameBlog.params[0] as UiStringText).text).isEqualTo("John Smith")
        assertThat((fullNameBlog.params[1] as UiStringText).text).isEqualTo("Fancy Blog")
    }
    // endregion

    // region DATELINE
    @Test
    fun `builds dateline from post's display date`() = test {
        // Arrange
        val post = createPost()
        val dummyDate: Date = mock()
        whenever(post.getDisplayDate(dateTimeUtilsWrapper)).thenReturn(dummyDate)
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(dummyDate)).thenReturn("success")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogSection.dateLine).isEqualTo("success")
    }

    @Test
    fun `builds dateline from post's display date for new UI`() = test {
        // Arrange
        val post = createPost()
        val dummyDate: Date = mock()
        whenever(post.getDisplayDate(dateTimeUtilsWrapper)).thenReturn(dummyDate)
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(dummyDate)).thenReturn("success")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogSection.dateLine).isEqualTo("success")
    }
    // endregion

    // region LIKE BUTTON
    @Test
    fun `like button is enabled on regular posts`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isTrue
    }

    @Test
    fun `like button is enabled on regular posts for new UI`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isTrue
    }

    @Test
    fun `like button is disabled when the user is logged off`() = test {
        // Arrange
        val post = createPost()
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse
    }

    @Test
    fun `like button is disabled when the user is logged off for new UI`() = test {
        // Arrange
        val post = createPost()
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse
    }

    @Test
    fun `like button is disabled when likes are disabled on the post`() = test {
        // Arrange
        val post = createPost(isCanLikePost = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse
    }

    @Test
    fun `like button is disabled when likes are disabled on the post for new UI`() = test {
        // Arrange
        val post = createPost(isCanLikePost = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse
    }

    @Test
    fun `onButtonClicked listener is correctly assigned to likeAction`() = test {
        // Arrange
        val post = createPost()
        val onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock()
        val uiState = mapPostToUiState(post, onButtonClicked = onButtonClicked)
        // Act
        uiState.likeAction.onClicked!!.invoke(1L, 1L, LIKE)
        // Assert
        verify(onButtonClicked).invoke(1L, 1L, LIKE)
    }
    @Test
    fun `onButtonClicked listener is correctly assigned to likeAction for new UI`() = test {
        // Arrange
        val post = createPost()
        val onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock()
        val uiState = mapPostToUiState(post, onButtonClicked = onButtonClicked)
        // Act
        uiState.likeAction.onClicked!!.invoke(1L, 1L, LIKE)
        // Assert
        verify(onButtonClicked).invoke(1L, 1L, LIKE)
    }
    // endregion

    // region REBLOG BUTTON
    @Test
    fun `reblog button is enabled on regular posts`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isTrue
    }

    @Test
    fun `reblog button is enabled on regular posts for new UI`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isTrue
    }

    @Test
    fun `reblog button is disabled on private posts`() = test {
        // Arrange
        val post = createPost(isPrivate = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isFalse
    }

    @Test
    fun `reblog button is disabled on private posts for new UI`() = test {
        // Arrange
        val post = createPost(isPrivate = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isFalse
    }

    @Test
    fun `reblog button is disabled when the user is logged off`() = test {
        // Arrange
        val post = createPost()
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isFalse
    }

    @Test
    fun `reblog button is disabled when the user is logged off for new UI`() = test {
        // Arrange
        val post = createPost()
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isFalse
    }

    @Test
    fun `onButtonClicked listener is correctly assigned to reblogAction`() = test {
        // Arrange
        val post = createPost()
        val onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock()
        val uiState = mapPostToUiState(post, onButtonClicked = onButtonClicked)
        // Act
        uiState.reblogAction.onClicked!!.invoke(1L, 1L, REBLOG)
        // Assert
        verify(onButtonClicked).invoke(1L, 1L, REBLOG)
    }
    @Test
    fun `onButtonClicked listener is correctly assigned to reblogAction for new UI`() = test {
        // Arrange
        val post = createPost()
        val onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock()
        val uiState = mapPostToUiState(post, onButtonClicked = onButtonClicked)
        // Act
        uiState.reblogAction.onClicked!!.invoke(1L, 1L, REBLOG)
        // Assert
        verify(onButtonClicked).invoke(1L, 1L, REBLOG)
    }
    // endregion

    // region COMMENT BUTTON
    @Test
    fun `Comments button is enabled on regular posts`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isTrue
    }

    @Test
    fun `Comments button is enabled on regular posts for new UI`() = test {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isTrue
    }

    @Test
    fun `Comments button is disabled when comments are disabled on the post`() = test {
        // Arrange
        val post = createPost(isCommentsOpen = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Comments button is disabled when comments are disabled on the post for new UI`() = test {
        // Arrange
        val post = createPost(isCommentsOpen = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Comments button is disabled on non-wpcom posts`() = test {
        // Arrange
        val post = createPost(isWPCom = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Comments button is disabled on non-wpcom posts for new UI`() = test {
        // Arrange
        val post = createPost(isWPCom = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Comments button is disabled when the user is logged off and the post does not have any comments`() = test {
        // Arrange
        val post = createPost(numOfReplies = 0)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Comments button is disabled when the user is logged off and the post does not have any comments for new UI`() = test {
        // Arrange
        val post = createPost(numOfReplies = 0)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Comments button is enabled when the user is logged off but the post has some comments`() = test {
        // Arrange
        val post = createPost(numOfReplies = 1)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isTrue
    }

    @Test
    fun `Comments button is enabled when the user is logged off but the post has some comments for new UI`() = test {
        // Arrange
        val post = createPost(numOfReplies = 1)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isTrue
    }

    @Test
    fun `Comments button is disabled on discover posts`() = test {
        // Arrange
        val post = createPost(isDiscoverPost = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Comments button is disabled on discover posts for new UI`() = test {
        // Arrange
        val post = createPost(isDiscoverPost = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse
    }

    @Test
    fun `Count on Comments button corresponds to number of comments on the post`() = test {
        // Arrange
        val numReplies = 15
        val post = createPost(numOfReplies = numReplies)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.count).isEqualTo(numReplies)
    }
    // endregion

    // region INTERACTION SECTION
    @Test
    fun `Count on comments section corresponds to number of comments on the post for new UI`() = test {
        // Arrange
        val numReplies = 15
        val post = createPost(numOfReplies = numReplies)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.interactionSection.commentCount).isEqualTo(numReplies)
    }

    @Test
    fun `Count on likes section corresponds to number of likes on the post for new UI`() = test {
        // Arrange
        val numLikes = 15
        val post = createPost(numOfLikes = numLikes)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.interactionSection.likeCount).isEqualTo(numLikes)
    }
    // endregion

    @Test
    fun `Ensures that there are 5 interests within the uiState even though the ReaderTagList contains 6`() = test {
        // arrange
        val expectedInterestListSize = 5
        val currentReaderTagListSize = 6

        val readerTagList = createReaderTagList(currentReaderTagListSize)

        // act
        val result = builder.mapTagListToReaderInterestUiState(readerTagList, mock())

        // assert
        assertThat(result.interest.size).isEqualTo(expectedInterestListSize)
    }

    @Test
    fun `Ensures that there are three interests within the uiState when the ReaderTagList contains 3`() = test {
        // arrange
        val expectedInterestListSize = 3
        val currentReaderTagListSize = 3

        val readerTagList = createReaderTagList(currentReaderTagListSize)

        // act
        val result = builder.mapTagListToReaderInterestUiState(readerTagList, mock())

        // assert
        assertThat(result.interest.size).isEqualTo(expectedInterestListSize)
    }

    @Test
    fun `given a tag list with 4 elements, then the uiState contains each style`() = test {
        // arrange
        val currentReaderTagListSize = 4
        val expectedListStyles = listOf(ChipStyleGreen, ChipStylePurple, ChipStyleYellow, ChipStyleOrange)

        val readerTagList = createReaderTagList(currentReaderTagListSize)

        // act
        val result = builder.mapTagListToReaderInterestUiState(readerTagList, mock())

        // assert
        for ((index, interest) in result.interest.withIndex()) {
            assertThat(interest.chipStyle).isEqualTo(expectedListStyles[index])
        }
    }

    @Test
    fun `given a tag list with 5 elements, then the 5th interest within the uiState is styled green`() = test {
        // arrange
        val currentReaderTagListSize = 5

        val readerTagList = createReaderTagList(currentReaderTagListSize)

        // act
        val result = builder.mapTagListToReaderInterestUiState(readerTagList, mock())

        // assert
        assertThat(result.interest.last().chipStyle).isInstanceOf(ChipStyleGreen::class.java)
    }

    // endregion

    @Test
    fun `scheme is removed from recommended blog url`() = test {
        // Arrange
        val url = "http://dummy.url"
        val blog = createRecommendedBlog(blogUrl = url)
        whenever(urlUtilsWrapper.removeScheme(url)).thenReturn("dummy.url")
        // Act
        val uiState = builder.mapRecommendedBlogsToReaderRecommendedBlogsCardUiState(
            listOf(blog),
            { _, _, _ -> },
            { }
        )
        // Assert
        assertThat(uiState.blogs[0].url).isEqualTo("dummy.url")
    }

    @Test
    fun `limits recommended blogs count to 3`() = test {
        // Arrange
        whenever(urlUtilsWrapper.removeScheme(any())).thenReturn("dummy.url")
        val blogs = List(6) { createRecommendedBlog() }

        // Act
        val uiState = builder.mapRecommendedBlogsToReaderRecommendedBlogsCardUiState(
            blogs,
            { _, _, _ -> },
            { }
        )

        // Assert
        assertThat(uiState.blogs.size).isEqualTo(3)
    }

    @Test
    fun `ReaderRecommendedBlogUiState description is null when description is empty`() = test {
        // Arrange
        whenever(urlUtilsWrapper.removeScheme(any())).thenReturn("dummy.url")
        val blogs = List(1) { createRecommendedBlog(blogDescription = "") }

        // Act
        val uiState = builder.mapRecommendedBlogsToReaderRecommendedBlogsCardUiState(
            blogs,
            { _, _, _ -> },
            { }
        )

        // Assert
        assertThat(uiState.blogs[0].description).isNull()
    }

    // region Private methods
    private suspend fun mapPostToUiState(
        post: ReaderPost,
        postListType: ReaderPostListType = TAG_FOLLOWED,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock()
    ): ReaderPostUiState {
        return builder.mapPostToUiState(
            source = "source",
            post = post,
            photonWidth = 0,
            photonHeight = 0,
            postListType = postListType,
            onButtonClicked = onButtonClicked,
            onItemClicked = mock(),
            onItemRendered = mock(),
            onMoreButtonClicked = mock(),
            onVideoOverlayClicked = mock(),
            onPostHeaderViewClicked = mock(),
            onMoreDismissed = mock()
        )
    }

    private fun createPost(
        hasTitle: Boolean = true,
        hasExcerpt: Boolean = true,
        hasBlogName: Boolean = true,
        blogUrl: String = "",
        isDiscoverPost: Boolean = false,
        cardType: ReaderCardType = DEFAULT,
        featuredVideoUrl: String? = null,
        postId: Long = 1L,
        blogId: Long = 2L,
        isBookmarked: Boolean = false,
        numOfReplies: Int = 0,
        numOfLikes: Int = 0,
        isWPCom: Boolean = true,
        isCommentsOpen: Boolean = true,
        isPrivate: Boolean = false,
        isCanLikePost: Boolean = true,
        hasFeaturedImage: Boolean = false,
        featuredImageUrlForDisplay: String? = null,
        hasFeaturedVideo: Boolean = false,
        isp2Post: Boolean = false,
        blogName: String = "",
        authorFirstName: String = "",
        authorName: String = ""
    ): ReaderPost {
        val post = spy(ReaderPost().apply {
            this.blogUrl = blogUrl
            this.cardType = cardType
            this.featuredVideo = featuredVideoUrl
            this.blogId = blogId
            this.postId = postId
            this.isBookmarked = isBookmarked
        })
        // The ReaderPost contains business logic and accesses static classes. Using spy() allows us to use it in tests.
        whenever(post.isDiscoverPost).thenReturn(isDiscoverPost)
        post.numReplies = numOfReplies
        post.numLikes = numOfLikes
        post.isPrivate = isPrivate
        whenever(post.hasTitle()).thenReturn(hasTitle)
        whenever(post.hasBlogName()).thenReturn(hasBlogName)
        whenever(post.hasExcerpt()).thenReturn(hasExcerpt)
        whenever(post.canLikePost()).thenReturn(isCanLikePost)
        whenever(post.isWP).thenReturn(isWPCom)
        post.isCommentsOpen = isCommentsOpen
        whenever(post.getFeaturedImageForDisplay(anyInt(), anyInt())).thenReturn(featuredImageUrlForDisplay)
        whenever(post.hasFeaturedImage()).thenReturn(hasFeaturedImage)
        whenever(post.hasFeaturedVideo()).thenReturn(hasFeaturedVideo)
        post.organization = if (isp2Post) P2 else NO_ORGANIZATION
        post.blogName = blogName
        post.authorFirstName = authorFirstName
        post.authorName = authorName
        return post
    }

    private fun createReaderTagList(numOfTags: Int) = ReaderTagList().apply {
        (0 until numOfTags).forEach {
            add(createReaderTag())
        }
    }

    private fun createReaderTag() = ReaderTag(
        "",
        "",
        "",
        null,
        mock(),
        false
    )

    private fun createRecommendedBlog(
        blogUrl: String = "url",
        blogDescription: String = "desc"
    ) = ReaderBlog().apply {
        blogId = 1L
        name = "name"
        description = blogDescription
        url = blogUrl
        imageUrl = null
        feedId = 0L
        isFollowing = false
    }
    // endregion
}
