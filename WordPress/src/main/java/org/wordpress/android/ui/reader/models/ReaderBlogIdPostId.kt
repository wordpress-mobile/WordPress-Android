package org.wordpress.android.ui.reader.models

import java.io.Serializable

data class ReaderBlogIdPostId(val blogId: Long, val postId: Long) : Serializable {
    companion object {
        private const val serialVersionUID = 0L
    }
}
