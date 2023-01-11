package org.wordpress.android.ui.posts

import android.app.Activity
import android.content.Intent
import android.view.View
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.uploads.UploadUtils.OnPublishingCallback
import org.wordpress.android.ui.uploads.UploadUtilsWrapper

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
        val isFirstTimePublish: Boolean,
        val errorMessage: String?
    ) : PostUploadAction()

    class MediaUploadedSnackbar(
        val site: SiteModel,
        val mediaList: List<MediaModel>,
        val isError: Boolean,
        val message: String?
    ) : PostUploadAction()

    class PostRemotePreviewSnackbarError(
        val messageResId: Int
    ) : PostUploadAction()

    /**
     * Cancel all post and media uploads related to this post
     */
    class CancelPostAndMediaUpload(val post: PostModel) : PostUploadAction()
}

@Suppress("LongParameterList")
fun handleUploadAction(
    action: PostUploadAction,
    activity: Activity,
    snackbarAttachView: View,
    uploadActionUseCase: UploadActionUseCase,
    uploadUtilsWrapper: UploadUtilsWrapper,
    onPublishingCallback: OnPublishingCallback
) {
    when (action) {
        is PostUploadAction.EditPostResult -> {
            uploadUtilsWrapper.handleEditPostResultSnackbars(
                activity,
                snackbarAttachView,
                action.data,
                action.post,
                action.site,
                uploadActionUseCase.getUploadAction(action.post),
                { action.publishAction() },
                onPublishingCallback
            )
        }
        is PostUploadAction.PublishPost -> {
            UploadUtils.publishPost(
                activity,
                action.post,
                action.site,
                action.dispatcher,
                onPublishingCallback
            )
        }
        is PostUploadAction.PostUploadedSnackbar -> {
            uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                activity,
                snackbarAttachView,
                action.isError,
                action.isFirstTimePublish,
                action.post,
                action.errorMessage,
                action.site,
                onPublishingCallback
            )
        }
        is PostUploadAction.MediaUploadedSnackbar -> {
            uploadUtilsWrapper.onMediaUploadedSnackbarHandler(
                activity,
                snackbarAttachView,
                action.isError,
                action.mediaList,
                action.site,
                action.message
            )
        }
        is PostUploadAction.PostRemotePreviewSnackbarError -> {
            uploadUtilsWrapper.showSnackbarError(
                snackbarAttachView,
                snackbarAttachView.resources.getString(action.messageResId)
            )
        }
        is PostUploadAction.CancelPostAndMediaUpload -> {
            UploadService.cancelQueuedPostUploadAndRelatedMedia(activity, action.post)
        }
    }
}
