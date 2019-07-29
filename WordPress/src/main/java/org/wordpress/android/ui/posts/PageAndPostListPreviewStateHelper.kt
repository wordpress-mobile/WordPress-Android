package org.wordpress.android.ui.posts

import androidx.fragment.app.FragmentActivity
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog

fun managePreviewStateTransitions(
    newState: PostListRemotePreviewState,
    prevState: PostListRemotePreviewState?,
    postInfo: PostInfoType,
    handleRemotePreview: (localPostId: Int, RemotePreviewType) -> Unit
) = when (newState) {
    PostListRemotePreviewState.PREVIEWING -> {
        prevState?.let {
            if (it == PostListRemotePreviewState.UPLOADING_FOR_PREVIEW ||
                    it == PostListRemotePreviewState.REMOTE_AUTO_SAVING_FOR_PREVIEW) {
                (postInfo as? PostInfoType.PostInfo)?.let { info ->
                    handleRemotePreview(info.post.id, getRemotePreviewType(prevState))
                }
            }
        }
    }
    PostListRemotePreviewState.NONE,
    PostListRemotePreviewState.UPLOADING_FOR_PREVIEW,
    PostListRemotePreviewState.REMOTE_AUTO_SAVING_FOR_PREVIEW,
    PostListRemotePreviewState.REMOTE_AUTO_SAVE_PREVIEW_ERROR -> {
        // nothing to do
    }
}

fun getUploadStrategyFunctions(
    activity: FragmentActivity,
    action: PostListAction.PreviewPost
) = object : RemotePreviewLogicHelper.RemotePreviewHelperFunctions {
    override fun notifyUploadInProgress(post: PostModel): Boolean {
        return if (UploadService.hasInProgressMediaUploadsForPost(post)) {
            action.showToast.invoke(action.messageMediaUploading)
            true
        } else {
            false
        }
    }

    override fun startUploading(isRemoteAutoSave: Boolean, post: PostModel) {
        if (isRemoteAutoSave) {
            action.triggerPreviewStateUpdate.invoke(
                    PostListRemotePreviewState.REMOTE_AUTO_SAVING_FOR_PREVIEW,
                    PostInfoType.PostNoInfo
            )
            if (!UploadService.isPostUploadingOrQueued(post)) {
                UploadService.uploadPost(activity, post, true)
            } else {
                AppLog.d(
                        AppLog.T.POSTS,
                        "Remote auto save for preview not possible: post already uploading or queued"
                )
            }
        } else {
            action.triggerPreviewStateUpdate.invoke(
                    PostListRemotePreviewState.UPLOADING_FOR_PREVIEW,
                    PostInfoType.PostNoInfo
            )
            if (!UploadService.isPostUploadingOrQueued(post)) {
                UploadService.uploadPost(activity, post)
            } else {
                AppLog.d(
                        AppLog.T.POSTS,
                        "Upload for preview not possible: post already uploading or queued"
                )
            }
        }
    }
}

private fun getRemotePreviewType(prevState: PostListRemotePreviewState): RemotePreviewType {
    return if (prevState == PostListRemotePreviewState.UPLOADING_FOR_PREVIEW) {
        RemotePreviewType.REMOTE_PREVIEW
    } else {
        RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
    }
}
