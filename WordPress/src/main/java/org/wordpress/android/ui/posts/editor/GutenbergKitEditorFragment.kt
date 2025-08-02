package org.wordpress.android.ui.posts.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import com.android.volley.toolbox.ImageLoader
import com.google.gson.Gson
import org.wordpress.android.R
import org.wordpress.android.editor.BuildConfig
import org.wordpress.android.editor.EditorEditMediaListener
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.editor.EditorFragmentActivity
import org.wordpress.android.editor.EditorImagePreviewListener
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.editor.EditorThemeUpdateListener
import org.wordpress.android.editor.LiveTextWatcher
import org.wordpress.android.editor.gutenberg.GutenbergDialogFragment.GutenbergDialogNegativeClickInterface
import org.wordpress.android.editor.gutenberg.GutenbergDialogFragment.GutenbergDialogPositiveClickInterface
import org.wordpress.android.editor.gutenberg.GutenbergNetworkConnectionListener
import org.wordpress.android.editor.gutenberg.GutenbergWebViewAuthorizationData
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase.Companion.getDatabase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ProfilingUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.util.helpers.MediaGallery
import org.wordpress.aztec.IHistoryListener
import org.wordpress.gutenberg.EditorConfiguration
import org.wordpress.gutenberg.GutenbergView
import org.wordpress.gutenberg.GutenbergView.ContentChangeListener
import org.wordpress.gutenberg.GutenbergView.FeaturedImageChangeListener
import org.wordpress.gutenberg.GutenbergView.HistoryChangeListener
import org.wordpress.gutenberg.GutenbergView.LogJsExceptionListener
import org.wordpress.gutenberg.GutenbergView.OpenMediaLibraryListener
import org.wordpress.gutenberg.GutenbergView.TitleAndContentCallback
import org.wordpress.gutenberg.GutenbergWebViewPool.getPreloadedWebView
import org.wordpress.gutenberg.GutenbergWebViewPool.recycleWebView
import org.wordpress.gutenberg.Media
import java.util.concurrent.CountDownLatch
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.ui.posts.GutenbergKitSettings
import org.wordpress.android.ui.posts.GutenbergKitViewModel
import org.wordpress.android.WordPress
import javax.inject.Inject

