package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_PREVIEW
import org.wordpress.android.ui.utils.UiString.UiStringRes

@RunWith(MockitoJUnitRunner::class)
class ReaderPostMoreButtonUiStateBuilderTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private val dummyOnClick: (Long, Long, ReaderPostCardActionType) -> Unit = { _, _, _ -> }
    private lateinit var builder: ReaderPostMoreButtonUiStateBuilder
    @Mock lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper

    @Before
    fun setUp() {
        builder = ReaderPostMoreButtonUiStateBuilder(readerPostTableWrapper, readerBlogTableWrapper)
    }

    @Test
    fun `contains follow action when post is not followed`() {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    it.label == UiStringRes(R.string.reader_btn_follow)
        }).isNotNull
    }

    @Test
    fun `contains unfollow action when post is followed`() {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    it.label == UiStringRes(R.string.reader_btn_unfollow)
        }).isNotNull
    }

    @Test
    fun `does not contain site notifications action when not followed`() {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS
        }).isNull()
    }

    @Test
    fun `contains site notifications action when followed`() {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS
        }).isNotNull
    }

    @Test
    fun `does not contain site notifications action for feeds`() {
        // Arrange
        val post = init(isFeed = true, isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS
        }).isNull()
    }

    @Test
    fun `site notifications action label is ON when notifications disabled`() {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    it.label == UiStringRes(R.string.reader_btn_notifications_on)
        }).isNotNull
    }

    @Test
    fun `site notifications action label is OFF when notifications enabled`() {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    it.label == UiStringRes(R.string.reader_btn_notifications_off)
        }).isNotNull
    }

    @Test
    fun `contains share action`() {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.SHARE }).isNotNull
    }

    @Test
    fun `contains visit site action`() {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.VISIT_SITE }).isNotNull
    }

    @Test
    fun `contains block site action when post list type is TAG_FOLLOWED`() {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.BLOCK_SITE }).isNotNull
    }

    @Test
    fun `does not contain block site action when post list type is not TAG_FOLLOWED`() {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_PREVIEW, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.BLOCK_SITE }).isNull()
    }

    @Test
    fun `follow action label color is primary(blue)`() {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    it.labelColor == R.attr.colorPrimary
        }).isNotNull
    }

    @Test
    fun `unfollow action label color is success(green)`() {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    it.labelColor == R.attr.wpColorSuccess
        }).isNotNull
    }

    @Test
    fun `site notifications action label color is default when notifications disabled`() {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    it.labelColor == R.attr.colorOnSurface
        }).isNotNull
    }

    @Test
    fun `site notifications action label color is success(green) when notifications enabled`() {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, TAG_FOLLOWED, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    it.labelColor == R.attr.wpColorSuccess
        }).isNotNull
    }

    private fun init(
        isFollowed: Boolean = false,
        isNotificationsEnabled: Boolean = false,
        isFeed: Boolean = false
    ): ReaderPost {
        whenever(readerPostTableWrapper.isPostFollowed(anyOrNull())).thenReturn(isFollowed)
        whenever(readerBlogTableWrapper.isNotificationsEnabled(anyLong())).thenReturn(isNotificationsEnabled)
        return ReaderPost().apply {
            this.blogId = 1L
            this.feedId = if (isFeed) 1L else 2L // set blogId == feedId so the post is treated as a feed
        }
    }
}
