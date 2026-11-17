package org.wordpress.android.ui.reader.models

class ReaderBlogIdPostIdList : ArrayList<ReaderBlogIdPostId>() {
    fun indexOf(blogId: Long, postId: Long) = indexOfFirst { it.blogId == blogId && it.postId == postId }
}
