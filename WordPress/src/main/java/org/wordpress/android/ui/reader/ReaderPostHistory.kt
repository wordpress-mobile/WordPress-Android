package org.wordpress.android.ui.reader

import android.os.Bundle
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import java.util.Stack

/**
 * The posts viewed in the detail fragment, so back can return through them. Only related posts use it.
 */
internal class ReaderPostHistory : Stack<ReaderBlogIdPostId>() {
    fun restoreInstance(bundle: Bundle?) {
        clear()
        bundle?.getStringArrayList(HISTORY_KEY_NAME)?.forEach { idPair ->
            val (blogId, postId) = idPair.split(":").map { it.toLongOrNull() ?: 0L }
            add(ReaderBlogIdPostId(blogId, postId))
        }
    }

    fun saveInstance(bundle: Bundle) {
        if (isNotEmpty()) {
            bundle.putStringArrayList(HISTORY_KEY_NAME, mapTo(ArrayList()) { "${it.blogId}:${it.postId}" })
        }
    }

    companion object {
        private const val HISTORY_KEY_NAME = "reader_post_history"
    }
}
