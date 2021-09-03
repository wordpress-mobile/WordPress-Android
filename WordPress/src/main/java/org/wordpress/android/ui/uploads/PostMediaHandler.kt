package org.wordpress.android.ui.uploads

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import javax.inject.Inject

class PostMediaHandler
@Inject constructor(private val mediaStore: MediaStore, private val dispatcher: Dispatcher) {
    fun updateMediaWithoutPostId(site: SiteModel, post: PostModel) {
        if (post.remotePostId != 0L) {
            val mediaForPost = mediaStore.getMediaForPost(post)
            mediaForPost.filter { it.postId == 0L }.forEach { media ->
                media.postId = post.remotePostId
                dispatcher.dispatch(MediaActionBuilder.newPushMediaAction(MediaPayload(site, media)))
            }
        }
    }
}
