package org.wordpress.android.ui.posts

import androidx.fragment.app.FragmentActivity
import dagger.Reusable
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@Reusable
class PreviewStateHelper @Inject constructor() {
    fun managePreviewStateTransitions(
        newState: PostListRemotePreviewState,
        prevState: PostListRemotePreviewState?,
        postInfo: PostInfoType,
        handleRemotePreview: (localPostId: Int, RemotePreviewType) -> Unit
    ) = when (newState) {
        PostListRemotePreviewState.PREVIEWING -> {
            prevState?.let {
                if (it == PostListRemotePreviewState.UPLOADING_FOR_PREVIEW ||
                    it == PostListRemotePreviewState.REMOTE_AUTO_SAVING_FOR_PREVIEW
                ) {
                    handleRemotePreview(
                        (postInfo as PostInfoType.PostInfo).post.id,
                        mapRemotePreviewStateToType(prevState)
                    )
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
        override fun notifyUploadInProgress(post: PostImmutableModel): Boolean {
            return if (UploadService.hasInProgressMediaUploadsForPost(post)) {
                action.showToast.invoke(action.messageMediaUploading)
                true
            } else {
                false
            }
        }

        override fun startUploading(isRemoteAutoSave: Boolean, post: PostImmutableModel) {
            if (isRemoteAutoSave) {
                action.triggerPreviewStateUpdate.invoke(
                    PostListRemotePreviewState.REMOTE_AUTO_SAVING_FOR_PREVIEW,
                    PostInfoType.PostNoInfo
                )
                if (!UploadService.isPostUploadingOrQueued(post)) {
                    UploadService.uploadPost(activity, post.id, false)
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
                    UploadService.uploadPost(activity, post.id, false)
                } else {
                    AppLog.d(
                        AppLog.T.POSTS,
                        "Upload for preview not possible: post already uploading or queued"
                    )
                }
            }
        }
    }

    private fun mapRemotePreviewStateToType(prevState: PostListRemotePreviewState): RemotePreviewType {
        return when (prevState) {
            PostListRemotePreviewState.UPLOADING_FOR_PREVIEW -> RemotePreviewType.REMOTE_PREVIEW
            PostListRemotePreviewState.REMOTE_AUTO_SAVING_FOR_PREVIEW ->
                RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
            else -> throw IllegalArgumentException("Unsupported argument: $prevState")
        }
    }
}
