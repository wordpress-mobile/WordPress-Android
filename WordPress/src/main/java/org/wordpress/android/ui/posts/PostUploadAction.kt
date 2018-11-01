package org.wordpress.android.ui.posts

import android.content.Intent
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

sealed class PostUploadAction {
    class EditPostResult(
        val site: SiteModel,
        val post: PostModel,
        val data: Intent,
        val publishAction: () -> Unit
    ) : PostUploadAction()

    class PublishPost(val dispatcher: Dispatcher, val site: SiteModel, val post: PostModel) : PostUploadAction()

    class PostUploadedSnackbar(
        val dispatcher: Dispatcher,
        val site: SiteModel,
        val post: PostModel,
        val isError: Boolean,
        val errorMessage: String?
    ) : PostUploadAction()

    class MediaUploadedSnackbar(
        val site: SiteModel,
        val mediaList: List<MediaModel>,
        val isError: Boolean,
        val message: String?
    ) : PostUploadAction()

    /**
     * Cancel all post and media uploads related to this post
     */
    class CancelPostAndMediaUpload(val post: PostModel) : PostUploadAction()
}
