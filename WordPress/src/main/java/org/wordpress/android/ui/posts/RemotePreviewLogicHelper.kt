package org.wordpress.android.ui.posts

import android.app.Activity
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePreviewLogicHelper @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLauncherWrapper: ActivityLauncherWrapper
) {
    enum class RemotePreviewType {
        NOT_A_REMOTE_PREVIEW,
        REMOTE_PREVIEW,
        REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
    }

    enum class PreviewLogicOperationResult {
        PREVIEW_NOT_AVAILABLE,
        NETWORK_NOT_AVAILABLE,
        MEDIA_UPLOAD_IN_PROGRESS,
        CANNOT_SAVE_EMPTY_DRAFT,
        GENERATING_PREVIEW,
        OPENING_PREVIEW,
        CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
    }

    interface RemotePreviewHelperFunctions {
        fun isNewPost() = false
        fun notifyNoNetwork()
        fun notifyUploadInProgress(post: PostModel): Boolean
        fun updatePostIfNeeded(): PostModel? = null
        fun canDiscard(): Boolean = false
        fun notifyEmptyDraft() {}
        fun startUploading(isRemoteAutoSave: Boolean, post: PostModel)
        fun notifyEmptyPost() {}
    }

    fun runPostPreviewLogic(
        activity: Activity,
        site: SiteModel,
        post: PostModel,
        helperFunctions: RemotePreviewHelperFunctions
    ): PreviewLogicOperationResult {
        if (!site.isUsingWpComRestApi) {
            activityLauncherWrapper.showActionableEmptyView(
                    activity,
                    WPWebViewActivity.ActionableReusableState.REMOTE_PREVIEW_NOT_AVAILABLE
            )
            return PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE
        } else if (!networkUtilsWrapper.isNetworkAvailable()) {
            helperFunctions.notifyNoNetwork()
            return PreviewLogicOperationResult.NETWORK_NOT_AVAILABLE
        } else {
            if (helperFunctions.notifyUploadInProgress(post)) {
                return PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS
            }

            val updatedPost = helperFunctions.updatePostIfNeeded() ?: post

            if (shouldSave(helperFunctions::isNewPost, updatedPost)) {
                if (helperFunctions.canDiscard()) {
                    helperFunctions.notifyEmptyDraft()
                    return PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT
                }

                helperFunctions.startUploading(false, updatedPost)
            } else if (shouldRemoteAutoSave(helperFunctions::isNewPost, updatedPost)) {
                if (helperFunctions.canDiscard()) {
                    helperFunctions.notifyEmptyPost()
                    return PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
                }
                helperFunctions.startUploading(true, updatedPost)
            } else {
                activityLauncherWrapper.previewPostOrPageForResult(
                        activity,
                        site,
                        updatedPost,
                        RemotePreviewType.REMOTE_PREVIEW
                )
                return PreviewLogicOperationResult.OPENING_PREVIEW
            }
        }
        return PreviewLogicOperationResult.GENERATING_PREVIEW
    }

    private fun shouldSave(isNewPost: () -> Boolean, post: PostModel): Boolean {
        val status = PostStatus.fromPost(post)
        return (isNewPost() || post.isLocalDraft ||
                ((status == PostStatus.DRAFT || status == PostStatus.SCHEDULED) && post.isLocallyChanged))
    }

    private fun shouldRemoteAutoSave(isNewPost: () -> Boolean, post: PostModel): Boolean {
        val status = PostStatus.fromPost(post)
        return (!isNewPost() && (status == PostStatus.PUBLISHED) && post.isLocallyChanged)
    }
}
