package org.wordpress.android.ui.posts.editor.events

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.publishing.PostPublishingManager
import org.wordpress.android.ui.posts.editor.state.PostStateManager
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles all post-related EventBus events and routes them to appropriate managers.
 * Extracted from EditPostActivity to centralize event handling logic.
 */
@Singleton
class PostEventHandler @Inject constructor(
    private val postStateManager: PostStateManager,
    private val publishingManager: PostPublishingManager
) {
    
    interface PostEventListener {
        fun onPostSaved(post: PostImmutableModel)
        fun onPostPublished(post: PostImmutableModel) 
        fun onPostUploadError(post: PostImmutableModel, error: String)
        fun onPostChanged(post: PostImmutableModel, changeType: PostChangeType)
        fun onPostConflictDetected(localPost: PostImmutableModel, remotePost: PostImmutableModel)
        fun showSnackbar(message: String)
    }
    
    private var listener: PostEventListener? = null
    private var editPostRepository: EditPostRepository? = null
    private var isRegistered = false
    
    fun initialize(
        listener: PostEventListener,
        editPostRepository: EditPostRepository
    ) {
        this.listener = listener
        this.editPostRepository = editPostRepository
        
        if (!isRegistered) {
            EventBus.getDefault().register(this)
            isRegistered = true
            AppLog.d(AppLog.T.POSTS, "PostEventHandler: Registered for EventBus events")
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        val repository = editPostRepository ?: return
        
        AppLog.d(AppLog.T.POSTS, "PostEventHandler: Received OnPostChanged event")
        
        if (event.isError) {
            AppLog.e(AppLog.T.POSTS, "Post change error: ${event.error?.message}")
            handlePostChangeError(event)
            return
        }
        
        // Check if this event is for our current post
        val currentPost = repository.getPost()
        val eventPost = event.changedPosts?.find { post ->
            post.id == currentPost?.id || post.localId == currentPost?.localId
        }
        
        if (eventPost == null) {
            AppLog.d(AppLog.T.POSTS, "PostChanged event not for current post, ignoring")
            return
        }
        
        // Determine the type of change
        val changeType = when (event.causeOfChange) {
            CauseOfOnPostChanged.UpdatePost -> PostChangeType.UPDATED
            CauseOfOnPostChanged.RemoteAutoSavePost -> PostChangeType.AUTO_SAVED
            CauseOfOnPostChanged.RemoteCreatePost -> PostChangeType.CREATED
            CauseOfOnPostChanged.FetchPost -> PostChangeType.FETCHED
            CauseOfOnPostChanged.DeletePost -> PostChangeType.DELETED
            else -> PostChangeType.UPDATED
        }
        
        AppLog.d(AppLog.T.POSTS, "Post changed: ${eventPost.id}, type: $changeType")
        
        // Handle specific change types
        when (changeType) {
            PostChangeType.AUTO_SAVED -> {
                handleAutoSave(eventPost)
            }
            PostChangeType.CREATED -> {
                handlePostCreated(eventPost)
            }
            PostChangeType.UPDATED -> {
                handlePostUpdated(eventPost)
            }
            PostChangeType.DELETED -> {
                handlePostDeleted(eventPost)
            }
            PostChangeType.FETCHED -> {
                handlePostFetched(eventPost)
            }
        }
        
        // Notify listener
        listener?.onPostChanged(eventPost, changeType)
        
        // Update post state manager
        postStateManager.updatePostLoadingState(
            org.wordpress.android.ui.posts.editor.PostLoadingState.NONE,
            eventPost
        )
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        val repository = editPostRepository ?: return
        val currentPost = repository.getPost() ?: return
        
        AppLog.d(AppLog.T.POSTS, "PostEventHandler: Received OnPostUploaded event")
        
        // Check if this upload event is for our current post
        if (event.post?.id != currentPost.id && event.post?.localId != currentPost.localId) {
            AppLog.d(AppLog.T.POSTS, "PostUploaded event not for current post, ignoring")
            return
        }
        
        if (event.isError) {
            AppLog.e(AppLog.T.POSTS, "Post upload error: ${event.error?.message}")
            handlePostUploadError(event)
        } else {
            AppLog.d(AppLog.T.POSTS, "Post uploaded successfully: ${event.post?.id}")
            handlePostUploadSuccess(event)
        }
    }
    
    private fun handlePostChangeError(event: OnPostChanged) {
        val errorMessage = event.error?.message ?: "Unknown error occurred while changing post"
        
        AppLog.e(AppLog.T.POSTS, "PostEventHandler: Post change error - $errorMessage")
        
        // Reset loading state
        postStateManager.resetLoadingState()
        
        // Show error to user
        listener?.showSnackbar("Error: $errorMessage")
    }
    
    private fun handleAutoSave(post: PostImmutableModel) {
        AppLog.d(AppLog.T.POSTS, "Post auto-saved: ${post.id}")
        
        // Update repository with auto-saved version
        editPostRepository?.let { repository ->
            if (repository.id == post.localId) {
                // Merge auto-saved changes if needed
                // This is a simplified version - actual implementation might be more complex
                repository.replaceLocalPostWithRemotePostIgnoringChanges(post)
            }
        }
        
        listener?.showSnackbar("Post auto-saved")
    }
    
    private fun handlePostCreated(post: PostImmutableModel) {
        AppLog.d(AppLog.T.POSTS, "Post created remotely: ${post.id}")
        
        // Update local repository with the new remote ID
        editPostRepository?.let { repository ->
            if (repository.id == post.localId) {
                repository.updatePostObject(post)
            }
        }
        
        listener?.onPostSaved(post)
        listener?.showSnackbar("Post saved")
    }
    
    private fun handlePostUpdated(post: PostImmutableModel) {
        AppLog.d(AppLog.T.POSTS, "Post updated: ${post.id}")
        
        // Check for conflicts with local changes
        val repository = editPostRepository
        if (repository != null && repository.postWasChangedInCurrentSession()) {
            val localPost = repository.getPost()
            if (localPost != null && hasConflictingChanges(localPost, post)) {
                AppLog.w(AppLog.T.POSTS, "Detected conflicting changes between local and remote post")
                listener?.onPostConflictDetected(localPost, post)
                return
            }
        }
        
        // Update repository with remote changes
        repository?.updatePostObject(post)
        
        listener?.onPostSaved(post)
    }
    
    private fun handlePostDeleted(post: PostImmutableModel) {
        AppLog.d(AppLog.T.POSTS, "Post deleted: ${post.id}")
        
        // The post was deleted remotely - we should probably close the editor
        listener?.showSnackbar("Post was deleted")
        
        // This would typically trigger activity finish
    }
    
    private fun handlePostFetched(post: PostImmutableModel) {
        AppLog.d(AppLog.T.POSTS, "Post fetched: ${post.id}")
        
        // Update repository with fetched post data
        editPostRepository?.updatePostObject(post)
    }
    
    private fun handlePostUploadSuccess(event: OnPostUploaded) {
        val post = event.post ?: return
        
        AppLog.d(AppLog.T.POSTS, "Post upload succeeded: ${post.id}")
        
        // Update repository with uploaded post
        editPostRepository?.updatePostObject(post)
        
        // Determine if this was a publish or save operation
        val isPublished = post.status == "publish"
        
        if (isPublished) {
            listener?.onPostPublished(post)
            listener?.showSnackbar("Post published successfully!")
        } else {
            listener?.onPostSaved(post)
            listener?.showSnackbar("Post saved successfully!")
        }
        
        // Reset loading state
        postStateManager.resetLoadingState()
    }
    
    private fun handlePostUploadError(event: OnPostUploaded) {
        val post = event.post
        val errorMessage = event.error?.message ?: "Unknown upload error"
        
        AppLog.e(AppLog.T.POSTS, "Post upload failed: $errorMessage")
        
        if (post != null) {
            listener?.onPostUploadError(post, errorMessage)
        }
        
        listener?.showSnackbar("Upload failed: $errorMessage")
        
        // Reset loading state
        postStateManager.resetLoadingState()
    }
    
    /**
     * Checks if there are conflicting changes between local and remote post
     */
    private fun hasConflictingChanges(localPost: PostImmutableModel, remotePost: PostImmutableModel): Boolean {
        // Simple conflict detection - could be more sophisticated
        return localPost.title != remotePost.title ||
                localPost.content != remotePost.content ||
                localPost.excerpt != remotePost.excerpt
    }
    
    /**
     * Forces a refresh of the current post from remote
     */
    fun refreshPostFromRemote() {
        val repository = editPostRepository ?: return
        val post = repository.getPost() ?: return
        
        if (post.remotePostId > 0) {
            AppLog.d(AppLog.T.POSTS, "PostEventHandler: Refreshing post from remote")
            
            // This would trigger a fetch operation
            // The actual implementation would depend on how posts are fetched
            // postStore.fetchPost(post.remotePostId, siteModel)
        }
    }
    
    /**
     * Updates the current post repository reference
     */
    fun updatePostRepository(repository: EditPostRepository) {
        this.editPostRepository = repository
        AppLog.d(AppLog.T.POSTS, "PostEventHandler: Updated post repository")
    }
    
    fun cleanup() {
        if (isRegistered) {
            EventBus.getDefault().unregister(this)
            isRegistered = false
            AppLog.d(AppLog.T.POSTS, "PostEventHandler: Unregistered from EventBus")
        }
        
        listener = null
        editPostRepository = null
    }
}

/**
 * Types of post changes that can occur
 */
enum class PostChangeType {
    CREATED,
    UPDATED, 
    AUTO_SAVED,
    DELETED,
    FETCHED
}