class GutenbergKitEditorFragment : EditorFragmentAbstract(), EditorMediaUploadListener, IHistoryListener,
    EditorThemeUpdateListener, GutenbergDialogPositiveClickInterface, GutenbergDialogNegativeClickInterface,
    GutenbergNetworkConnectionListener {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var gutenbergKitViewModel: GutenbergKitViewModel

    private var gutenbergView: GutenbergView? = null
    private var isHtmlModeEnabled = false

    private val textWatcher = LiveTextWatcher()
    private var historyChangeListener: HistoryChangeListener? = null
    private var featuredImageChangeListener: FeaturedImageChangeListener? = null
    private var openMediaLibraryListener: OpenMediaLibraryListener? = null
    private var onLogJsExceptionListener: LogJsExceptionListener? = null

    private var isEditorStarted = false
    private var isEditorDidMount = false
    private var rootView: View? = null

    // Access settings through ViewModel
    private val settings: GutenbergKitSettings?
        get() = if (::gutenbergKitViewModel.isInitialized) {
            gutenbergKitViewModel.editorSettings.value
        } else {
            null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProfilingUtils.start("Visual Editor Startup")
        ProfilingUtils.split("EditorFragment.onCreate")

        // Trigger dependency injection
        (requireActivity().applicationContext as WordPress).component().inject(this)

        // Initialize shared ViewModel (same scope as Activity) - after DI is complete
        gutenbergKitViewModel = ViewModelProvider(
            requireActivity(),
            viewModelFactory
        )[GutenbergKitViewModel::class.java]

        if (savedInstanceState != null) {
            isHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED)
            isEditorStarted = savedInstanceState.getBoolean(KEY_EDITOR_STARTED)
            isEditorDidMount = savedInstanceState.getBoolean(KEY_EDITOR_DID_MOUNT)
            mFeaturedImageId = savedInstanceState.getLong(ARG_FEATURED_IMAGE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // request dependency injection. Do this after setting min/max dimensions
        if (activity is EditorFragmentActivity) {
            (activity as EditorFragmentActivity).initializeEditorFragment()
        }

        mEditorFragmentListener.onEditorFragmentInitialized()

        rootView = inflater.inflate(R.layout.fragment_gutenberg_kit_editor, container, false)
        val gutenbergViewContainer = rootView!!.findViewById<ViewGroup>(R.id.gutenberg_view_container)

        gutenbergView = getPreloadedWebView(requireContext()).also { gutenbergView ->
            gutenbergView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            gutenbergViewContainer.addView(gutenbergView)

            gutenbergView.setOnFileChooserRequestedListener { intent: Intent?, requestCode: Int? ->
                @Suppress("DEPRECATION") startActivityForResult(intent!!, requestCode!!)
                null
            }
            gutenbergView.setContentChangeListener(object : ContentChangeListener {
                override fun onContentChanged() {
                    textWatcher.postTextChanged()
                }
            })
            historyChangeListener?.let(gutenbergView::setHistoryChangeListener)
            featuredImageChangeListener?.let(gutenbergView::setFeaturedImageChangeListener)
            openMediaLibraryListener?.let(gutenbergView::setOpenMediaLibraryListener)
            onLogJsExceptionListener?.let(gutenbergView::setLogJsExceptionListener)
            gutenbergView.setEditorDidBecomeAvailable {
                isEditorDidMount = true
                mEditorFragmentListener.onEditorFragmentContentReady(ArrayList<Any?>(), false)
                setEditorProgressBarVisibility(false)
            }
        }

        setEditorProgressBarVisibility(true)

        return rootView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        gutenbergView?.invalidate()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)

        gutenbergView?.let { gutenbergView ->
            if (requestCode == gutenbergView.pickImageRequestCode) {
                handleFileChooserResult(gutenbergView, resultCode, data)
            }
        }
    }

    private fun handleFileChooserResult(gutenbergView: GutenbergView, resultCode: Int, data: Intent?) {
        val filePathCallback = gutenbergView.filePathCallback ?: return

        val uris = extractUrisFromIntent(resultCode, data)
        filePathCallback.onReceiveValue(uris)
        gutenbergView.resetFilePathCallback()
    }

    private fun extractUrisFromIntent(resultCode: Int, data: Intent?): Array<Uri?>? {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null
        }

        return when {
            data.clipData != null -> {
                val clipData = data.clipData!!
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            }

            data.data != null -> arrayOf(data.data)
            else -> null
        }
    }

    override fun onResume() {
        super.onResume()
        setEditorProgressBarVisibility(!isEditorDidMount)
    }

    private fun setEditorProgressBarVisibility(shown: Boolean) {
        if (isAdded) {
            rootView?.findViewById<View?>(R.id.editor_progress).setVisibleOrGone(shown)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        if (PermissionUtils.checkCameraAndStoragePermissions(this.activity)) {
            if (requestCode == CAPTURE_PHOTO_PERMISSION_REQUEST_CODE) {
                mEditorFragmentListener.onCapturePhotoClicked()
            } else if (requestCode == CAPTURE_VIDEO_PERMISSION_REQUEST_CODE) {
                mEditorFragmentListener.onCaptureVideoClicked()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = context as Activity

        mEditorDragAndDropListener = requireActivityImplements<EditorDragAndDropListener>(activity)
        mEditorImagePreviewListener = requireActivityImplements<EditorImagePreviewListener>(activity)
        mEditorEditMediaListener = requireActivityImplements<EditorEditMediaListener>(activity)
    }

    private inline fun <reified T> requireActivityImplements(activity: Activity): T? {
        return try {
            activity as T?
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement ${T::class.simpleName}: $e")
        }
    }

    // View extension functions
    private fun View?.setVisibleOrGone(visible: Boolean) {
        this?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_HTML_MODE_ENABLED, isHtmlModeEnabled)
        outState.putBoolean(KEY_EDITOR_STARTED, isEditorStarted)
        outState.putBoolean(KEY_EDITOR_DID_MOUNT, isEditorDidMount)
        outState.putLong(ARG_FEATURED_IMAGE_ID, mFeaturedImageId)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(org.wordpress.android.editor.R.menu.menu_gutenberg, menu)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onPrepareOptionsMenu(menu: Menu) {
        val debugMenuItem = menu.findItem(org.wordpress.android.editor.R.id.debugmenu)
        debugMenuItem.isVisible = BuildConfig.DEBUG

        @Suppress("DEPRECATION") super.onPrepareOptionsMenu(menu)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return false
    }

    override fun onRedoEnabled() {
        // Currently unsupported
    }

    override fun onUndoEnabled() {
        // Currently unsupported
    }

    override fun onUndo() {
        // Analytics tracking is not available in GB mobile
    }

    override fun onRedo() {
        // Analytics tracking is not available in GB mobile
    }

    override fun setTitle(title: CharSequence?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun setContent(text: CharSequence?) {
        var text = text
        if (text == null) {
            text = ""
        }

        gutenbergView?.setContent(text as String)
    }

    override fun updateContent(text: CharSequence?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    fun onToggleHtmlMode() {
        if (!isAdded) {
            return
        }

        toggleHtmlMode()
    }

    private fun toggleHtmlMode() {
        isHtmlModeEnabled = !isHtmlModeEnabled
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED)
        mEditorFragmentListener.onHtmlModeToggledInToolbar()
        gutenbergView?.textEditorEnabled = isHtmlModeEnabled
    }

    @Throws(EditorFragmentNotAddedException::class)
    override fun getTitleAndContent(originalContent: CharSequence): Pair<CharSequence, CharSequence> {
        return getTitleAndContent(originalContent, false)
    }

    @Throws(EditorFragmentNotAddedException::class)
    fun getTitleAndContent(
        originalContent: CharSequence, completeComposition: Boolean
    ): Pair<CharSequence, CharSequence> {
        val gutenbergView = gutenbergView ?: return Pair("", "")

        val result: Array<Pair<CharSequence, CharSequence>?> = arrayOfNulls(1)
        val latch = CountDownLatch(1)

        gutenbergView.getTitleAndContent(originalContent, object : TitleAndContentCallback {
            override fun onResult(title: CharSequence, content: CharSequence) {
                result[0] = Pair(title, content)
                latch.countDown()
            }
        }, completeComposition)

        val finalResult = try {
            latch.await()
            result[0]
        } catch (e: InterruptedException) {
            AppLog.w(
                AppLog.T.EDITOR, "Thread interrupted while waiting for title and content from Gutenberg editor: $e"
            )
            Thread.currentThread().interrupt()
            null
        }

        return finalResult ?: Pair("", "")
    }

    override fun getEditorName(): String {
        return GUTENBERG_EDITOR_NAME
    }

    override fun isActionInProgress(): Boolean {
        return false
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    @Throws(EditorFragmentNotAddedException::class)
    override fun getContent(originalContent: CharSequence?): CharSequence {
        if (!isAdded) {
            throw EditorFragmentNotAddedException()
        }

        return ""
    }

    @Throws(EditorFragmentNotAddedException::class)
    override fun showContentInfo() {
        if (!isAdded) {
            throw EditorFragmentNotAddedException()
        }
    }

    override fun onEditorHistoryChanged(listener: HistoryChangeListener) {
        historyChangeListener = listener
    }

    override fun onFeaturedImageChanged(listener: FeaturedImageChangeListener) {
        featuredImageChangeListener = listener
    }

    override fun onOpenMediaLibrary(listener: OpenMediaLibraryListener) {
        openMediaLibraryListener = listener
    }

    override fun onLogJsException(listener: LogJsExceptionListener) {
        onLogJsExceptionListener = listener
    }

    override fun getTitleOrContentChanged(): LiveData<Editable> {
        return textWatcher.afterTextChanged
    }

    override fun appendMediaFile(
        mediaFile: MediaFile?, mediaUrl: String?, imageLoader: ImageLoader?
    ) {
        // noop implementation for shared interface with Aztec
    }

    override fun appendMediaFiles(mediaList: MutableMap<String?, MediaFile?>) {
        if (activity == null) {
            // appendMediaFile may be called from a background thread (example: EditPostActivity.java#L2165) and
            // Activity may have already be gone.
            // Ticket: https://github.com/wordpress-mobile/WordPress-Android/issues/7386
            AppLog.d(AppLog.T.MEDIA, "appendMediaFiles() called but Activity is null!")
            return
        }

        // Get media URL of first of media first to check if it is network or local one.
        var mediaUrl: String? = ""
        val mediaUrls: Array<Any?> = mediaList.keys.toTypedArray()
        if (mediaUrls.isNotEmpty()) {
            mediaUrl = mediaUrls[0] as String?
        }

        val isNetworkUrl = URLUtil.isNetworkUrl(mediaUrl)

        // Disable upload handling until supported--e.g., media shared to the app
        if (gutenbergView == null || !isNetworkUrl) {
            return
        }

        val processedMediaList = mediaList.entries.map { (url, mediaFile) ->
            val mediaId = mediaFile!!.mediaId.toInt()
            Media.createMediaUsingMimeType(
                mediaId, url!!, mediaFile.mimeType, mediaFile.caption, mediaFile.title, mediaFile.alt
            )
        }

        val mediaString = Gson().toJson(processedMediaList)
        gutenbergView?.setMediaUploadAttachment(mediaString)
    }

    override fun appendGallery(mediaGallery: MediaGallery?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun setUrlForVideoPressId(videoId: String?, videoUrl: String?, posterUrl: String?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun isUploadingMedia(): Boolean {
        // Unused, no-op retained for the shared interface with Gutenberg
        return false
    }

    override fun hasFailedMediaUploads(): Boolean {
        // Unused, no-op retained for the shared interface with Gutenberg
        return false
    }

    override fun removeAllFailedMediaUploads() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun removeMedia(mediaId: String?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onDestroy() {
        gutenbergView?.let { gutenbergView ->
            recycleWebView(gutenbergView)
            historyChangeListener = null
            featuredImageChangeListener = null
        }
        isEditorStarted = false
        super.onDestroy()
    }

    override fun mediaSelectionCancelled() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onMediaUploadReattached(localMediaId: String?, currentProgress: Float) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onMediaUploadRetry(localMediaId: String?, mediaType: MediaType?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onMediaUploadSucceeded(localMediaId: String?, mediaFile: MediaFile?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onMediaUploadProgress(localMediaId: String?, progress: Float) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onMediaUploadFailed(localMediaId: String?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onMediaUploadPaused(localMediaId: String?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onGalleryMediaUploadSucceeded(
        galleryId: Long, remoteMediaId: Long, remaining: Int
    ) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onEditorThemeUpdated(editorTheme: Bundle?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    fun startWithEditorSettings(editorSettings: String) {
        if (gutenbergView == null || isEditorStarted) {
            return
        }

        val config = buildEditorConfiguration(editorSettings)
        isEditorStarted = true
        gutenbergView?.start(config)
    }

    private fun buildEditorConfiguration(editorSettings: String): EditorConfiguration {
        val kitSettings = settings!!

        val postId = kitSettings.postId?.let { if (it == 0) -1 else it }
        val firstNamespace = kitSettings.siteApiNamespace.firstOrNull() ?: ""
        val editorAssetsEndpoint = "${kitSettings.siteApiRoot}wpcom/v2/${firstNamespace}editor-assets"

        return EditorConfiguration.Builder()
            .setTitle(kitSettings.postTitle ?: "")
            .setContent(kitSettings.postContent ?: "")
            .setPostId(postId)
            .setPostType(kitSettings.postType)
            .setThemeStyles(kitSettings.themeStyles)
            .setPlugins(kitSettings.plugins)
            .setSiteApiRoot(kitSettings.siteApiRoot)
            .setSiteApiNamespace(kitSettings.siteApiNamespace.toTypedArray())
            .setNamespaceExcludedPaths(kitSettings.namespaceExcludedPaths.toTypedArray())
            .setAuthHeader(kitSettings.authHeader)
            .setWebViewGlobals(kitSettings.webViewGlobals)
            .setEditorSettings(editorSettings)
            .setLocale(kitSettings.locale)
            .setEditorAssetsEndpoint(editorAssetsEndpoint)
            .setCachedAssetHosts(setOf("s0.wp.com", UrlUtils.getHost(kitSettings.siteURL)))
            .setEnableAssetCaching(true)
            .setCookies(kitSettings.cookies)
            .build()
    }

    override fun showNotice(message: String?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun showEditorHelp() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onUndoPressed() {
        gutenbergView?.undo()
    }

    override fun onRedoPressed() {
        gutenbergView?.redo()
    }

    override fun onGutenbergDialogPositiveClicked(instanceTag: String, id: Int) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onGutenbergDialogNegativeClicked(instanceTag: String) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onConnectionStatusChange(isConnected: Boolean) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    companion object {
        private const val GUTENBERG_EDITOR_NAME = "gutenberg"
        private const val KEY_HTML_MODE_ENABLED = "KEY_HTML_MODE_ENABLED"
        private const val KEY_EDITOR_STARTED = "KEY_EDITOR_STARTED"
        private const val KEY_EDITOR_DID_MOUNT = "KEY_EDITOR_DID_MOUNT"
        private const val ARG_IS_NEW_POST = "param_is_new_post"
        private const val ARG_GUTENBERG_WEB_VIEW_AUTH_DATA = "param_gutenberg_web_view_auth_data"
        const val ARG_FEATURED_IMAGE_ID: String = "featured_image_id"
        const val ARG_JETPACK_FEATURES_ENABLED: String = "jetpack_features_enabled"

        private const val CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101
        private const val CAPTURE_VIDEO_PERMISSION_REQUEST_CODE = 102

        fun newInstance(
            context: Context,
            isNewPost: Boolean,
            webViewAuthorizationData: GutenbergWebViewAuthorizationData?,
            jetpackFeaturesEnabled: Boolean,
        ): GutenbergKitEditorFragment {
            val fragment = GutenbergKitEditorFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_NEW_POST, isNewPost)
            args.putBoolean(ARG_JETPACK_FEATURES_ENABLED, jetpackFeaturesEnabled)
            fragment.setArguments(args)
            val db = getDatabase(context)
            db?.addParcel(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA, webViewAuthorizationData)
            return fragment
        }
    }
}
