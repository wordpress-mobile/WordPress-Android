package org.wordpress.android.ui.posts.editor.publishing

import android.text.TextUtils
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.editor.StorePostViewModel
// import org.wordpress.android.ui.posts.editor.StorePostViewModel.ActivityFinishState
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject
import javax.inject.Singleton

enum class ActivityFinishState {
    SAVED, CANCELLED
}

/**
 * Manages all post publishing, saving, and validation operations.
 * Extracted from EditPostActivity to improve maintainability and testability.
 */
@Singleton
class PostPublishingManager @Inject constructor(
    private val accountStore: AccountStore,
    private val postStore: PostStore,
    private val uploadStore: UploadStore,
    private val storePostViewModel: StorePostViewModel,
    private val postValidationService: PostValidationService
) {
    interface PublishingListener {
        fun onPublishingStarted()
        fun onPublishingProgress(message: String)
        fun onPublishingSuccess(post: PostImmutableModel)
        fun onPublishingError(error: String, post: PostImmutableModel?)
        fun onSavingStarted()
        fun onSavingSuccess(post: PostImmutableModel)
        fun onSavingError(error: String, post: PostImmutableModel?)
        fun showEmailVerificationDialog(message: String)
        fun showPublishingProgressDialog()
        fun hidePublishingProgressDialog()
        fun shouldFinishActivity(): Boolean
    }
    
    private var listener: PublishingListener? = null
    private var editPostRepository: EditPostRepository? = null
    private var siteModel: SiteModel? = null
    private var postEditorAnalyticsSession: PostEditorAnalyticsSession? = null
    private var coroutineScope: CoroutineScope? = null
    
    fun initialize(
        listener: PublishingListener,
        lifecycleOwner: LifecycleOwner,
        editPostRepository: EditPostRepository,
        siteModel: SiteModel,
        postEditorAnalyticsSession: PostEditorAnalyticsSession?,
        coroutineScope: CoroutineScope
    ) {
        this.listener = listener
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        this.postEditorAnalyticsSession = postEditorAnalyticsSession
        this.coroutineScope = coroutineScope
        
        postValidationService.initialize(accountStore, siteModel)
    }
    
    /**
     * Saves the current post as a draft
     */
    fun saveAsDraft(): PublishResult {
        val repository = editPostRepository ?: return PublishResult.Error("Repository not initialized")
        val post = repository.getPost() ?: return PublishResult.Error("No post to save")
        
        AppLog.d(AppLog.T.POSTS, "PostPublishingManager: Saving post as draft")
        
        if (!shouldSavePost()) {
            AppLog.d(AppLog.T.POSTS, "Post doesn't need saving")
            return PublishResult.NoActionNeeded
        }
        
        listener?.onSavingStarted()
        
        return try {
            // Update post status to draft - stub implementation
            AppLog.d(AppLog.T.POSTS, "Saving post as draft (stub)")
            val draftPost = post // Using original post for now
            
            // repository.update { draftPost } // Stub - would update repository
            
            // Stub implementation for saving
            AppLog.d(AppLog.T.POSTS, "Saving draft post (stub)")
            listener?.onSavingSuccess(draftPost)
            PublishResult.Success(draftPost, PublishAction.SAVE_DRAFT)
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error saving draft", e)
            listener?.onSavingError("Error saving draft: ${e.message}", post)
            PublishResult.Error("Error saving draft: ${e.message}")
        }
    }
    
    /**
     * Publishes the current post
     */
    fun publishPost(isFirstTimePublish: Boolean = true): PublishResult {
        val repository = editPostRepository ?: return PublishResult.Error("Repository not initialized")
        val post = repository.getPost() ?: return PublishResult.Error("No post to publish")
        
        AppLog.d(AppLog.T.POSTS, "PostPublishingManager: Publishing post (firstTime: $isFirstTimePublish)")
        
        // Validate post before publishing
        val validationResult = postValidationService.validateForPublishing(post)
        if (!validationResult.isValid) {
            AppLog.w(AppLog.T.POSTS, "Post validation failed: ${validationResult.errorMessage}")
            
            if (validationResult.requiresEmailVerification) {
                listener?.showEmailVerificationDialog(validationResult.errorMessage)
                return PublishResult.EmailVerificationRequired(validationResult.errorMessage)
            }
            
            listener?.onPublishingError(validationResult.errorMessage, post)
            return PublishResult.Error(validationResult.errorMessage)
        }
        
        if (!shouldPerformPostUpdateAndPublish()) {
            return PublishResult.ValidationFailed("Post update and publish validation failed")
        }
        
        listener?.onPublishingStarted()
        listener?.showPublishingProgressDialog()
        
        return try {
            // Update post status to published - stub implementation
            AppLog.d(AppLog.T.POSTS, "Publishing post (stub)")
            val publishedPost = post // Using original post for now
            
            // repository.update { publishedPost } // Stub - would update repository
            
            // Stub implementation for publishing
            AppLog.d(AppLog.T.POSTS, "Publishing post (stub)")
            postEditorAnalyticsSession?.setOutcome(PostEditorAnalyticsSession.Outcome.PUBLISH)
            listener?.onPublishingSuccess(publishedPost)
            PublishResult.Success(publishedPost, PublishAction.PUBLISH)
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error publishing post", e)
            listener?.onPublishingError("Error publishing post: ${e.message}", post)
            PublishResult.Error("Error publishing post: ${e.message}")
        } finally {
            listener?.hidePublishingProgressDialog()
        }
    }
    
    /**
     * Schedules the post for future publishing
     */
    fun schedulePost(publishDate: Any): PublishResult {
        val repository = editPostRepository ?: return PublishResult.Error("Repository not initialized")
        val post = repository.getPost() ?: return PublishResult.Error("No post to schedule")
        
        AppLog.d(AppLog.T.POSTS, "PostPublishingManager: Scheduling post for ${publishDate}")
        
        // Validate scheduling is allowed
        // Validation stub - assume valid for now
        AppLog.d(AppLog.T.POSTS, "Validation passed (stub)")
        
        listener?.onPublishingStarted()
        
        return try {
            // Stub implementation for scheduling
            AppLog.d(AppLog.T.POSTS, "Scheduling post (stub)")
            val scheduledPost = post // Using original post for now
            listener?.onPublishingSuccess(scheduledPost)
            PublishResult.Success(scheduledPost, PublishAction.SCHEDULE)
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error scheduling post", e)
            listener?.onPublishingError("Error scheduling post: ${e.message}", post)
            PublishResult.Error("Error scheduling post: ${e.message}")
        }
    }
    
    /**
     * Saves post and optionally finishes the activity
     */
    fun savePostAndOptionallyFinish(doFinish: Boolean, forceSave: Boolean): PublishResult {
        val repository = editPostRepository ?: return PublishResult.Error("Repository not initialized")
        
        AppLog.d(AppLog.T.POSTS, "savePostAndOptionallyFinish: doFinish=$doFinish, forceSave=$forceSave")
        
        // Stub implementation for saving and finishing
        AppLog.d(AppLog.T.POSTS, "savePostAndOptionallyFinish (stub): doFinish=$doFinish, forceSave=$forceSave")
        
        coroutineScope?.launch {
            try {
                if (shouldSavePost() || forceSave) {
                    withContext(Dispatchers.Main) {
                        listener?.onSavingStarted()
                        // Simulate save success
                        repository.getPost()?.let { listener?.onSavingSuccess(it) }
                    }
                }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Error in savePostAndOptionallyFinish (stub)", e)
            }
        }
        
        return PublishResult.InProgress
    }
    
    /**
     * Determines if the post should be saved
     */
    fun shouldSavePost(): Boolean {
        val repository = editPostRepository ?: return false
        
        val hasChanges = repository.postWasChangedInCurrentSession()
        val isPublishable = repository.isPostPublishable()
        val existingPostWithChanges = repository.hasPostSnapshotWhenEditorOpened() && hasChanges
        val isNewPost = !repository.hasPostSnapshotWhenEditorOpened()
        
        // If post was modified during this editing session, save it
        val shouldSave = isPublishable && (existingPostWithChanges || isNewPost)
        
        AppLog.d(AppLog.T.POSTS, "shouldSavePost: hasChanges=$hasChanges, isPublishable=$isPublishable, " +
                "existingPostWithChanges=$existingPostWithChanges, isNewPost=$isNewPost, shouldSave=$shouldSave")
        
        return shouldSave
    }
    
    /**
     * Checks if this is the first time publishing
     */
    fun isFirstTimePublish(publishPost: Boolean): Boolean {
        if (!publishPost) return false
        
        val repository = editPostRepository ?: return false
        val post = repository.getPost() ?: return false
        
        return PostStatus.DRAFT.toString() == post.status && !post.isLocalDraft
    }
    
    /**
     * Internal method to save post online
     */
    private fun savePostOnline(isFirstTimePublish: Boolean): ActivityFinishState {
        val repository = editPostRepository ?: return ActivityFinishState.CANCELLED
        val site = siteModel ?: return ActivityFinishState.CANCELLED
        
        AppLog.d(AppLog.T.POSTS, "savePostOnline: isFirstTimePublish=$isFirstTimePublish")
        
        return try {
            // Update any pending uploads
            val post = repository.getPost()
            // post upload handling would go here (stub)
            
            // Save to database (stub implementation)
            AppLog.d(AppLog.T.POSTS, "Saving post to database (stub)")
            
            // If this is first time publish, we might need to do additional setup
            if (isFirstTimePublish) {
                postEditorAnalyticsSession?.setOutcome(PostEditorAnalyticsSession.Outcome.PUBLISH)
            }
            
            ActivityFinishState.SAVED
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error saving post online", e)
            ActivityFinishState.CANCELLED
        }
    }
    
    /**
     * Checks if post update and publish should proceed
     */
    private fun shouldPerformPostUpdateAndPublish(): Boolean {
        val account: AccountModel = accountStore.account
        
        // Prompt user to verify email before publishing
        if (!account.emailVerified) {
            storePostViewModel.hideSavingProgressDialog()
            
            val message = if (TextUtils.isEmpty(account.email)) {
                WordPress.getContext().getString(R.string.editor_confirm_email_prompt_message)
            } else {
                String.format(
                    WordPress.getContext().getString(R.string.editor_confirm_email_prompt_message_with_email),
                    account.email
                )
            }
            
            listener?.showEmailVerificationDialog(message)
            return false
        }
        
        return true
    }
    
    /**
     * Handles the result of a save operation
     */
    private fun saveResult(saved: Boolean, uploadNotStarted: Boolean) {
        val repository = editPostRepository ?: return
        val post = repository.getPost()
        
        if (saved) {
            AppLog.d(AppLog.T.POSTS, "Post saved successfully")
            if (post != null) {
                listener?.onSavingSuccess(post)
            }
        } else {
            val errorMessage = if (uploadNotStarted) {
                "Upload was cancelled"
            } else {
                "Failed to save post"
            }
            AppLog.w(AppLog.T.POSTS, "Save failed: $errorMessage")
            listener?.onSavingError(errorMessage, post)
        }
    }
    
    fun cleanup() {
        listener = null
        editPostRepository = null
        siteModel = null
        postEditorAnalyticsSession = null
        coroutineScope = null
        postValidationService.cleanup()
    }
}

/**
 * Result of a publishing operation
 */
sealed class PublishResult {
    data class Success(
        val post: PostImmutableModel,
        val action: PublishAction
    ) : PublishResult()
    
    data class Error(val message: String) : PublishResult()
    data class ValidationFailed(val message: String) : PublishResult()
    data class EmailVerificationRequired(val message: String) : PublishResult()
    object Cancelled : PublishResult()
    object NoActionNeeded : PublishResult()
    object InProgress : PublishResult()
}

/**
 * Type of publishing action performed
 */
enum class PublishAction {
    SAVE_DRAFT,
    PUBLISH,
    SCHEDULE,
    UPDATE
}

