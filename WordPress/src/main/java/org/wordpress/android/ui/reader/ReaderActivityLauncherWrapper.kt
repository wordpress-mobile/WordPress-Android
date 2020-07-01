package org.wordpress.android.ui.reader

import android.content.ActivityNotFoundException
import android.content.Context
import dagger.Reusable
import org.wordpress.android.models.ReaderPost
import javax.inject.Inject

/**
 * Injectable wrapper around ReaderActivityLauncher.
 *
 * ReaderActivityLauncher interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Reusable
class ReaderActivityLauncherWrapper @Inject constructor(private val appContext: Context) {
    @Throws(ActivityNotFoundException::class) fun sharePost(post: ReaderPost) =
            ReaderActivityLauncher.sharePost(appContext, post)

    fun openPost(post: ReaderPost) = ReaderActivityLauncher.openPost(appContext, post)

    fun showReaderComments(blogId: Long, postId: Long) =
            ReaderActivityLauncher.showReaderComments(appContext, blogId, postId)
}
