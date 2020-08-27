package org.wordpress.android.ui.reader.discover

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

@Reusable
class ReaderPostMoreButtonUiStateBuilder @Inject constructor(
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper
) {
    fun buildMoreMenuItems(
        post: ReaderPost,
        postListType: ReaderPostListType,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): List<SecondaryAction> {
        val menuItems = mutableListOf<SecondaryAction>()
        if (readerPostTableWrapper.isPostFollowed(post)) {
            menuItems.add(
                    SecondaryAction(
                            type = FOLLOW,
                            label = UiStringRes(R.string.reader_btn_unfollow),
                            labelColor = R.attr.wpColorSuccess,
                            iconRes = R.drawable.ic_reader_following_white_24dp,
                            isSelected = true,
                            onClicked = onButtonClicked
                    )
            )

            // When blogId and feedId are not equal, post is not a feed so show notifications option.
            if (post.blogId != post.feedId) {
                if (readerBlogTableWrapper.isNotificationsEnabled(post.blogId)) {
                    menuItems.add(
                            SecondaryAction(
                                    type = SITE_NOTIFICATIONS,
                                    label = UiStringRes(R.string.reader_btn_notifications_off),
                                    labelColor = R.attr.wpColorSuccess,
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
                            labelColor = R.attr.colorPrimary,
                            iconRes = R.drawable.ic_reader_follow_white_24dp,
                            isSelected = false,
                            onClicked = onButtonClicked
                    )
            )
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
                        iconRes = R.drawable.ic_external_white_24dp,
                        iconColor = R.attr.wpColorOnSurfaceMedium,
                        onClicked = onButtonClicked
                )
        )

        if (postListType == TAG_FOLLOWED) {
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
        }
        return menuItems
    }
}
