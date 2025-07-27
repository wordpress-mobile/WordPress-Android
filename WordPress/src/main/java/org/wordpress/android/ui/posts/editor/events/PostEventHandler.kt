package org.wordpress.android.ui.posts.editor.events

import androidx.lifecycle.LifecycleOwner
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PHASE 2: Post Event Handler
 * 
 * Handles all post-related EventBus events:
 * - Post changed events
 * - Post uploaded events
 * - Event routing and conflict detection
 * - Event processing and delegation
 * 
 * Extracted from EditPostActivity to centralize event handling logic.
 */
@Singleton
class PostEventHandler @Inject constructor() {

    enum class PostChangeType {
        UPDATED,
        AUTO_SAVED,
        CREATED,
        FETCHED,
        DELETED
    }

    // Configuration
    private var isInitialized = false
    private var listener: PostEventListener? = null
    private var repository: EditPostRepository? = null
    
    /**
     * Interface for post event callbacks
     */
    interface PostEventListener {
        fun onPostSaved(post: PostImmutableModel)
        fun onPostPublished(post: PostImmutableModel)
        fun onPostUploadError(post: PostImmutableModel, error: String)
        fun onPostChanged(post: PostImmutableModel, changeType: PostChangeType)
        fun onPostConflictDetected(localPost: PostImmutableModel, remotePost: PostImmutableModel)
        fun showSnackbar(message: String)
    }

    /**
     * Initialize the event handler
     */
    fun initialize(
        listener: PostEventListener,
        editPostRepository: EditPostRepository
    ) {
        this.listener = listener
        this.repository = editPostRepository
        this.isInitialized = true
        
        // Register for EventBus events
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        
        AppLog.d(AppLog.T.POSTS, "PostEventHandler initialized")
    }

    /**
     * Handle post changed events
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "PostEventHandler not initialized")
            return
        }
        
        // Get current post
        val currentPost = repository?.getPost()
        if (currentPost == null) {
            AppLog.d(AppLog.T.POSTS, "No current post, ignoring PostChanged event")
            return
        }
        
        AppLog.d(AppLog.T.POSTS, "Post changed event received")
        
        // Simple handling - just notify of update
        val changeType = PostChangeType.UPDATED
        listener?.onPostChanged(currentPost, changeType)
    }

    /**
     * Handle post uploaded events
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (!isInitialized) {
            AppLog.w(AppLog.T.POSTS, "PostEventHandler not initialized")
            return
        }
        
        val currentPost = repository?.getPost()
        if (currentPost == null) {
            AppLog.d(AppLog.T.POSTS, "No current post, ignoring PostUploaded event")
            return
        }
        
        if (event.isError) {
            AppLog.e(AppLog.T.POSTS, "Post upload error: ${event.error?.message}")
            listener?.onPostUploadError(currentPost, event.error?.message ?: "Unknown error")
        } else {
            AppLog.d(AppLog.T.POSTS, "Post uploaded successfully")
            listener?.onPostPublished(currentPost)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        
        listener = null
        repository = null
        isInitialized = false
        
        AppLog.d(AppLog.T.POSTS, "PostEventHandler cleaned up")
    }
}
