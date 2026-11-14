package org.wordpress.android.ui.reader

import android.os.Bundle
import androidx.core.os.BundleCompat
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import java.util.Stack

/**
 * The posts viewed in the detail fragment, so back can return through them. Only related posts use it.
 */
internal class ReaderPostHistory : Stack<ReaderBlogIdPostId>() {
    fun restoreInstance(bundle: Bundle?) {
        clear()
        bundle?.let { BundleCompat.getSerializable(it, HISTORY_KEY_NAME, ArrayList::class.java) }
            ?.filterIsInstance<ReaderBlogIdPostId>()
            ?.let { addAll(it) }
    }

    fun saveInstance(bundle: Bundle) {
        if (isNotEmpty()) bundle.putSerializable(HISTORY_KEY_NAME, ArrayList(this))
    }

    companion object {
        private const val HISTORY_KEY_NAME = "reader_post_history"
    }
}
