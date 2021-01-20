package org.wordpress.android.datasets

import org.wordpress.android.models.ReaderBlog
import javax.inject.Inject

class ReaderBlogTableWrapper
@Inject constructor() {
    fun getFollowedBlogs(): List<ReaderBlog> = ReaderBlogTable.getFollowedBlogs()!!
    fun getBlogInfo(blogId: Long): ReaderBlog? = ReaderBlogTable.getBlogInfo(blogId)
    fun getFeedInfo(feedId: Long): ReaderBlog? = ReaderBlogTable.getFeedInfo(feedId)
    fun isNotificationsEnabled(blogId: Long): Boolean = ReaderBlogTable.isNotificationsEnabled(blogId)
    fun setNotificationsEnabledByBlogId(blogId: Long, isEnabled: Boolean) =
            ReaderBlogTable.setNotificationsEnabledByBlogId(blogId, isEnabled)

    fun getReaderBlog(blogId: Long, feedId: Long): ReaderBlog? {
        return if (feedId != 0L) {
            ReaderBlogTable.getFeedInfo(feedId)
        } else {
            ReaderBlogTable.getBlogInfo(blogId)
        }
    }

    fun isSiteFollowed(blogId: Long, feedId: Long): Boolean {
        return if (feedId != 0L) {
            ReaderBlogTable.isFollowedFeed(feedId)
        } else {
            ReaderBlogTable.isFollowedBlog(blogId)
        }
    }

    fun incrementUnseenCount(blogId: Long) = ReaderBlogTable.incrementUnseenCount(blogId)
    fun decrementUnseenCount(blogId: Long) = ReaderBlogTable.decrementUnseenCount(blogId)
}
