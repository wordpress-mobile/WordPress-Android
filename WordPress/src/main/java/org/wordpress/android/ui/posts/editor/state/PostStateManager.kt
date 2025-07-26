package org.wordpress.android.ui.posts.editor.state

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.PostLoadingState
import org.wordpress.android.ui.posts.editor.StorePostViewModel
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages post loading states and UI state transitions for the editor.
 * Extracted from EditPostActivity to centralize state management logic.
 */
@Singleton
class PostStateManager @Inject constructor(
    private val storePostViewModel: StorePostViewModel
) {
    
    interface StateListener {
        fun onPostLoadingStateChanged(state: PostLoadingState, post: PostImmutableModel?)
        fun onPostSnapshotSaved()
        fun onPostUpdatedFromEditor(post: PostImmutableModel)
        fun onPostContentChanged(hasChanges: Boolean)
        fun showLoadingDialog(message: String)
        fun hideLoadingDialog()
        fun updateToolbarState()
    }
    
    private var listener: StateListener? = null
    private var editPostRepository: EditPostRepository? = null
    private var siteModel: SiteModel? = null
    private var currentLoadingState: PostLoadingState = PostLoadingState.NONE
    
    fun initialize(
        listener: StateListener,
        lifecycleOwner: LifecycleOwner,
        editPostRepository: EditPostRepository,
        siteModel: SiteModel
    ) {
        this.listener = listener
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        
        observePostChanges(lifecycleOwner)
    }
    
    private fun observePostChanges(lifecycleOwner: LifecycleOwner) {
        // Observe post content changes
        editPostRepository?.let { repository ->
            // This would typically observe LiveData from the repository
            // repository.postLiveData.observe(lifecycleOwner, Observer { post ->
            //     onPostContentChanged(post)
            // })
        }
    }
    
    /**
     * Updates the post loading state and notifies listeners
     */
    fun updatePostLoadingState(state: PostLoadingState, post: PostImmutableModel? = null) {
        if (currentLoadingState == state) {
            AppLog.d(AppLog.T.POSTS, "PostStateManager: State already set to $state, ignoring")
            return
        }
        
        val previousState = currentLoadingState
        currentLoadingState = state
        
        AppLog.d(AppLog.T.POSTS, "PostStateManager: State changed from $previousState to $state")
        
        // Handle state-specific logic
        when (state) {
            PostLoadingState.UPLOADING_FOR_PREVIEW -> {
                listener?.showLoadingDialog("Preparing preview...")
            }
            PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW -> {
                listener?.showLoadingDialog("Auto-saving for preview...")
            }
            PostLoadingState.SAVING -> {
                listener?.showLoadingDialog("Saving post...")
            }
            PostLoadingState.UPLOADING -> {
                listener?.showLoadingDialog("Publishing post...")
            }
            PostLoadingState.NONE -> {
                listener?.hideLoadingDialog()
            }
            else -> {
                // Other states might not need dialog updates
            }
        }
        
        // Notify listener of state change
        listener?.onPostLoadingStateChanged(state, post)
        
        // Update toolbar based on new state
        listener?.updateToolbarState()
    }
    
    /**
     * Handles post updates from the editor
     */
    fun updatePostFromEditor(
        title: String? = null,
        content: String? = null,
        excerpt: String? = null
    ) {
        val repository = editPostRepository ?: return
        
        AppLog.d(AppLog.T.POSTS, "PostStateManager: Updating post from editor")
        
        try {
            repository.updateAsync({ post ->
                val updatedPost = post.copy()
                
                title?.let { updatedPost.title = it }
                content?.let { updatedPost.content = it }
                excerpt?.let { updatedPost.excerpt = it }
                
                // Update modification date
                updatedPost.dateLocallyChanged = System.currentTimeMillis().toString()
                
                updatedPost
            }, { updatedPost ->
                // Callback when update is complete
                AppLog.d(AppLog.T.POSTS, "Post updated successfully from editor")
                listener?.onPostUpdatedFromEditor(updatedPost)
                
                // Check if post has changes
                val hasChanges = repository.postWasChangedInCurrentSession()
                listener?.onPostContentChanged(hasChanges)
            })
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error updating post from editor", e)
        }
    }
    
    /**
     * Saves a snapshot of the current post state
     */
    fun savePostSnapshot() {
        val repository = editPostRepository ?: return
        
        AppLog.d(AppLog.T.POSTS, "PostStateManager: Saving post snapshot")
        
        try {
            repository.savePostSnapshot()
            listener?.onPostSnapshotSaved()
            AppLog.d(AppLog.T.POSTS, "Post snapshot saved successfully")
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error saving post snapshot", e)
        }
    }
    
    /**
     * Saves snapshot when editor is first opened
     */
    fun savePostSnapshotWhenEditorOpened() {
        val repository = editPostRepository ?: return
        
        if (!repository.hasPostSnapshotWhenEditorOpened()) {
            AppLog.d(AppLog.T.POSTS, "PostStateManager: Saving initial post snapshot")
            repository.savePostSnapshotWhenEditorOpened()
        }
    }
    
    /**
     * Checks if the post has been modified in the current session
     */
    fun hasPostChanged(): Boolean {
        val repository = editPostRepository ?: return false
        return repository.postWasChangedInCurrentSession()
    }
    
    /**
     * Checks if the post is publishable
     */
    fun isPostPublishable(): Boolean {
        val repository = editPostRepository ?: return false
        return repository.isPostPublishable()
    }
    
    /**
     * Initializes the post object in the repository
     */
    fun initializePostObject(
        postLocalId: Int? = null,
        isNewPost: Boolean = false,
        isPage: Boolean = false
    ) {
        val repository = editPostRepository ?: return
        val site = siteModel ?: return
        
        AppLog.d(AppLog.T.POSTS, "PostStateManager: Initializing post object (localId=$postLocalId, new=$isNewPost, page=$isPage)")
        
        try {
            if (postLocalId != null && postLocalId != 0) {
                // Load existing post
                repository.loadPostByLocalPostId(postLocalId)
                if (repository.hasPost()) {
                    AppLog.d(AppLog.T.POSTS, "Loaded existing post: ${repository.id}")
                } else {
                    AppLog.w(AppLog.T.POSTS, "Failed to load post with localId: $postLocalId")
                }
            } else {
                // Create new post
                val newPost = PostModel().apply {
                    localSiteId = site.id
                    isPage = isPage
                    status = "draft"
                    title = ""
                    content = ""
                    excerpt = ""
                    dateCreated = System.currentTimeMillis().toString()
                    dateLocallyChanged = System.currentTimeMillis().toString()
                }
                
                repository.set { newPost }
                AppLog.d(AppLog.T.POSTS, "Created new ${if (isPage) "page" else "post"}")
            }
            
            // Save initial snapshot
            savePostSnapshotWhenEditorOpened()
            
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error initializing post object", e)
        }
    }
    
    /**
     * Gets the current loading state
     */
    fun getCurrentLoadingState(): PostLoadingState {
        return currentLoadingState
    }
    
    /**
     * Checks if the post is currently in a loading state
     */
    fun isInLoadingState(): Boolean {
        return currentLoadingState != PostLoadingState.NONE
    }
    
    /**
     * Resets the loading state to NONE
     */
    fun resetLoadingState() {
        updatePostLoadingState(PostLoadingState.NONE)
    }
    
    /**
     * Handles recovery from loading state errors
     */
    fun handleLoadingStateError(errorMessage: String) {
        AppLog.e(AppLog.T.POSTS, "PostStateManager: Loading state error - $errorMessage")
        
        // Reset to normal state
        updatePostLoadingState(PostLoadingState.NONE)
        
        // Could notify listener about the error if needed
        // This would depend on how errors are handled in the UI
    }
    
    /**
     * Updates post status
     */
    fun updatePostStatus(status: String) {
        val repository = editPostRepository ?: return
        
        AppLog.d(AppLog.T.POSTS, "PostStateManager: Updating post status to $status")
        
        repository.updateAsync({ post ->
            post.copy().apply {
                this.status = status
                this.dateLocallyChanged = System.currentTimeMillis().toString()
            }
        }, { updatedPost ->
            AppLog.d(AppLog.T.POSTS, "Post status updated to $status")
            listener?.onPostUpdatedFromEditor(updatedPost)
        })
    }
    
    /**
     * Gets summary information about the current post state
     */
    fun getPostStateSummary(): PostStateSummary {
        val repository = editPostRepository ?: return PostStateSummary.empty()
        
        return PostStateSummary(
            hasPost = repository.hasPost(),
            isPublishable = repository.isPostPublishable(),
            hasChanges = repository.postWasChangedInCurrentSession(),
            hasSnapshot = repository.hasPostSnapshotWhenEditorOpened(),
            loadingState = currentLoadingState,
            postId = repository.id,
            postStatus = repository.getPost()?.status ?: "unknown"
        )
    }
    
    fun cleanup() {
        listener = null
        editPostRepository = null
        siteModel = null
        currentLoadingState = PostLoadingState.NONE
    }
}

/**
 * Summary of the current post state
 */
data class PostStateSummary(
    val hasPost: Boolean,
    val isPublishable: Boolean,
    val hasChanges: Boolean,
    val hasSnapshot: Boolean,
    val loadingState: PostLoadingState,
    val postId: Int,
    val postStatus: String
) {
    companion object {
        fun empty() = PostStateSummary(
            hasPost = false,
            isPublishable = false,
            hasChanges = false,
            hasSnapshot = false,
            loadingState = PostLoadingState.NONE,
            postId = 0,
            postStatus = "unknown"
        )
    }
}