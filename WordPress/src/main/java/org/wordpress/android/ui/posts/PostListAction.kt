package org.wordpress.android.ui.posts

import android.content.ClipData
import androidx.fragment.app.FragmentActivity
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NativeNotificationsUtils
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_POSTS_LIST
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.stories.intro.StoriesIntroDialogFragment
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.extensions.clipboardManager
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
    class CopyUrl(
        val site: SiteModel,
        val post: PostModel,
        val showSnackbar: (SnackbarMessageHolder) -> Unit,
        val messageSuccess: SnackbarMessageHolder,
        val messageError: SnackbarMessageHolder
    ) : PostListAction()

    class ViewStats(val site: SiteModel, val post: PostModel) : PostListAction()
    class ViewPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class DismissPendingNotification(val pushId: Int) : PostListAction()
}

@Suppress("TooGenericExceptionCaught")
fun handlePostListAction(
    activity: FragmentActivity,
    action: PostListAction,
    remotePreviewLogicHelper: RemotePreviewLogicHelper,
    previewStateHelper: PreviewStateHelper,
    mediaPickerLauncher: MediaPickerLauncher
) {
    when (action) {
        is PostListAction.EditPost -> {
            ActivityLauncher.editPostOrPageForResult(activity, action.site, action.post, action.loadAutoSaveRevision)
        }
        is PostListAction.NewPost -> {
            ActivityLauncher.addNewPostForResult(activity, action.site, action.isPromo, POST_FROM_POSTS_LIST, -1, null)
        }
        is PostListAction.NewStoryPost -> {
            if (AppPrefs.shouldShowStoriesIntro()) {
                StoriesIntroDialogFragment.newInstance(action.site)
                        .show(activity.supportFragmentManager, StoriesIntroDialogFragment.TAG)
            } else {
                mediaPickerLauncher.showStoriesPhotoPickerForResultAndTrack(activity, action.site)
            }
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
        is PostListAction.CopyUrl -> {
            try {
                activity.clipboardManager?.setPrimaryClip(
                        ClipData.newPlainText("${action.post.id}", action.post.link)
                ) ?: throw NullPointerException("ClipboardManager is not supported on this device")

                action.showSnackbar.invoke(action.messageSuccess)
            } catch (e: Exception) {
                /**
                 * Ignore any exceptions here as certain devices have bugs and will fail.
                 * See https://crrev.com/542cb9cfcc927295615809b0c99917b09a219d9f for more info.
                 */
                AppLog.e(AppLog.T.POSTS, e)
                action.showSnackbar.invoke(action.messageError)
            }
        }
    }
}
