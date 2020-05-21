package org.wordpress.android.datasets

import org.wordpress.android.models.ReaderBlog
import javax.inject.Inject

class ReaderBlogTableWrapper
@Inject constructor() {
    fun getFollowedBlogs(): List<ReaderBlog> = ReaderBlogTable.getFollowedBlogs()!!
    fun getBlogInfo(blogId: Long): ReaderBlog? = ReaderBlogTable.getBlogInfo(blogId)
    fun getFeedInfo(feedId: Long): ReaderBlog? = ReaderBlogTable.getFeedInfo(feedId)
}
