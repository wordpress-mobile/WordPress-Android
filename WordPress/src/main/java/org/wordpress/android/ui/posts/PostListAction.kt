package org.wordpress.android.ui.posts

import androidx.fragment.app.FragmentActivity
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NativeNotificationsUtils
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder

sealed class PostListAction {
    class EditPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class NewPost(val site: SiteModel, val isPromo: Boolean = false) : PostListAction()
    class PreviewPost(
        val site: SiteModel,
        val post: PostModel,
        val triggerPreviewStateUpdate: (PostListRemotePreviewState, PostInfoType) -> Unit,
        val showSnackbar: (SnackbarMessageHolder) -> Unit,
        val showToast: (ToastMessageHolder) -> Unit,
        val messageNoNetwork: SnackbarMessageHolder,
        val messageMediaUploading: ToastMessageHolder
    ) : PostListAction()
    class RemotePreviewPost(
        val site: SiteModel,
        val post: PostModel,
        val remotePreviewType: RemotePreviewType
    ) : PostListAction()
    class RetryUpload(
        val post: PostModel,
        val trackAnalytics: Boolean = PostUtils.isFirstTimePublish(post),
        val publish: Boolean = false,
        val retry: Boolean = true
    ) : PostListAction()

    class ViewStats(val site: SiteModel, val post: PostModel) : PostListAction()
    class ViewPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class DismissPendingNotification(val pushId: Int) : PostListAction()
}

private fun getPostsListStrategyFunctions(
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

fun handlePostListAction(
    activity: FragmentActivity,
    action: PostListAction,
    remotePreviewLogicHelper: RemotePreviewLogicHelper
) {
    when (action) {
        is PostListAction.EditPost -> {
            ActivityLauncher.editPostOrPageForResult(activity, action.site, action.post)
        }
        is PostListAction.NewPost -> {
            ActivityLauncher.addNewPostForResult(activity, action.site, action.isPromo)
        }
        is PostListAction.PreviewPost -> {
            if (action.post.isPage) {
                ActivityLauncher.viewPostPreviewForResult(activity, action.site, action.post)
            } else {
                val opResult = remotePreviewLogicHelper.runPostPreviewLogic(
                        activity = activity,
                        site = action.site,
                        post = action.post,
                        helperFunctions = getPostsListStrategyFunctions(activity, action)
                )

                // TODO: consider to remove this once the modifications related to
                // https://github.com/wordpress-mobile/WordPress-Android/issues/10106 will be available.
                // In current implementation only Trashed posts can trigger the below condition but
                // once the above is implemented should not be possible to trigger below condition anymore.
                if (opResult == RemotePreviewLogicHelper.PreviewLogicOperationResult.OPENING_PREVIEW) {
                    action.triggerPreviewStateUpdate.invoke(
                            PostListRemotePreviewState.PREVIEWING,
                            PostInfoType.PostNoInfo
                    )
                }
            }
        }
        is PostListAction.RemotePreviewPost -> {
            ActivityLauncher.previewPostOrPageForResult(activity, action.site, action.post, action.remotePreviewType)
        }
        is PostListAction.RetryUpload -> {
            // restart the UploadService with retry parameters
            val intent = UploadService.getUploadPostServiceIntent(
                    activity,
                    action.post,
                    action.trackAnalytics,
                    action.publish,
                    action.retry
            )
            activity.startService(intent)
        }
        is PostListAction.ViewStats -> {
            ActivityLauncher.viewStatsSinglePostDetails(activity, action.site, action.post)
        }
        is PostListAction.ViewPost -> {
            ActivityLauncher.browsePostOrPage(activity, action.site, action.post)
        }
        is PostListAction.DismissPendingNotification -> {
            NativeNotificationsUtils.dismissNotification(action.pushId, activity)
        }
    }
}
