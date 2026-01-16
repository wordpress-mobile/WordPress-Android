package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult.Updated
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Outcome
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.posts.editor.StorePostViewModel.ActivityFinishState
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

/**
 * Coordinates post publishing and upload operations for the post editor.
 *
 * This class extracts publishing logic from EditPostActivity to improve
 * testability and separation of concerns.
 */
@Reusable
class PostPublishingCoordinator @Inject constructor(
    private val accountStore: AccountStore,
    private val dispatcher: Dispatcher,
    private val postUtilsWrapper: PostUtilsWrapper,
    private val uploadUtilsWrapper: UploadUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    /**
     * Listener for publishing events that require Activity-level handling.
     */
    interface PublishingListener {
        fun getContext(): Context
        fun getEditPostRepository(): EditPostRepository
        fun provideStorePostViewModel(): StorePostViewModel
        fun getSiteModel(): SiteModel
        fun getEditorFragment(): Any?
        fun getPostEditorAnalyticsSession(): PostEditorAnalyticsSession?
        fun providePrimaryAction(): PrimaryEditorAction
        fun isPage(): Boolean
        fun showPrepublishingNudgeBottomSheet()
        fun updateAndSavePostAsyncOnEditorExit(onComplete: (UpdatePostResult) -> Unit)
        fun savePostAndOptionallyFinish(doFinish: Boolean, forceSave: Boolean)
    }

    private var listener: PublishingListener? = null

    /**
     * Initializes the coordinator with the required listener.
     */
    fun start(listener: PublishingListener) {
        this.listener = listener
    }

    /**
     * Handles the primary action button click based on the current action type.
     */
    fun performPrimaryAction() {
        val listener = this.listener ?: return

        when (listener.providePrimaryAction()) {
            PrimaryEditorAction.PUBLISH_NOW -> {
                analyticsTrackerWrapper.track(Stat.EDITOR_POST_PUBLISH_TAPPED)
                listener.showPrepublishingNudgeBottomSheet()
            }

            PrimaryEditorAction.UPDATE,
            PrimaryEditorAction.CONTINUE,
            PrimaryEditorAction.SCHEDULE,
            PrimaryEditorAction.SUBMIT_FOR_REVIEW -> {
                listener.showPrepublishingNudgeBottomSheet()
            }

            PrimaryEditorAction.SAVE -> uploadPost(false)
        }
    }

    /**
     * Initiates the post upload process.
     */
    fun uploadPost(publishPost: Boolean) {
        listener?.updateAndSavePostAsyncOnEditorExit { _ ->
            if (shouldPerformPostUpdateAndPublish()) {
                performPostUpdateAndPublish(publishPost)
            }
        }
    }

    /**
     * Validates whether the post can be published.
     * Checks email verification and post content.
     */
    @Suppress("ReturnCount")
    fun shouldPerformPostUpdateAndPublish(): Boolean {
        val listener = this.listener ?: return false
        val context = listener.getContext()
        val account: AccountModel = accountStore.account

        // Prompt user to verify email before publishing
        if (!account.emailVerified) {
            listener.provideStorePostViewModel().hideSavingProgressDialog()
            val message: String = if (TextUtils.isEmpty(account.email)) {
                context.getString(R.string.editor_confirm_email_prompt_message)
            } else {
                String.format(
                    context.getString(R.string.editor_confirm_email_prompt_message_with_email),
                    account.email
                )
            }

            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(R.string.editor_confirm_email_prompt_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ToastUtils.showToast(
                        context,
                        context.getString(R.string.toast_saving_post_as_draft)
                    )
                    listener.savePostAndOptionallyFinish(doFinish = true, forceSave = false)
                }
                .setNegativeButton(R.string.editor_confirm_email_prompt_negative) { _, _ ->
                    dispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction())
                }
            builder.create().show()
            return false
        }

        listener.getEditPostRepository().getPost()?.let {
            if (!postUtilsWrapper.isPublishable(it)) {
                listener.provideStorePostViewModel().hideSavingProgressDialog()
                listener.getEditPostRepository().updateStatusFromPostSnapshotWhenEditorOpened()

                val message: String = context.getString(
                    if (listener.isPage()) {
                        R.string.error_publish_empty_page
                    } else {
                        R.string.error_publish_empty_post
                    }
                )
                ToastUtils.showToast(context, message, ToastUtils.Duration.SHORT)
                return false
            }
        }
        return true
    }

    /**
     * Performs the actual post update and publish operation.
     */
    fun performPostUpdateAndPublish(publishPost: Boolean) {
        val listener = this.listener ?: return
        val storePostViewModel = listener.provideStorePostViewModel()
        val editPostRepository = listener.getEditPostRepository()

        storePostViewModel.showSavingProgressDialog()
        val isFirstTimePublish: Boolean = editPostRepository.isFirstTimePublish(publishPost)

        editPostRepository.updateAsync({ postModel: PostModel ->
            if (publishPost) {
                // Now set status to PUBLISHED - only do this AFTER we have run the
                // isFirstTimePublish() check, otherwise we'd have an incorrect value.
                // Also re-set the published date in case it was SCHEDULED and they want
                // to publish NOW.
                if (postModel.status == PostStatus.SCHEDULED.toString()) {
                    postModel.setDateCreated(dateTimeUtilsWrapper.currentTimeInIso8601())
                }
                if (uploadUtilsWrapper.userCanPublish(listener.getSiteModel())) {
                    postModel.setStatus(PostStatus.PUBLISHED.toString())
                } else {
                    postModel.setStatus(PostStatus.PENDING.toString())
                }
                listener.getPostEditorAnalyticsSession()?.setOutcome(Outcome.PUBLISH)
            } else {
                listener.getPostEditorAnalyticsSession()?.setOutcome(Outcome.SAVE)
            }
            AppLog.d(
                AppLog.T.POSTS,
                "User explicitly confirmed changes. Post Title: " + postModel.title
            )
            // The user explicitly confirmed an intention to upload the post
            postModel.setChangesConfirmedContentHashcode(postModel.contentHashcode())
            true
        }) { _: PostImmutableModel?, result: UpdatePostResult ->
            if (result === Updated) {
                val activityFinishState: ActivityFinishState =
                    savePostOnline(isFirstTimePublish, listener)
                storePostViewModel.finish(activityFinishState)
            }
        }
    }

    /**
     * Saves the post to the server.
     */
    private fun savePostOnline(
        isFirstTimePublish: Boolean,
        listener: PublishingListener
    ): ActivityFinishState {
        val editorFragment = listener.getEditorFragment()
        if (editorFragment is GutenbergEditorFragment) {
            editorFragment.sendToJSPostSaveEvent()
        }
        return listener.provideStorePostViewModel().savePostOnline(
            isFirstTimePublish,
            listener.getContext(),
            listener.getEditPostRepository(),
            listener.getSiteModel()
        )
    }
}
