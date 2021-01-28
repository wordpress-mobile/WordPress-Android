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
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_POST
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.TOGGLE_SEEN_STATUS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig
import javax.inject.Inject
import javax.inject.Named

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
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): List<SecondaryAction> {
        return withContext(bgDispatcher) {
            buildMoreMenuItemsBlocking(post, onButtonClicked)
        }
    }

    fun buildMoreMenuItemsBlocking(
        post: ReaderPost,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): MutableList<SecondaryAction> {
        val menuItems = mutableListOf<SecondaryAction>()
        if (readerPostTableWrapper.isPostFollowed(post)) {
            menuItems.add(
                    SecondaryAction(
                            type = FOLLOW,
                            label = UiStringRes(R.string.reader_btn_unfollow),
                            labelColor = R.attr.wpColorOnSurfaceMedium,
                            iconRes = R.drawable.ic_reader_following_white_24dp,
                            isSelected = true,
                            onClicked = onButtonClicked
                    )
            )

            // When post not from external feed then show notifications option.
            if (!readerUtilsWrapper.isExternalFeed(post.blogId, post.feedId)) {
                if (readerBlogTableWrapper.isNotificationsEnabled(post.blogId)) {
                    menuItems.add(
                            SecondaryAction(
                                    type = SITE_NOTIFICATIONS,
                                    label = UiStringRes(R.string.reader_btn_notifications_off),
                                    labelColor = R.attr.wpColorOnSurfaceMedium,
                                    iconRes = R.drawable.ic_bell_white_24dp,
                                    isSelected = true,
                                    onClicked = onButtonClicked
                            )
                    )
                } else {
                    menuItems.add(
                            SecondaryAction(
                                    type = SITE_NOTIFICATIONS,
                                    label = UiStringRes(R.string.reader_btn_notifications_on),
                                    labelColor = R.attr.colorOnSurface,
                                    iconRes = R.drawable.ic_bell_white_24dp,
                                    iconColor = R.attr.wpColorOnSurfaceMedium,
                                    isSelected = false,
                                    onClicked = onButtonClicked
                            )
                    )
                }
            }
        } else {
            menuItems.add(
                    SecondaryAction(
                            type = FOLLOW,
                            label = UiStringRes(R.string.reader_btn_follow),
                            labelColor = R.attr.colorSecondary,
                            iconRes = R.drawable.ic_reader_follow_white_24dp,
                            isSelected = false,
                            onClicked = onButtonClicked
                    )
            )
        }

        if (seenUnseenWithCounterFeatureConfig.isEnabled()) {
            if (post.isSeenSupported) {
                if (readerPostTableWrapper.isPostSeen(post)) {
                    menuItems.add(
                            SecondaryAction(
                                    type = TOGGLE_SEEN_STATUS,
                                    label = UiStringRes(R.string.reader_menu_mark_as_unseen),
                                    labelColor = R.attr.colorOnSurface,
                                    iconRes = R.drawable.ic_not_visible_white_24dp,
                                    iconColor = R.attr.wpColorOnSurfaceMedium,
                                    isSelected = false,
                                    onClicked = onButtonClicked
                            )
                    )
                } else {
                    menuItems.add(
                            SecondaryAction(
                                    type = TOGGLE_SEEN_STATUS,
                                    label = UiStringRes(R.string.reader_menu_mark_as_seen),
                                    labelColor = R.attr.colorOnSurface,
                                    iconRes = R.drawable.ic_visible_white_24dp,
                                    iconColor = R.attr.wpColorOnSurfaceMedium,
                                    isSelected = false,
                                    onClicked = onButtonClicked
                            )
                    )
                }
            }
        }

        menuItems.add(
                SecondaryAction(
                        type = SHARE,
                        label = UiStringRes(R.string.reader_btn_share),
                        labelColor = R.attr.colorOnSurface,
                        iconRes = R.drawable.ic_share_white_24dp,
                        iconColor = R.attr.wpColorOnSurfaceMedium,
                        onClicked = onButtonClicked
                )
        )
        menuItems.add(
                SecondaryAction(
                        type = VISIT_SITE,
                        label = UiStringRes(R.string.reader_label_visit),
                        labelColor = R.attr.colorOnSurface,
                        iconRes = R.drawable.ic_globe_white_24dp,
                        iconColor = R.attr.wpColorOnSurfaceMedium,
                        onClicked = onButtonClicked
                )
        )

        menuItems.add(
                SecondaryAction(
                        type = BLOCK_SITE,
                        label = UiStringRes(R.string.reader_menu_block_blog),
                        labelColor = R.attr.colorOnSurface,
                        iconRes = R.drawable.ic_block_white_24dp,
                        iconColor = R.attr.wpColorOnSurfaceMedium,
                        onClicked = onButtonClicked
                )
        )
        menuItems.add(
                SecondaryAction(
                        type = REPORT_POST,
                        label = UiStringRes(R.string.reader_menu_report_post),
                        labelColor = R.attr.colorOnSurface,
                        iconRes = R.drawable.ic_block_white_24dp,
                        iconColor = R.attr.wpColorOnSurfaceMedium,
                        onClicked = onButtonClicked
                )
        )
        return menuItems
    }
}
