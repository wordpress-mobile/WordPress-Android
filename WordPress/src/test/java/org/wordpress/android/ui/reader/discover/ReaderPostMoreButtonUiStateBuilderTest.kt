package org.wordpress.android.ui.reader.discover

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostMoreButtonUiStateBuilderTest : BaseUnitTest() {
    private val dummyOnClick: (Long, Long, ReaderPostCardActionType) -> Unit = { _, _, _ -> }
    private lateinit var builder: ReaderPostMoreButtonUiStateBuilder
    @Mock lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper
    @Mock lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock lateinit var mSeenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig

    @Before
    fun setUp() = test {
        whenever(mSeenUnseenWithCounterFeatureConfig.isEnabled()).thenReturn(true)

        builder = ReaderPostMoreButtonUiStateBuilder(
                readerPostTableWrapper,
                readerBlogTableWrapper,
                readerUtilsWrapper,
                mSeenUnseenWithCounterFeatureConfig,
                testDispatcher()
        )
    }

    @Test
    fun `contains follow action when post is not followed`() = test {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    (it as SecondaryAction).label == UiStringRes(R.string.reader_btn_follow)
        }).isNotNull
    }

    @Test
    fun `contains unfollow action when post is followed`() = test {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    (it as SecondaryAction).label == UiStringRes(R.string.reader_btn_unfollow)
        }).isNotNull
    }

    @Test
    fun `does not contain site notifications action when not followed`() = test {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS
        }).isNull()
    }

    @Test
    fun `contains site notifications action when followed`() = test {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS
        }).isNotNull
    }

    @Test
    fun `does not contain site notifications action for feeds`() = test {
        // Arrange
        val post = init(isFeed = true, isFollowed = true)
        whenever(readerUtilsWrapper.isExternalFeed(post.feedId, post.blogId)).thenReturn(true)

        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS
        }).isNull()
    }

    @Test
    fun `site notifications action label is ON when notifications disabled`() = test {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    (it as SecondaryAction).label == UiStringRes(R.string.reader_btn_notifications_on)
        }).isNotNull
    }

    @Test
    fun `site notifications action label is OFF when notifications enabled`() = test {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    (it as SecondaryAction).label == UiStringRes(R.string.reader_btn_notifications_off)
        }).isNotNull
    }

    @Test
    fun `contains share action`() = test {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.SHARE }).isNotNull
    }

    @Test
    fun `contains visit site action`() = test {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.VISIT_SITE }).isNotNull
    }

    @Test
    fun `contains block site action when post is not followed`() = test {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.BLOCK_SITE }).isNotNull
    }

    @Test
    fun `contains block site action when post is followed`() = test {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.BLOCK_SITE }).isNotNull
    }

    @Test
    fun `follow action label color is secondary(pink)`() = test {
        // Arrange
        val post = init(isFollowed = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    (it as SecondaryAction).labelColor == R.attr.colorSecondary
        }).isNotNull
    }

    @Test
    fun `unfollow action label color is OnSurfaceMedium(grey)`() = test {
        // Arrange
        val post = init(isFollowed = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.FOLLOW &&
                    (it as SecondaryAction).labelColor == R.attr.wpColorOnSurfaceMedium
        }).isNotNull
    }

    @Test
    fun `site notifications action label color is default when notifications disabled`() = test {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    (it as SecondaryAction).labelColor == R.attr.colorOnSurface
        }).isNotNull
    }

    @Test
    fun `site notifications action label color is OnSurfaceMedium(grey) when notifications enabled`() = test {
        // Arrange
        val post = init(isFollowed = true, isNotificationsEnabled = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.SITE_NOTIFICATIONS &&
                    (it as SecondaryAction).labelColor == R.attr.wpColorOnSurfaceMedium
        }).isNotNull
    }

    @Test
    fun `contains report post action`() = test {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.REPORT_POST }).isNotNull
    }

    @Test
    fun `contains report user action`() = test {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.REPORT_USER }).isNotNull
    }

    @Test
    fun `contains mark as seen when post is unseen`() = test {
        // Arrange
        val post = init(isSeen = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.TOGGLE_SEEN_STATUS &&
                    (it as SecondaryAction).label == UiStringRes(R.string.reader_menu_mark_as_seen)
        }).isNotNull
    }

    @Test
    fun `contains mark as unseen action when post is seen`() = test {
        // Arrange
        val post = init(isSeen = true)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find {
            it.type == ReaderPostCardActionType.TOGGLE_SEEN_STATUS &&
                    (it as SecondaryAction).label == UiStringRes(R.string.reader_menu_mark_as_unseen)
        }).isNotNull
    }

    @Test
    fun `contains seen status toggle action when posts isSeenSupported is true`() = test {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.TOGGLE_SEEN_STATUS }).isNotNull
    }

    @Test
    fun `does not contain seen status toggle action when posts isSeenSupported is false`() = test {
        // Arrange
        val post = init(isSeenSupported = false)
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.TOGGLE_SEEN_STATUS }).isNull()
    }

    @Test
    fun `given post list card actions created, then list contains spacer no action`() = test {
        // Arrange
        val post = init()
        // Act
        val menuItems = builder.buildMoreMenuItems(post, dummyOnClick)
        // Assert
        assertThat(menuItems.find { it.type == ReaderPostCardActionType.SPACER_NO_ACTION }).isNotNull
    }

    private fun init(
        isFollowed: Boolean = false,
        isNotificationsEnabled: Boolean = false,
        isFeed: Boolean = false,
        isSeenSupported: Boolean = true,
        isSeen: Boolean = false
    ): ReaderPost {
        whenever(readerPostTableWrapper.isPostFollowed(anyOrNull())).thenReturn(isFollowed)
        whenever(readerPostTableWrapper.isPostSeen(anyOrNull())).thenReturn(isSeen)
        whenever(readerBlogTableWrapper.isNotificationsEnabled(anyLong())).thenReturn(isNotificationsEnabled)
        return ReaderPost().apply {
            this.blogId = 1L
            this.feedId = if (isFeed) 1L else 2L // set blogId == feedId so the post is treated as a feed
            this.isSeenSupported = isSeenSupported
            this.isSeen = isSeen
        }
    }
}
