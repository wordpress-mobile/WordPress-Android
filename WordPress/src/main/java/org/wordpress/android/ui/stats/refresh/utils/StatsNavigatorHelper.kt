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
    @Suppress("LongParameterList")
    fun openPostInReaderOrInAppWebView(
        context: Context,
        blogId: Long,
        itemId: Long,
        itemType: String,
        itemUrl: String,
        readerTracker: ReaderTracker
    ) {
        when (itemType) {
            StatsConstants.ITEM_TYPE_POST -> showPostInReader(context, blogId, itemId, readerTracker)
            StatsConstants.ITEM_TYPE_HOME_PAGE -> showPostInReaderPreview(context, blogId, itemId, readerTracker)
            // For now, itemType.ATTACHMENT falls down this path. No need to repeat unless we
            // want to handle attachments differently in the future.
            else -> showPostInAppWebView(context, itemUrl)
        }
    }

    private fun showPostInReader(
        context: Context,
        remoteBlogId: Long,
        remoteItemId: Long,
        readerTracker: ReaderTracker
    ) {
        // If the post/page has ID == 0 is the home page, and we need to load the blog preview,
        // otherwise 404 is returned if we try to show the post in the reader
        if (remoteItemId == 0L) {
            showPostInReaderPreview(context, remoteBlogId, remoteItemId, readerTracker)
        } else {
            ReaderActivityLauncher.showReaderPostDetail(context, remoteBlogId, remoteItemId)
        }
    }

    private fun showPostInReaderPreview(
        context: Context,
        remoteBlogId: Long,
        remoteItemId: Long,
        readerTracker: ReaderTracker
    ) {
        val post = ReaderPostTable.getBlogPost(remoteBlogId, remoteItemId, true)
        ReaderActivityLauncher.showReaderBlogPreview(
            context,
            post,
            ReaderTracker.SOURCE_STATS,
            readerTracker
        )
    }

    private fun showPostInAppWebView(context: Context, itemUrl: String) {
        AppLog.d(UTILS, "Opening the in-app browser: $itemUrl")
        WPWebViewActivity.openURL(context, itemUrl)
    }
}
