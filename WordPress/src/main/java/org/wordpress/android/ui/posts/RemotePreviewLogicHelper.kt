package org.wordpress.android.ui.posts

import android.app.Activity
import dagger.Reusable
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.REMOTE_AUTO_SAVE
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.UPLOAD
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.UPLOAD_AS_DRAFT
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@Reusable
class RemotePreviewLogicHelper @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLauncherWrapper: ActivityLauncherWrapper,
    private val postUtilsWrapper: PostUtilsWrapper,
    private val uploadActionUseCase: UploadActionUseCase
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
        fun notifyUploadInProgress(post: PostImmutableModel): Boolean
        fun updatePostIfNeeded(): PostImmutableModel? = null
        fun notifyEmptyDraft() {}
        fun startUploading(isRemoteAutoSave: Boolean, post: PostImmutableModel)
        fun notifyEmptyPost() {}
    }

    fun runPostPreviewLogic(
        activity: Activity,
        site: SiteModel,
        post: PostImmutableModel,
        helperFunctions: RemotePreviewHelperFunctions
    ): PreviewLogicOperationResult {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            activityLauncherWrapper.showActionableEmptyView(
                    activity,
                    WPWebViewUsageCategory.REMOTE_PREVIEW_NO_NETWORK,
                    post.title
            )
            return PreviewLogicOperationResult.NETWORK_NOT_AVAILABLE
        }

        // If a media upload is currently in progress, we can't upload / auto-save or preview until it's finished.
        if (helperFunctions.notifyUploadInProgress(post)) {
            return PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS
        }

        // Update the post object (copy title/content from the editor to the post object) when it's needed
        // (eg. during and editing session)
        val updatedPost = helperFunctions.updatePostIfNeeded() ?: post

        val uploadAction = uploadActionUseCase.getUploadAction(updatedPost)

        return when {
            shouldUpload(updatedPost, uploadAction) -> {
                // We can't upload an unpublishable post (empty), we'll let the user know we can't preview it.
                if (!postUtilsWrapper.isPublishable(updatedPost)) {
                    helperFunctions.notifyEmptyDraft()
                    return PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT
                }
                helperFunctions.startUploading(false, updatedPost)
                PreviewLogicOperationResult.GENERATING_PREVIEW
            }
            shouldRemoteAutoSave(updatedPost, uploadAction) -> {
                // We don't support remote auto-save for self hosted sites (accessed via XMLRPC),
                // we make the preview unavailable in that case.
                if (!site.isUsingWpComRestApi) {
                    activityLauncherWrapper.showActionableEmptyView(
                            activity,
                            WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE,
                            post.title
                    )
                    return PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE
                }

                // We can't remote auto-save an unpublishable post (empty), we'll let the user know we can't preview it.
                if (!postUtilsWrapper.isPublishable(updatedPost)) {
                    helperFunctions.notifyEmptyPost()
                    return PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
                }
                // TODO: remove the following Jetpack exception when the API bug for Jetpack sites is fixed
                // and previewing auto-saves are working. More informations about this on the following ticket:
                // https://github.com/Automattic/wp-calypso/issues/20265
                if (site.isJetpackConnected) {
                    activityLauncherWrapper.showActionableEmptyView(
                            activity,
                            WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE,
                            post.title
                    )
                    return PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE
                }
                helperFunctions.startUploading(true, updatedPost)
                PreviewLogicOperationResult.GENERATING_PREVIEW
            }
            else -> {
                // If we don't need upload or auto save the post (eg. post not modified), open the preview directly.
                activityLauncherWrapper.previewPostOrPageForResult(
                        activity,
                        site,
                        updatedPost,
                        RemotePreviewType.REMOTE_PREVIEW
                )
                PreviewLogicOperationResult.OPENING_PREVIEW
            }
        }
    }

    private fun shouldUpload(post: PostImmutableModel, action: UploadAction): Boolean {
        return (post.isLocallyChanged || post.isLocalDraft) && (action == UPLOAD_AS_DRAFT || action == UPLOAD)
    }

    private fun shouldRemoteAutoSave(post: PostImmutableModel, action: UploadAction): Boolean {
        return post.isLocallyChanged && action == REMOTE_AUTO_SAVE
    }
}
