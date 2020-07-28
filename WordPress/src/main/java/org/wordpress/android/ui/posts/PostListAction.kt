package org.wordpress.android.ui.posts

import androidx.fragment.app.FragmentActivity
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NativeNotificationsUtils
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_POSTS_LIST
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_POSTS_LIST
import org.wordpress.android.ui.media.MediaBrowserType.WP_STORIES_MEDIA_PICKER
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder

sealed class PostListAction {
    class EditPost(val site: SiteModel, val post: PostModel, val loadAutoSaveRevision: Boolean) : PostListAction()
    class NewPost(val site: SiteModel, val isPromo: Boolean = false) : PostListAction()
    class NewStoryPost(val site: SiteModel) : PostListAction()
    class PreviewPost(
        val site: SiteModel,
        val post: PostModel,
        val triggerPreviewStateUpdate: (PostListRemotePreviewState, PostInfoType) -> Unit,
        val showToast: (ToastMessageHolder) -> Unit,
        val messageMediaUploading: ToastMessageHolder
    ) : PostListAction()
    class RemotePreviewPost(
        val site: SiteModel,
        val post: PostModel,
        val remotePreviewType: RemotePreviewType
    ) : PostListAction()
    class RetryUpload(
        val post: PostModel,
        val trackAnalytics: Boolean = PostUtils.isFirstTimePublish(post)
    ) : PostListAction()

    class ViewStats(val site: SiteModel, val post: PostModel) : PostListAction()
    class ViewPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class DismissPendingNotification(val pushId: Int) : PostListAction()
}

fun handlePostListAction(
    activity: FragmentActivity,
    action: PostListAction,
    remotePreviewLogicHelper: RemotePreviewLogicHelper,
    previewStateHelper: PreviewStateHelper
) {
    when (action) {
        is PostListAction.EditPost -> {
            ActivityLauncher.editPostOrPageForResult(activity, action.site, action.post, action.loadAutoSaveRevision)
        }
        is PostListAction.NewPost -> {
            ActivityLauncher.addNewPostForResult(activity, action.site, action.isPromo, POST_FROM_POSTS_LIST)
        }
        is PostListAction.NewStoryPost -> {
            ActivityLauncher.showPhotoPickerForResult(
                    activity,
                    WP_STORIES_MEDIA_PICKER,
                    action.site,
                    null // this is not required, only used for featured image in normal Posts
            )
        }
        is PostListAction.PreviewPost -> {
            val helperFunctions = previewStateHelper.getUploadStrategyFunctions(activity, action)
            remotePreviewLogicHelper.runPostPreviewLogic(
                    activity = activity,
                    site = action.site,
                    post = action.post,
                    helperFunctions = helperFunctions
            )
        }
        is PostListAction.RemotePreviewPost -> {
            ActivityLauncher.previewPostOrPageForResult(activity, action.site, action.post, action.remotePreviewType)
        }
        is PostListAction.RetryUpload -> {
            // restart the UploadService with retry parameters
            val intent = UploadService.getRetryUploadServiceIntent(
                    activity,
                    action.post,
                    action.trackAnalytics
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
