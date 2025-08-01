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
import org.wordpress.gutenberg.WebViewGlobal
import java.io.Serializable
import java.util.concurrent.CountDownLatch

class GutenbergKitEditorFragment : EditorFragmentAbstract(), EditorMediaUploadListener, IHistoryListener,
    EditorThemeUpdateListener, GutenbergDialogPositiveClickInterface, GutenbergDialogNegativeClickInterface,
    GutenbergNetworkConnectionListener {
    private var mGutenbergView: GutenbergView? = null
    private var mHtmlModeEnabled = false

    private val mTextWatcher = LiveTextWatcher()
    private var mHistoryChangeListener: HistoryChangeListener? = null
    private var mFeaturedImageChangeListener: FeaturedImageChangeListener? = null
    private var mOpenMediaLibraryListener: OpenMediaLibraryListener? = null
    private var mOnLogJsExceptionListener: LogJsExceptionListener? = null

    private var mEditorStarted = false
    private var mEditorDidMount = false
    private var mRootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProfilingUtils.start("Visual Editor Startup")
        ProfilingUtils.split("EditorFragment.onCreate")

        if (savedInstanceState != null) {
            mHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED)
            mEditorStarted = savedInstanceState.getBoolean(KEY_EDITOR_STARTED)
            mEditorDidMount = savedInstanceState.getBoolean(KEY_EDITOR_DID_MOUNT)
            mFeaturedImageId = savedInstanceState.getLong(ARG_FEATURED_IMAGE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        if (arguments != null) {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            mSettings = requireArguments().getSerializable(ARG_GUTENBERG_KIT_SETTINGS) as Map<String, Any?>?
        }

        // request dependency injection. Do this after setting min/max dimensions
        if (activity is EditorFragmentActivity) {
            (activity as EditorFragmentActivity).initializeEditorFragment()
        }

        mEditorFragmentListener.onEditorFragmentInitialized()

        mRootView = inflater.inflate(R.layout.fragment_gutenberg_kit_editor, container, false)
        val gutenbergViewContainer = mRootView!!.findViewById<ViewGroup>(R.id.gutenberg_view_container)

        mGutenbergView = getPreloadedWebView(requireContext()).also { view ->
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            gutenbergViewContainer.addView(view)
            
            setEditorProgressBarVisibility(true)
            
            view.setOnFileChooserRequestedListener { intent: Intent?, requestCode: Int? ->
                @Suppress("DEPRECATION") startActivityForResult(intent!!, requestCode!!)
                null
            }
            view.setContentChangeListener(object : ContentChangeListener {
                override fun onContentChanged() {
                    mTextWatcher.postTextChanged()
                }
            })
            if (mHistoryChangeListener != null) {
                view.setHistoryChangeListener(mHistoryChangeListener!!)
            }
            if (mFeaturedImageChangeListener != null) {
                view.setFeaturedImageChangeListener(mFeaturedImageChangeListener!!)
            }
            if (mOpenMediaLibraryListener != null) {
                view.setOpenMediaLibraryListener(mOpenMediaLibraryListener!!)
            }
            if (mOnLogJsExceptionListener != null) {
                view.setLogJsExceptionListener(mOnLogJsExceptionListener!!)
            }
            view.setEditorDidBecomeAvailable { _: GutenbergView? ->
                mEditorDidMount = true
                mEditorFragmentListener.onEditorFragmentContentReady(ArrayList<Any?>(), false)
                setEditorProgressBarVisibility(false)
            }
        }

        return mRootView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        mGutenbergView?.invalidate()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)

        mGutenbergView?.let { gutenbergView ->
            if (requestCode == gutenbergView.pickImageRequestCode) {
                val filePathCallback = gutenbergView.filePathCallback

                if (filePathCallback != null) {
                    if (resultCode == Activity.RESULT_OK && data != null) {
                        if (data.clipData != null) {
                            val clipData = data.clipData
                            val uris = arrayOfNulls<Uri>(clipData!!.itemCount)
                            for (i in 0..<clipData.itemCount) {
                                uris[i] = clipData.getItemAt(i).uri
                            }
                            filePathCallback.onReceiveValue(uris)
                        } else if (data.data != null) {
                            val uri = data.data
                            filePathCallback.onReceiveValue(arrayOf(uri))
                        } else {
                            filePathCallback.onReceiveValue(null)
                        }
                    } else {
                        filePathCallback.onReceiveValue(null)
                    }
                    gutenbergView.resetFilePathCallback()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setEditorProgressBarVisibility(!mEditorDidMount)
    }

    private fun setEditorProgressBarVisibility(shown: Boolean) {
        if (isAdded && mRootView != null) {
            mRootView!!.findViewById<View?>(R.id.editor_progress)?.visibility = if (shown) View.VISIBLE else View.GONE
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

        try {
            mEditorDragAndDropListener = activity as EditorDragAndDropListener?
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement EditorDragAndDropListener: $e")
        }

        try {
            mEditorImagePreviewListener = activity as EditorImagePreviewListener?
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement EditorImagePreviewListener: $e")
        }

        try {
            mEditorEditMediaListener = activity as EditorEditMediaListener?
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement EditorEditMediaListener: $e")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_HTML_MODE_ENABLED, mHtmlModeEnabled)
        outState.putBoolean(KEY_EDITOR_STARTED, mEditorStarted)
        outState.putBoolean(KEY_EDITOR_DID_MOUNT, mEditorDidMount)
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

        mGutenbergView?.setContent(text as String)
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
        mHtmlModeEnabled = !mHtmlModeEnabled
        mEditorFragmentListener.onTrackableEvent(TrackableEvent.HTML_BUTTON_TAPPED)
        mEditorFragmentListener.onHtmlModeToggledInToolbar()
        mGutenbergView?.textEditorEnabled = mHtmlModeEnabled
    }

    @Throws(EditorFragmentNotAddedException::class)
    override fun getTitleAndContent(originalContent: CharSequence): Pair<CharSequence, CharSequence> {
        return getTitleAndContent(originalContent, false)
    }

    @Throws(EditorFragmentNotAddedException::class)
    fun getTitleAndContent(
        originalContent: CharSequence, completeComposition: Boolean
    ): Pair<CharSequence, CharSequence> {
        val gutenbergView = mGutenbergView ?: return Pair("", "")
        
        val result: Array<Pair<CharSequence, CharSequence>?> = arrayOfNulls(1)
        val latch = CountDownLatch(1)

        gutenbergView.getTitleAndContent(originalContent, object : TitleAndContentCallback {
            override fun onResult(title: CharSequence, content: CharSequence) {
                result[0] = Pair(title, content)
                latch.countDown()
            }
        }, completeComposition)

        try {
            latch.await()
        } catch (e: InterruptedException) {
            AppLog.w(
                AppLog.T.EDITOR, "Thread interrupted while waiting for title and content from Gutenberg editor: $e"
            )
            Thread.currentThread().interrupt()
            return Pair("", "")
        }

        return result[0] ?: Pair("", "")
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
        mHistoryChangeListener = listener
    }

    override fun onFeaturedImageChanged(listener: FeaturedImageChangeListener) {
        mFeaturedImageChangeListener = listener
    }

    override fun onOpenMediaLibrary(listener: OpenMediaLibraryListener) {
        mOpenMediaLibraryListener = listener
    }

    override fun onLogJsException(listener: LogJsExceptionListener) {
        mOnLogJsExceptionListener = listener
    }

    override fun getTitleOrContentChanged(): LiveData<Editable> {
        return mTextWatcher.afterTextChanged
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
        if (mGutenbergView == null || !isNetworkUrl) {
            return
        }

        val processedMediaList = ArrayList<Media?>()

        for (mediaEntry in mediaList.entries) {
            val mediaId = mediaEntry.value!!.mediaId.toInt()
            val url: String = mediaEntry.key!!
            val mediaFile: MediaFile = mediaEntry.value!!
            val metadata = Bundle()
            val videoPressGuid = mediaFile.videoPressGuid
            if (videoPressGuid != null) {
                metadata.putString("videopressGUID", videoPressGuid)
            }
            processedMediaList.add(
                Media.createMediaUsingMimeType(
                    mediaId, url, mediaFile.mimeType, mediaFile.caption, mediaFile.title, mediaFile.alt
                )
            )
        }

        val mediaString = Gson().toJson(processedMediaList)
        mGutenbergView?.setMediaUploadAttachment(mediaString)
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
        mGutenbergView?.let { gutenbergView ->
            recycleWebView(gutenbergView)
            mHistoryChangeListener = null
            mFeaturedImageChangeListener = null
        }
        mEditorStarted = false
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
        if (mGutenbergView == null || mEditorStarted) {
            return
        }

        var postId = mSettings!!["postId"] as Int?
        if (postId != null && postId == 0) {
            postId = -1
        }

        val siteURL = mSettings!!["siteURL"] as String?
        val siteApiRoot = mSettings!!["siteApiRoot"] as String?

        @Suppress("UNCHECKED_CAST") val siteApiNamespace = mSettings!!["siteApiNamespace"] as Array<String?>?
        val firstNamespace = if (siteApiNamespace != null && siteApiNamespace.isNotEmpty()) siteApiNamespace[0] else ""
        val editorAssetsEndpoint = siteApiRoot + "wpcom/v2/" + firstNamespace + "editor-assets"

        @Suppress("UNCHECKED_CAST") var cookies = mSettings!!["cookies"] as Map<String, String>?
        if (cookies == null) {
            cookies = HashMap()
        }

        @Suppress("UNCHECKED_CAST") val namespaceExcludedPaths =
            (mSettings!!["namespaceExcludedPaths"] as Array<String>?) ?: emptyArray()

        @Suppress("UNCHECKED_CAST") val webViewGlobals =
            (mSettings!!["webViewGlobals"] as List<WebViewGlobal>?) ?: emptyList()

        val config = EditorConfiguration.Builder().setTitle(mSettings!!["postTitle"] as String)
            .setContent(mSettings!!["postContent"] as String).setPostId(postId)
            .setPostType(mSettings!!["postType"] as String?).setThemeStyles(mSettings!!["themeStyles"] as Boolean)
            .setPlugins(mSettings!!["plugins"] as Boolean).setSiteApiRoot(mSettings!!["siteApiRoot"] as String)
            .setSiteApiNamespace(siteApiNamespace?.filterNotNull()?.toTypedArray() ?: emptyArray())
            .setNamespaceExcludedPaths(namespaceExcludedPaths).setAuthHeader(mSettings!!["authHeader"] as String)
            .setWebViewGlobals(webViewGlobals).setEditorSettings(editorSettings)
            .setLocale(mSettings!!["locale"] as String?).setEditorAssetsEndpoint(editorAssetsEndpoint)
            .setCachedAssetHosts(setOf("s0.wp.com", UrlUtils.getHost(siteURL))).setEnableAssetCaching(true)
            .setCookies(cookies).build()

        mEditorStarted = true
        mGutenbergView?.start(config)
    }

    override fun showNotice(message: String?) {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun showEditorHelp() {
        // Unused, no-op retained for the shared interface with Gutenberg
    }

    override fun onUndoPressed() {
        mGutenbergView?.undo()
    }

    override fun onRedoPressed() {
        mGutenbergView?.redo()
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
        const val ARG_GUTENBERG_KIT_SETTINGS: String = "gutenberg_kit_settings"

        private const val CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101
        private const val CAPTURE_VIDEO_PERMISSION_REQUEST_CODE = 102

        private var mSettings: Map<String, Any?>? = null

        fun newInstance(
            context: Context,
            isNewPost: Boolean,
            webViewAuthorizationData: GutenbergWebViewAuthorizationData?,
            jetpackFeaturesEnabled: Boolean,
            settings: Map<String, Any?>?
        ): GutenbergKitEditorFragment {
            val fragment = GutenbergKitEditorFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_NEW_POST, isNewPost)
            args.putBoolean(ARG_JETPACK_FEATURES_ENABLED, jetpackFeaturesEnabled)
            args.putSerializable(ARG_GUTENBERG_KIT_SETTINGS, settings as Serializable?)
            fragment.setArguments(args)
            val db = getDatabase(context)
            mSettings = settings
            db?.addParcel(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA, webViewAuthorizationData)
            return fragment
        }
    }
}
