@file:Suppress("DEPRECATION")
package org.wordpress.android.ui.posts

// ... existing imports ...
import org.wordpress.android.ui.posts.editor.media.EditorMediaManager
import org.wordpress.android.ui.posts.editor.media.EditorMediaEventHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * REFACTORED EditPostActivity - Shows how to integrate the new MediaManager
 * This demonstrates the key changes needed to use the extracted media management components.
 * 
 * Before refactoring: 4,251 lines
 * After media extraction: ~3,800 lines (450+ lines moved to MediaManager)
 */
@AndroidEntryPoint
@Suppress("LargeClass") // Still large, but significantly reduced
class EditPostActivity : BaseAppCompatActivity(), 
    EditorFragmentActivity, 
    EditorImageSettingsListener,
    EditorImagePreviewListener, 
    EditorEditMediaListener, 
    EditorDragAndDropListener, 
    EditorFragmentListener,
    ActivityCompat.OnRequestPermissionsResultCallback,
    PhotoPickerListener, 
    EditorPhotoPickerListener, 
    EditorMediaListener, 
    EditPostActivityHook,
    OnPostSettingsDialogFragmentListener, 
    HistoryItemClickInterface, 
    EditPostSettingsCallback,
    PrepublishingBottomSheetListener, 
    PrivateAtCookieProgressDialogOnDismissListener, 
    ExceptionLogger,
    SiteSettingsListener,
    EditorMediaManager.MediaManagerListener,           // NEW: Implement MediaManager interface
    EditorMediaEventHandler.MediaEventListener {      // NEW: Implement EventHandler interface

    // ... existing injected dependencies ...
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var userAgent: UserAgent
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var uploadStore: UploadStore
    @Inject lateinit var editorThemeStore: EditorThemeStore
    @Inject lateinit var imageLoader: FluxCImageLoader
    @Inject lateinit var shortcutUtils: ShortcutUtils
    @Inject lateinit var quickStartStore: QuickStartStore
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper
    @Inject lateinit var progressDialogHelper: ProgressDialogHelper
    @Inject lateinit var featuredImageHelper: FeaturedImageHelper
    @Inject lateinit var reactNativeRequestHandler: ReactNativeRequestHandler
    @Inject lateinit var editorMedia: EditorMedia
    @Inject lateinit var perAppLocaleManager: PerAppLocaleManager
    @Inject internal lateinit var editPostRepository: EditPostRepository
    
    // NEW: Inject the new media management components
    @Inject lateinit var mediaManager: EditorMediaManager
    @Inject lateinit var mediaEventHandler: EditorMediaEventHandler

    // ... existing private variables ...
    var aztecImageLoader: AztecImageLoader? = null
    
    // REMOVED: All the media-related private variables are now in MediaManager
    // private var pendingVideoPressInfoRequests: MutableList<String>? = null
    // private var editorMediaUploadListener: EditorMediaUploadListener? = null
    // etc.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ... existing setup code ...
        
        // NEW: Initialize the media manager and event handler
        initializeMediaManagement()
        
        // ... rest of existing onCreate code ...
    }
    
    /**
     * NEW: Initialize media management components
     */
    private fun initializeMediaManagement() {
        // Initialize the media manager with required dependencies
        mediaManager.initialize(
            listener = this,
            lifecycleOwner = this,
            site = siteModel,
            post = post,
            aztecImageLoader = aztecImageLoader
        )
        
        // Initialize the event handler
        mediaEventHandler.initialize(
            listener = this,
            post = post
        )
    }

    // ========================================
    // REFACTORED: Media-related methods now delegate to MediaManager
    // ========================================

    /**
     * REFACTORED: Simplified media selection handling
     * Before: 20+ lines of complex logic
     * After: Simple delegation to MediaManager
     */
    override fun onPhotoPickerMediaChosen(uriList: List<Uri>) {
        editorPhotoPicker?.hidePhotoPicker()
        
        // BEFORE: Complex logic here for processing URIs
        // AFTER: Simple delegation
        mediaManager.handleMediaSelection(uriList)
    }

    /**
     * REFACTORED: Media library opening
     * Before: Direct implementation with MediaPickerLauncher
     * After: Delegated to MediaManager
     */
    private fun openMediaLibrary(mediaType: MediaBrowserType, initialSelection: List<Int> = emptyList()) {
        // BEFORE: Direct mediaPickerLauncher.viewWPMediaLibraryPickerForResult() call
        // AFTER: Delegated to manager
        mediaManager.openMediaLibrary(mediaType, initialSelection)
    }

    /**
     * REFACTORED: Gutenberg media type mapping
     * Before: Implemented directly in class (40+ lines)
     * After: Delegated to MediaManager
     */
    private fun mapAllowedTypesToMediaBrowserType(allowedTypes: Array<MediaType>, multiple: Boolean): MediaBrowserType {
        return mediaManager.mapAllowedTypesToMediaBrowserType(allowedTypes, multiple)
    }

    /**
     * REFACTORED: Failed media check
     * Before: Complex database queries and state checking
     * After: Simple delegation
     */
    private fun hasFailedMedia(): Boolean {
        return mediaManager.hasFailedMedia(post)
    }

    /**
     * REFACTORED: Reset uploading media
     * Before: Complex state management logic
     * After: Delegated to MediaManager
     */
    private fun resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued() {
        mediaManager.resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued(post)
    }

    // ========================================
    // NEW: MediaManagerListener Implementation
    // ========================================

    override fun onMediaUploadStarted(mediaModel: MediaModel) {
        AppLog.d(AppLog.T.POSTS, "Media upload started: ${mediaModel.id}")
        // Update UI to show upload progress
        updateUploadProgress(mediaModel, 0f)
    }

    override fun onMediaUploadProgress(mediaModel: MediaModel, progress: Float) {
        AppLog.d(AppLog.T.POSTS, "Media upload progress: ${mediaModel.id} - ${progress}%")
        updateUploadProgress(mediaModel, progress)
    }

    override fun onMediaUploadSuccess(mediaModel: MediaModel) {
        AppLog.d(AppLog.T.POSTS, "Media upload success: ${mediaModel.id}")
        // Update editor with successful media
        editorFragment?.onMediaUploadSucceeded(mediaModel)
        hideProgressDialog()
    }

    override fun onMediaUploadError(mediaModel: MediaModel, error: String) {
        AppLog.e(AppLog.T.POSTS, "Media upload error: ${mediaModel.id} - $error")
        // Show error state in editor
        editorFragment?.onMediaUploadFailed(mediaModel, error)
        hideProgressDialog()
        showSnackbar(getString(R.string.media_upload_failed, error))
    }

    override fun onMediaLibraryRequested(mediaType: MediaBrowserType, initialSelection: List<Int>) {
        // Delegate to the existing media picker launcher
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            activity = this,
            site = siteModel,
            browserType = mediaType,
            initialSelection = initialSelection
        )
    }

    override fun showProgressDialog(message: String) {
        progressDialogHelper.showProgressDialog(message)
    }

    override fun hideProgressDialog() {
        progressDialogHelper.hideProgressDialog()
    }

    // ========================================
    // NEW: MediaEventListener Implementation
    // ========================================

    override fun onMediaRetryRequested(mediaModel: MediaModel) {
        AppLog.d(AppLog.T.POSTS, "Retrying media upload: ${mediaModel.id}")
        mediaManager.retryFailedUploads(post)
    }

    // ========================================
    // GUTENBERG INTEGRATION: Simplified with MediaManager
    // ========================================

    /**
     * REFACTORED: Gutenberg media library callback
     * Before: 50+ lines of type checking and configuration
     * After: Clean delegation to MediaManager
     */
    private fun setupGutenbergMediaHandling() {
        editorFragment?.setOnLogJsExceptionListener(object : GutenbergView.LogJsExceptionListener {
            override fun onLogJsException(exception: GutenbergJsException) {
                crashLogging.recordJsException(JsException(exception))
            }
        })

        // REFACTORED: Media library handling
        editorFragment?.setOpenMediaLibraryListener(object : GutenbergView.OpenMediaLibraryListener {
            override fun onOpenMediaLibrary(config: GutenbergView.OpenMediaLibraryConfig) {
                val mediaType = mediaManager.mapAllowedTypesToMediaBrowserType(
                    config.allowedTypes,
                    config.allowMultiple
                )
                mediaManager.openMediaLibrary(mediaType, config.initialSelection)
            }
        })
    }

    // ========================================
    // LIFECYCLE: Proper cleanup of new components
    // ========================================

    override fun onDestroy() {
        super.onDestroy()
        
        // NEW: Cleanup media management components
        mediaManager.cleanup()
        mediaEventHandler.cleanup()
        
        // ... existing cleanup code ...
    }

    override fun onPause() {
        super.onPause()
        // Update the current post in event handler when pausing
        mediaEventHandler.updateCurrentPost(post)
    }

    // ========================================
    // HELPER METHODS: Simplified with MediaManager
    // ========================================

    private fun updateUploadProgress(mediaModel: MediaModel, progress: Float) {
        // Update any progress indicators in the UI
        // This could update progress bars, notifications, etc.
        editorFragment?.updateMediaUploadProgress(mediaModel, progress)
    }

    private fun showSnackbar(message: String) {
        make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    // ========================================
    // REMOVED METHODS: These are now in MediaManager
    // ========================================
    
    // REMOVED: private fun onUploadSuccess(media: MediaModel?) { ... }
    // REMOVED: private fun onUploadProgress(media: MediaModel?, progress: Float) { ... }
    // REMOVED: private fun getUploadErrorHtml(mediaId: String, path: String): String { ... }
    // REMOVED: private fun setPostMediaFromShareAction() { ... }
    // REMOVED: private fun isMediaTypeIntent(intent: Intent, uri: Uri?): Boolean { ... }
    // REMOVED: All the complex media state management logic
    // REMOVED: EventBus subscribers for media events (now in MediaEventHandler)
    
    // ... rest of the class remains the same but ~450 lines shorter ...
}

/**
 * SUMMARY OF REFACTORING BENEFITS:
 * 
 * 1. **Reduced File Size**: From 4,251 lines to ~3,800 lines (450+ lines extracted)
 * 2. **Single Responsibility**: Media logic separated from activity lifecycle
 * 3. **Testability**: MediaManager can be unit tested independently  
 * 4. **Maintainability**: Media bugs can be fixed in one place
 * 5. **Reusability**: MediaManager can be used in other editor contexts
 * 6. **Cleaner EventBus**: All media events handled in dedicated component
 * 
 * NEXT STEPS:
 * 1. Extract PostPublishingManager (save, publish, validation logic)
 * 2. Extract EditorStateManager (editor switching, loading states)
 * 3. Extract NavigationManager (ViewPager, fragment management)
 * 4. Each extraction would remove 300-500 more lines
 * 
 * FINAL TARGET: ~1,500-2,000 lines (from original 4,251)
 */