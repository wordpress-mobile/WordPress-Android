package org.wordpress.android.ui.stats.refresh.utils

import android.content.Context
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.stats.StatsConstants
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS

object StatsNavigatorHelper {
    fun openPostInReaderOrInAppWebview(
        ctx: Context?,
        remoteBlogID: Long,
        remoteItemID: String,
        itemType: String?,
        itemURL: String,
        readerTracker: ReaderTracker?
    ) {
        val itemID = remoteItemID.toLong()
        if (itemType == null) {
            // If we don't know the type of the item, open it with the browser.
            AppLog.d(UTILS, "Type of the item is null. Opening it in the in-app browser: $itemURL")
            WPWebViewActivity.openURL(ctx, itemURL)
        } else if (itemType == StatsConstants.ITEM_TYPE_POST) {
            // If the post/page has ID == 0 is the home page, and we need to load the blog preview,
            // otherwise 404 is returned if we try to show the post in the reader
            if (itemID == 0L) {
                val post = ReaderPostTable.getBlogPost(remoteBlogID, itemID, true)
                ReaderActivityLauncher.showReaderBlogPreview(
                        ctx,
                        remoteBlogID,
                        post?.isFollowedByCurrentUser,
                        ReaderTracker.SOURCE_STATS,
                        readerTracker
                )
            } else {
                ReaderActivityLauncher.showReaderPostDetail(
                        ctx,
                        remoteBlogID,
                        itemID
                )
            }
        } else if (itemType == StatsConstants.ITEM_TYPE_HOME_PAGE) {
            val post = ReaderPostTable.getBlogPost(remoteBlogID, itemID, true)
            ReaderActivityLauncher.showReaderBlogPreview(
                    ctx,
                    remoteBlogID,
                    post?.isFollowedByCurrentUser,
                    ReaderTracker.SOURCE_STATS,
                    readerTracker
            )
        } else {
            // For now, itemType.ATTACHMENT falls down this path. No need to repeat unless we
            // want to handle attachments differently in the future.
            AppLog.d(UTILS, "Opening the in-app browser: $itemURL")
            WPWebViewActivity.openURL(ctx, itemURL)
        }
    }
}
