package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.EDITOR_PICK
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class ReaderGetPostUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    suspend fun get(blogId: Long, postId: Long, isFeed: Boolean): Pair<ReaderPost?, Boolean> =
        withContext(ioDispatcher) {
            val readerPost = if (isFeed) {
                readerPostTableWrapper.getFeedPost(blogId = blogId, postId = postId, excludeTextColumn = false)
            } else {
                readerPostTableWrapper.getBlogPost(blogId = blogId, postId = postId, excludeTextColumn = false)
            }

            // "discover" Editor Pick posts should open the original (source) post
            val discoverEditorPost = readerPost?.let { getDiscoverEditorPickSourcePost(it) }

            val post = discoverEditorPost ?: readerPost
            val isFeedPost = if (discoverEditorPost != null) false else isFeed

            Pair(post, isFeedPost)
        }

    private fun getDiscoverEditorPickSourcePost(post: ReaderPost) =
        post.takeIf { it.isDiscoverPost && isDiscoverEditorPickPost(it.discoverData) }?.let {
            readerPostTableWrapper.getBlogPost(it.discoverData.blogId, it.discoverData.postId, false)
        }

    private fun isDiscoverEditorPickPost(discoverPostData: ReaderPostDiscoverData?) =
        discoverPostData?.discoverType == EDITOR_PICK &&
                discoverPostData.blogId != 0L &&
                discoverPostData.postId != 0L
}
