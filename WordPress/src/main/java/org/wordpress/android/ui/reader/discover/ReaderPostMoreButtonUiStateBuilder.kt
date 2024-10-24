package org.wordpress.android.ui.reader.discover

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SpacerNoAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_USER
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.READING_PREFERENCES
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_POST
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_USER
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.TOGGLE_SEEN_STATUS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import javax.inject.Inject
import javax.inject.Named
import com.google.android.material.R as MaterialR

@Reusable
class ReaderPostMoreButtonUiStateBuilder @Inject constructor(
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val seenUnseenWithCounterFeatureConfig: SeenUnseenWithCounterFeatureConfig,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun buildMoreMenuItems(
        post: ReaderPost,
        includeBookmark: Boolean,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): List<ReaderPostCardAction> {
        return withContext(bgDispatcher) {
            buildMoreMenuItemsBlocking(post, includeBookmark, false, onButtonClicked)
        }
    }

    fun buildMoreMenuItemsBlocking(
        post: ReaderPost,
        includeBookmark: Boolean,
        includeReadingPreferences: Boolean,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): MutableList<ReaderPostCardAction> {
        val menuItems = mutableListOf<ReaderPostCardAction>()
        val isPostFollowed = readerPostTableWrapper.isPostFollowed(post)
        val isPostBookmarked = post.isBookmarked

        menuItems.add(buildVisitSite(onButtonClicked))
        checkAndAddMenuItemForSiteNotifications(menuItems, isPostFollowed, post, onButtonClicked)
        checkAndAddMenuItemForPostSeenUnseen(menuItems, post, onButtonClicked)
        if (includeBookmark) menuItems.add(buildBookmark(isPostBookmarked, onButtonClicked))
        menuItems.add(buildShare(onButtonClicked))
        menuItems.add(buildFollow(isPostFollowed, onButtonClicked))
        if (includeReadingPreferences) {
            menuItems.add(buildReadingPreferences(onButtonClicked))
        }
        menuItems.add(buildBlockSite(onButtonClicked))
        menuItems.add(buildReportPost(onButtonClicked))
        checkAndAddUserMenuItems(post, menuItems, onButtonClicked)

        return menuItems
    }

    private fun buildVisitSite(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = VISIT_SITE,
            label = UiStringRes(R.string.reader_label_visit),
            labelColor = MaterialR.attr.colorOnSurface,
            iconRes = R.drawable.ic_globe_white_24dp,
            iconColor = R.attr.wpColorOnSurfaceMedium,
            onClicked = onButtonClicked
        )

    private fun checkAndAddMenuItemForSiteNotifications(
        menuItems: MutableList<ReaderPostCardAction>,
        isPostFollowed: Boolean,
        post: ReaderPost,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ) {
        if (isPostFollowed) {
            // When post not from external feed then show notifications option.
            if (!readerUtilsWrapper.isExternalFeed(post.blogId, post.feedId)) {
                menuItems.add(
                    buildSiteNotifications(
                        readerBlogTableWrapper.isNotificationsEnabled(post.blogId), onButtonClicked
                    )
                )
            }
        }
    }

    private fun buildSiteNotifications(
        isNotificationsEnabled: Boolean,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): SecondaryAction =
        if (isNotificationsEnabled) {
            SecondaryAction(
                type = SITE_NOTIFICATIONS,
                label = UiStringRes(R.string.reader_btn_blog_notifications_off),
                labelColor = R.attr.wpColorOnSurfaceMedium,
                iconRes = R.drawable.ic_reader_bell_24dp,
                isSelected = true,
                onClicked = onButtonClicked
            )
        } else {
            SecondaryAction(
                type = SITE_NOTIFICATIONS,
                label = UiStringRes(R.string.reader_btn_blog_notifications_on),
                labelColor = MaterialR.attr.colorOnSurface,
                iconRes = R.drawable.ic_reader_bell_24dp,
                iconColor = R.attr.wpColorOnSurfaceMedium,
                isSelected = false,
                onClicked = onButtonClicked
            )
        }

    private fun checkAndAddMenuItemForPostSeenUnseen(
        menuItems: MutableList<ReaderPostCardAction>,
        post: ReaderPost,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ) {
        if (seenUnseenWithCounterFeatureConfig.isEnabled()) {
            if (post.isSeenSupported) {
                menuItems.add(buildPostSeenUnseen(readerPostTableWrapper.isPostSeen(post), onButtonClicked))
            }
        }
    }

    private fun buildPostSeenUnseen(
        isPostSeen: Boolean,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): SecondaryAction =
        if (isPostSeen) {
            SecondaryAction(
                type = TOGGLE_SEEN_STATUS,
                label = UiStringRes(R.string.reader_menu_mark_as_unseen),
                labelColor = MaterialR.attr.colorOnSurface,
                iconRes = R.drawable.ic_not_visible_white_24dp,
                iconColor = R.attr.wpColorOnSurfaceMedium,
                isSelected = false,
                onClicked = onButtonClicked
            )
        } else {
            SecondaryAction(
                type = TOGGLE_SEEN_STATUS,
                label = UiStringRes(R.string.reader_menu_mark_as_seen),
                labelColor = MaterialR.attr.colorOnSurface,
                iconRes = R.drawable.ic_visible_white_24dp,
                iconColor = R.attr.wpColorOnSurfaceMedium,
                isSelected = false,
                onClicked = onButtonClicked
            )
        }

    private fun buildBookmark(
        isPostBookmarked: Boolean,
        onClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): SecondaryAction = if (isPostBookmarked) {
        SecondaryAction(
            type = BOOKMARK,
            label = UiStringRes(R.string.reader_secondary_bookmarked),
            labelColor =  R.attr.wpColorOnSurfaceMedium,
            iconRes = R.drawable.ic_bookmark_fill_new_24dp,
            isSelected = true,
            onClicked = onClicked,
        )
    } else {
        SecondaryAction(
            type = BOOKMARK,
            label = UiStringRes(R.string.reader_secondary_bookmark),
            labelColor = MaterialR.attr.colorSecondary,
            iconRes = R.drawable.ic_bookmark_outline_new_24dp,
            isSelected = false,
            onClicked = onClicked,
        )
    }

    private fun buildShare(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = SHARE,
            label = UiStringRes(R.string.reader_btn_share),
            labelColor = MaterialR.attr.colorOnSurface,
            iconRes = R.drawable.ic_share_white_24dp,
            iconColor = R.attr.wpColorOnSurfaceMedium,
            onClicked = onButtonClicked
        )

    private fun buildFollow(isPostFollowed: Boolean, onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        if (isPostFollowed) {
            SecondaryAction(
                type = FOLLOW,
                label = UiStringRes(R.string.reader_btn_subscribed),
                labelColor = R.attr.wpColorOnSurfaceMedium,
                iconRes = R.drawable.ic_reader_following_white_24dp,
                isSelected = true,
                onClicked = onButtonClicked
            )
        } else {
            SecondaryAction(
                type = FOLLOW,
                label = UiStringRes(R.string.reader_btn_subscribe),
                labelColor = MaterialR.attr.colorSecondary,
                iconRes = R.drawable.ic_reader_follow_white_24dp,
                isSelected = false,
                onClicked = onButtonClicked
            )
        }

    private fun buildBlockSite(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = BLOCK_SITE,
            label = UiStringRes(R.string.reader_menu_block_this_blog),
            labelColor = R.attr.wpColorError,
            iconRes = R.drawable.ic_block_white_24dp,
            iconColor = R.attr.wpColorError,
            onClicked = onButtonClicked
        )

    private fun buildReadingPreferences(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = READING_PREFERENCES,
            label = UiStringRes(R.string.reader_menu_reading_preferences),
            labelColor = MaterialR.attr.colorOnSurface,
            iconRes = R.drawable.ic_reader_preferences,
            iconColor = R.attr.wpColorOnSurfaceMedium,
            onClicked = onButtonClicked
        )

    private fun checkAndAddUserMenuItems(
        post: ReaderPost,
        menuItems: MutableList<ReaderPostCardAction>,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ) {
        if (!readerUtilsWrapper.isSelfHosted(post.authorBlogId)) {
            menuItems.add(buildReportUser(onButtonClicked))
            menuItems.add(buildBlockUser(onButtonClicked))
        }
    }

    private fun buildBlockUser(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = BLOCK_USER,
            label = UiStringRes(R.string.reader_menu_block_user),
            labelColor = R.attr.wpColorError,
            iconRes = R.drawable.ic_block_white_24dp,
            iconColor = R.attr.wpColorError,
            onClicked = onButtonClicked
        )

    private fun buildReportPost(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = REPORT_POST,
            label = UiStringRes(R.string.reader_menu_report_post),
            labelColor = R.attr.wpColorError,
            iconRes = R.drawable.ic_flag_white_24dp,
            iconColor = R.attr.wpColorError,
            onClicked = onButtonClicked
        )

    private fun buildReportUser(onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit) =
        SecondaryAction(
            type = REPORT_USER,
            label = UiStringRes(R.string.reader_menu_report_user),
            labelColor = R.attr.wpColorError,
            iconRes = R.drawable.ic_flag_white_24dp,
            iconColor = R.attr.wpColorError,
            onClicked = onButtonClicked
        )
}
