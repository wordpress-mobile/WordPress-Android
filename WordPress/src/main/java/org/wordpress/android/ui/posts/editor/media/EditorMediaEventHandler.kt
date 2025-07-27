package org.wordpress.android.ui.posts.editor.media

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.ui.uploads.ProgressEvent
import org.wordpress.android.ui.uploads.UploadService.UploadMediaRetryEvent
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles EventBus events related to media operations and forwards them to EditorMediaManager.
 * This separates the EventBus concerns from the media management logic.
 */
@Singleton
class EditorMediaEventHandler @Inject constructor(
    private val mediaManager: EditorMediaManager
) {
    
    interface MediaEventListener {
        fun onMediaUploadProgress(mediaModel: MediaModel, progress: Float)
        fun onMediaUploadCompleted(mediaModel: MediaModel, isError: Boolean)
        fun onMediaRetryRequested(mediaModel: MediaModel)
    }
    
    private var listener: MediaEventListener? = null
    private var currentPost: PostModel? = null
    private var isRegistered = false
    
    fun initialize(listener: MediaEventListener, post: PostModel) {
        this.listener = listener
        this.currentPost = post
        
        if (!isRegistered) {
            EventBus.getDefault().register(this)
            isRegistered = true
        }
    }
    
    fun cleanup() {
        if (isRegistered) {
            EventBus.getDefault().unregister(this)
            isRegistered = false
        }
        listener = null
        currentPost = null
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        val post = currentPost ?: return
        
        if (event.media == null) {
            AppLog.w(AppLog.T.POSTS, "MediaEventHandler: Received OnMediaUploaded with null media")
            return
        }
        
        // Only handle media for the current post
        val media = event.media
        if (media != null && isMediaForCurrentPost(media, post)) {
            if (event.isError) {
                AppLog.e(AppLog.T.POSTS, "Media upload failed: ${event.error?.message}")
                mediaManager.onUploadError(media, event.error?.message ?: "Unknown error")
                listener?.onMediaUploadCompleted(media, true)
            } else {
                AppLog.d(AppLog.T.POSTS, "Media upload succeeded: ${media.id}")
                mediaManager.onUploadSuccess(media)
                listener?.onMediaUploadCompleted(media, false)
            }
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaChanged(event: OnMediaChanged) {
        val post = currentPost ?: return
        
        if (event.mediaList.isNotEmpty()) {
            // Handle media state changes
            event.mediaList.forEach { media ->
                if (isMediaForCurrentPost(media, post)) {
                    when (media.uploadState) {
                        MediaModel.MediaUploadState.UPLOADING.toString() -> {
                            AppLog.d(AppLog.T.POSTS, "Media upload started: ${media.id}")
                        }
                        MediaModel.MediaUploadState.UPLOADED.toString() -> {
                            AppLog.d(AppLog.T.POSTS, "Media upload completed: ${media.id}")
                            mediaManager.onUploadSuccess(media)
                            listener?.onMediaUploadCompleted(media, false)
                        }
                        MediaModel.MediaUploadState.FAILED.toString() -> {
                            AppLog.e(AppLog.T.POSTS, "Media upload failed: ${media.id}")
                            mediaManager.onUploadError(media, "Upload failed")
                            listener?.onMediaUploadCompleted(media, true)
                        }
                    }
                }
            }
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        if (event.isError) {
            AppLog.e(AppLog.T.POSTS, "Error fetching media list: ${event.error?.message}")
        } else {
            AppLog.d(AppLog.T.POSTS, "Media list fetched successfully")
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProgressEvent(event: ProgressEvent) {
        val post = currentPost ?: return
        
        val media = event.media
        if (isMediaForCurrentPost(media, post)) {
            val progress = event.progress ?: 0f
            AppLog.d(AppLog.T.POSTS, "Media upload progress: ${media.id} - ${progress}%")
            
            mediaManager.onUploadProgress(media, progress)
            listener?.onMediaUploadProgress(media, progress)
        }
    }
    
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUploadMediaRetryEvent(event: UploadMediaRetryEvent) {
        val post = currentPost ?: return
        
        if (event.mediaModelList.isNotEmpty()) {
            event.mediaModelList.forEach { media ->
                if (isMediaForCurrentPost(media, post)) {
                    AppLog.d(AppLog.T.POSTS, "Retrying media upload: ${media.id}")
                    listener?.onMediaRetryRequested(media)
                }
            }
        }
    }
    
    /**
     * Checks if the given media belongs to the current post
     */
    private fun isMediaForCurrentPost(media: MediaModel, post: PostModel): Boolean {
        // Check if media is associated with the current post
        return media.postId == post.id.toLong() || 
               media.localPostId == post.id ||
               isMediaInPostContent(media, post)
    }
    
    /**
     * Checks if media is referenced in the post content
     */
    private fun isMediaInPostContent(media: MediaModel, post: PostModel): Boolean {
        val content = post.content
        return content.contains(media.id.toString()) || 
               content.contains(media.url ?: "") ||
               (!media.filePath.isNullOrEmpty() && content.contains(media.filePath ?: ""))
    }
    
    fun updateCurrentPost(post: PostModel) {
        this.currentPost = post
        AppLog.d(AppLog.T.POSTS, "MediaEventHandler: Updated current post to ${post.id}")
    }
}
