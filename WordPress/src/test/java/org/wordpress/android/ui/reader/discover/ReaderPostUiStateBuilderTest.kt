package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderCardType
import org.wordpress.android.models.ReaderCardType.DEFAULT
import org.wordpress.android.models.ReaderCardType.GALLERY
import org.wordpress.android.models.ReaderCardType.PHOTO
import org.wordpress.android.models.ReaderCardType.VIDEO
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.EDITOR_PICK
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.OTHER
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.SITE_PICK
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.BLOG_PREVIEW
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.models.ReaderImageList
import org.wordpress.android.ui.reader.utils.ReaderImageScanner
import org.wordpress.android.ui.reader.utils.ReaderImageScannerProvider
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.image.ImageType
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ReaderPostUiStateBuilderTest {
    // region Set-up
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var builder: ReaderPostUiStateBuilder

    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var urlUtilsWrapper: UrlUtilsWrapper
    @Mock lateinit var gravatarUtilsWrapper: GravatarUtilsWrapper
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var readerImageScannerProvider: ReaderImageScannerProvider
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder
    @Mock lateinit var readerPostTagsUiStateBuilder: ReaderPostTagsUiStateBuilder
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper

    @Before
    fun setUp() {
        builder = ReaderPostUiStateBuilder(
                accountStore,
                urlUtilsWrapper,
                gravatarUtilsWrapper,
                dateTimeUtilsWrapper,
                readerImageScannerProvider,
                readerUtilsWrapper,
                readerPostMoreButtonUiStateBuilder,
                readerPostTagsUiStateBuilder,
                appPrefsWrapper
        )
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(anyOrNull())).thenReturn("")
        whenever(gravatarUtilsWrapper.fixGravatarUrlWithResource(anyOrNull(), anyInt())).thenReturn("")
        val imageScanner: ReaderImageScanner = mock()
        whenever(readerImageScannerProvider.createReaderImageScanner(anyOrNull(), anyBoolean()))
                .thenReturn(imageScanner)
        whenever(imageScanner.getImageList(anyInt(), anyInt())).thenReturn(ReaderImageList(false))
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(readerUtilsWrapper.getLongLikeLabelText(anyInt(), anyBoolean())).thenReturn("")
    }
    // endregion

    // region BLOG HEADER
    @Test
    fun `clicks on blog header are disabled on blog preview`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post, BLOG_PREVIEW)
        // Assert
        assertThat(uiState.postHeaderClickData).isNull()
    }

    @Test
    fun `clicks on blog header are enabled when not blog preview`() {
        // Arrange
        val post = createPost()
        ReaderPostListType.values().filter { it != BLOG_PREVIEW }.forEach {
            // Act
            val uiState = mapPostToUiState(post, it)
            // Assert
            assertThat(uiState.postHeaderClickData).isNotNull
        }
    }
    // endregion

    // region BLOG URL
    @Test
    fun `scheme is removed from blog url`() {
        // Arrange
        val post = createPost(blogUrl = "http://dummy.url")
        whenever(urlUtilsWrapper.removeScheme("http://dummy.url")).thenReturn("dummy.url")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogUrl).isEqualTo("dummy.url")
    }
    // endregion

    // region DISCOVER SECTION
    @Test
    fun `discover section is empty when isDiscoverPost is false`() {
        // Arrange
        val post = createPost(isDiscoverPost = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.discoverSection).isNull()
    }

    @Test
    fun `discover section is not empty when isDiscoverPost is true`() {
        // Arrange
        val post = createPost(isDiscoverPost = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.discoverSection).isNotNull
    }

    @Test
    fun `discover section is empty when discoverType is OTHER`() {
        // Arrange
        val post = createPost(isDiscoverPost = true, discoverType = OTHER)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.discoverSection).isNull()
    }

    @Test
    fun `discover section is not empty when discoverType is other than OTHER`() {
        // Arrange
        DiscoverType.values().filter { it != OTHER }.forEach {
            val post = createPost(isDiscoverPost = true, discoverType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.discoverSection).isNotNull
        }
    }

    @Test
    fun `discover uses ImageType AVATAR when EDITOR_PICK`() {
        // Arrange
        val post = createPost(isDiscoverPost = true, discoverType = EDITOR_PICK)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.discoverSection!!.imageType).isEqualTo(ImageType.AVATAR)
    }

    @Test
    fun `discover uses ImageType BLAVATAR when SITE_PICK`() {
        // Arrange
        val post = createPost(isDiscoverPost = true, discoverType = SITE_PICK)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.discoverSection!!.imageType).isEqualTo(ImageType.BLAVATAR)
    }

    @Test
    fun `discover uses fixed avatar URL`() {
        // Arrange
        val post = createPost(isDiscoverPost = true)
        whenever(gravatarUtilsWrapper.fixGravatarUrlWithResource(anyOrNull(), anyInt())).thenReturn("12345")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.discoverSection!!.discoverAvatarUrl).isEqualTo("12345")
    }
    // endregion

    // region VIDEO
    @Test
    fun `videoUrl gets initialized for video cards`() {
        // Arrange
        val post = createPost(cardType = VIDEO, featuredVideoUrl = "12345")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.fullVideoUrl).isEqualTo("12345")
    }

    @Test
    fun `videoUrl does not get initialized for other than video cards`() {
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
    fun `video overlay is displayed for video cards`() {
        // Arrange
        val post = createPost(cardType = VIDEO)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.videoOverlayVisibility).isTrue()
    }

    @Test
    fun `video overlay is not displayed for other than video cards`() {
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
    fun `thumbnail strip is not empty for GALLERY`() {
        // Arrange
        val post = createPost(cardType = GALLERY)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.thumbnailStripSection).isNotNull
    }

    @Test
    fun `thumbnail strip is empty for other than GALLERY`() {
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
    fun `featured image is displayed for photo and default card types`() {
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
    fun `featured image is not displayed for other than photo and default card types`() {
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
    fun `featured image is not displayed when hasFeaturedImage returns false`() {
        // Arrange
        val post = createPost(cardType = PHOTO, hasFeaturedImage = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.featuredImageUrl).isNull()
    }
    // endregion

    // region PHOTO TITLE
    @Test
    fun `photo title is displayed for photo card type`() {
        // Arrange
        val post = createPost(cardType = PHOTO)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoTitle).isNotNull()
    }

    @Test
    fun `photo title is not displayed for other than photo card type`() {
        // Arrange
        ReaderCardType.values().filter { it != PHOTO }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.photoTitle).isNull()
        }
    }

    @Test
    fun `photo title is not displayed when hasTitle returns false`() {
        // Arrange
        val post = createPost(cardType = PHOTO, hasTitle = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoTitle).isNull()
    }
    // endregion

    // region PHOTO FRAME
    @Test
    fun `photo frame is visible for other than gallery type`() {
        // Arrange
        ReaderCardType.values().filter { it != GALLERY }.forEach {
            val post = createPost(cardType = it, hasFeaturedVideo = true)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.photoFrameVisibility).isTrue()
        }
    }

    @Test
    fun `photo frame is not visible for gallery type`() {
        // Arrange
        val post = createPost(cardType = GALLERY, hasFeaturedVideo = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoFrameVisibility).isFalse()
    }

    @Test
    fun `photo frame is visible when hasFeaturedVideo returns true`() {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasFeaturedVideo = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoFrameVisibility).isTrue()
    }

    @Test
    fun `photo frame is not visible when hasFeaturedVideo returns false`() {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasFeaturedVideo = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoFrameVisibility).isFalse()
    }

    @Test
    fun `photo frame is visible when hasFeaturedImage returns true`() {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasFeaturedImage = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoFrameVisibility).isTrue()
    }

    @Test
    fun `photo frame is not visible when hasFeaturedImage returns false`() {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasFeaturedImage = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.photoFrameVisibility).isFalse()
    }
    // endregion

    // region TITLE & EXCERPT
    @Test
    fun `title is displayed for other than PHOTO card type`() {
        // Arrange
        ReaderCardType.values().filter { it != PHOTO }.forEach {
            val post = createPost(cardType = it)
            // Act
            val uiState = mapPostToUiState(post)
            // Assert
            assertThat(uiState.title).isNotNull()
        }
    }

    @Test
    fun `title is not displayed for PHOTO card type`() {
        // Arrange
        val post = createPost(cardType = PHOTO)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.title).isNull()
    }

    @Test
    fun `title is not displayed when the post doesn't have a title`() {
        // Arrange
        val post = createPost(cardType = DEFAULT, hasTitle = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.title).isNull()
    }

    @Test
    fun `excerpt is displayed for other than PHOTO card type`() {
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
    fun `excerpt is not displayed for PHOTO card type`() {
        // Arrange
        val post = createPost(cardType = PHOTO)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.excerpt).isNull()
    }

    @Test
    fun `excerpt is not displayed when the post doesn't have an excerpt`() {
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
    fun `blog name is displayed for regular post`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogName).isNotNull()
    }

    @Test
    fun `blogName is not displayed the post doesn't have a blog name`() {
        // Arrange
        val post = createPost(hasBlogName = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.blogName).isNull()
    }
    // endregion

    // region DATELINE
    @Test
    fun `builds dateline from post's display date`() {
        // Arrange
        val post = createPost()
        val dummyDate: Date = mock()
        whenever(post.getDisplayDate(dateTimeUtilsWrapper)).thenReturn(dummyDate)
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(dummyDate)).thenReturn("success")
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.dateLine).isEqualTo("success")
    }
    // endregion

    // region BOOKMARK BUTTON
    @Test
    fun `bookmark button is disabled when postId is empty`() {
        // Arrange
        val post = createPost(postId = 0)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.bookmarkAction.isEnabled).isFalse()
    }

    @Test
    fun `bookmark button is disabled when blogId is empty`() {
        // Arrange
        val post = createPost(blogId = 0)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.bookmarkAction.isEnabled).isFalse()
    }

    @Test
    fun `bookmark button is enabled when blogid and postId is not empty`() {
        // Arrange
        val post = createPost(postId = 1L, blogId = 2L)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.bookmarkAction.isEnabled).isTrue()
    }

    @Test
    fun `bookmark button is selected when the post is bookmarked`() {
        // Arrange
        val post = createPost(isBookmarked = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.bookmarkAction.isSelected).isTrue()
    }

    @Test
    fun `bookmark button is not selected when the post is not bookmarked`() {
        // Arrange
        val post = createPost(isBookmarked = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.bookmarkAction.isSelected).isFalse()
    }

    @Test
    fun `onButtonClicked listener is correctly assigned to bookmarkAction`() {
        // Arrange
        val post = createPost()
        val onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock()
        val uiState = mapPostToUiState(post, onButtonClicked = onButtonClicked)
        // Act
        uiState.bookmarkAction.onClicked!!.invoke(1L, 1L, BOOKMARK)
        // Assert
        verify(onButtonClicked).invoke(1L, 1L, BOOKMARK)
    }
    // endregion

    // region LIKE BUTTON
    @Test
    fun `like button is enabled on regular posts`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isTrue()
    }

    @Test
    fun `like button is disabled on bookmark list`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post, isBookmarkList = true)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse()
    }

    @Test
    fun `like button is disabled when the user is logged off`() {
        // Arrange
        val post = createPost()
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse()
    }

    @Test
    fun `like button is disabled when likes are disabled on the post`() {
        // Arrange
        val post = createPost(isCanLikePost = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.likeAction.isEnabled).isFalse()
    }

    @Test
    fun `onButtonClicked listener is correctly assigned to likeAction`() {
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
    fun `reblog button is enabled on regular posts`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isTrue()
    }

    @Test
    fun `reblog button is disabled on private posts`() {
        // Arrange
        val post = createPost(isPrivate = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isFalse()
    }

    @Test
    fun `reblog button is disabled when the user is logged off`() {
        // Arrange
        val post = createPost()
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.reblogAction.isEnabled).isFalse()
    }

    @Test
    fun `onButtonClicked listener is correctly assigned to reblogAction`() {
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
    fun `Comments button is enabled on regular posts`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isTrue()
    }

    @Test
    fun `Comments button is disabled when comments are disabled on the post`() {
        // Arrange
        val post = createPost(isCommentsOpen = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse()
    }

    @Test
    fun `Comments button is disabled on non-wpcom posts`() {
        // Arrange
        val post = createPost(isWPCom = false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse()
    }

    @Test
    fun `Comments button is disabled when the user is logged off and the post does not have any comments`() {
        // Arrange
        val post = createPost(numOfReplies = 0)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse()
    }

    @Test
    fun `Comments button is enabled when the user is logged off but the post has some comments`() {
        // Arrange
        val post = createPost(numOfReplies = 1)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isTrue()
    }

    @Test
    fun `Comments button is disabled on discover posts`() {
        // Arrange
        val post = createPost(isDiscoverPost = true)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse()
    }

    @Test
    fun `Comments button is disabled on bookmark list`() {
        // Arrange
        val post = createPost()
        // Act
        val uiState = mapPostToUiState(post, isBookmarkList = true)
        // Assert
        assertThat(uiState.commentsAction.isEnabled).isFalse()
    }

    @Test
    fun `Count on Comments button corresponds to number of comments on the post`() {
        // Arrange
        val numReplies = 15
        val post = createPost(numOfReplies = numReplies)
        // Act
        val uiState = mapPostToUiState(post)
        // Assert
        assertThat(uiState.commentsAction.count).isEqualTo(numReplies)
    }

    @Test
    fun `Ensures that there are 5 interests within the uiState even though the ReaderTagList contains 6`() {
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
    fun `Ensures that there are three interests within the uiState when the ReaderTagList contains 3`() {
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
    fun `Ensures that the first InterestUiState has isDividerVisible set to true`() {
        // arrange
        val currentReaderTagListSize = 2
        val readerTagList = createReaderTagList(currentReaderTagListSize)

        // act
        val result = builder.mapTagListToReaderInterestUiState(readerTagList, mock())

        // assert
        assertThat(result.interest.first().isDividerVisible).isTrue()
    }

    @Test
    fun `Ensures that the last InterestUiState has isDividerVisible set to false`() {
        // arrange
        val currentReaderTagListSize = 2
        val readerTagList = createReaderTagList(currentReaderTagListSize)

        // act
        val result = builder.mapTagListToReaderInterestUiState(readerTagList, mock())

        // assert
        assertThat(result.interest.last().isDividerVisible).isFalse()
    }
    // endregion

    // region Private methods
    private fun mapPostToUiState(
        post: ReaderPost,
        postListType: ReaderPostListType = TAG_FOLLOWED,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = mock(),
        isBookmarkList: Boolean = false
    ): ReaderPostUiState {
        return builder.mapPostToUiState(
                post = post,
                isDiscover = false,
                photonWidth = 0,
                photonHeight = 0,
                postListType = postListType,
                isBookmarkList = isBookmarkList,
                onButtonClicked = onButtonClicked,
                onItemClicked = mock(),
                onItemRendered = mock(),
                onDiscoverSectionClicked = mock(),
                onMoreButtonClicked = mock(),
                onVideoOverlayClicked = mock(),
                onPostHeaderViewClicked = mock(),
                onTagItemClicked = mock()
        )
    }

    private fun createPost(
        hasTitle: Boolean = true,
        hasExcerpt: Boolean = true,
        hasBlogName: Boolean = true,
        blogUrl: String = "",
        isDiscoverPost: Boolean = false,
        discoverType: DiscoverType = SITE_PICK,
        cardType: ReaderCardType = DEFAULT,
        featuredVideoUrl: String? = null,
        postId: Long = 1L,
        blogId: Long = 2L,
        isBookmarked: Boolean = false,
        numOfReplies: Int = 0,
        isWPCom: Boolean = true,
        isCommentsOpen: Boolean = true,
        isPrivate: Boolean = false,
        isCanLikePost: Boolean = true,
        hasFeaturedImage: Boolean = false,
        featuredImageUrlForDisplay: String? = null,
        hasFeaturedVideo: Boolean = false
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
        if (isDiscoverPost) {
            val mockedDiscoverData: ReaderPostDiscoverData = mock()
            whenever(post.discoverData).thenReturn(mockedDiscoverData)
            whenever(mockedDiscoverData.discoverType).thenReturn(discoverType)
            whenever(mockedDiscoverData.attributionHtml).thenReturn(mock())
            whenever(mockedDiscoverData.avatarUrl).thenReturn("dummyUrl")
        }
        post.numReplies = numOfReplies
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
        return post
    }

    private fun createReaderTagList(numOfTags: Int) = ReaderTagList().apply {
        for (x in 0 until numOfTags) {
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
    // endregion
}
