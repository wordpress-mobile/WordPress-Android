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
import com.google.gson.Gson
import org.wordpress.android.R
import org.wordpress.android.editor.BuildConfig
import org.wordpress.android.editor.EditorEditMediaListener
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.editor.EditorImagePreviewListener
import org.wordpress.android.editor.LiveTextWatcher
import org.wordpress.android.editor.gutenberg.GutenbergWebViewAuthorizationData
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase.Companion.getDatabase
import org.wordpress.android.ui.posts.EditorConfigurationBuilder
import org.wordpress.android.ui.posts.GutenbergKitSettingsBuilder
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ProfilingUtils
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.gutenberg.EditorConfiguration
import org.wordpress.gutenberg.GutenbergView
import org.wordpress.gutenberg.GutenbergView.ContentChangeListener
import org.wordpress.gutenberg.GutenbergView.FeaturedImageChangeListener
import org.wordpress.gutenberg.GutenbergView.HistoryChangeListener
import org.wordpress.gutenberg.GutenbergView.LogJsExceptionListener
import org.wordpress.gutenberg.GutenbergView.OpenMediaLibraryListener
import org.wordpress.gutenberg.GutenbergView.TitleAndContentCallback
import org.wordpress.gutenberg.Media
import java.io.Serializable
import java.util.concurrent.CountDownLatch

class GutenbergKitEditorFragment : GutenbergKitEditorFragmentBase() {
    private var gutenbergView: GutenbergView? = null
    private var isHtmlModeEnabled = false

    private val textWatcher = LiveTextWatcher()
    private var historyChangeListener: HistoryChangeListener? = null
    private var featuredImageChangeListener: FeaturedImageChangeListener? = null
    private var openMediaLibraryListener: OpenMediaLibraryListener? = null
    private var onLogJsExceptionListener: LogJsExceptionListener? = null

    private var editorStarted = false
    private var isEditorDidMount = false
    private var rootView: View? = null
    private var isXPostsEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProfilingUtils.start("Visual Editor Startup")
        ProfilingUtils.split("EditorFragment.onCreate")

        if (savedInstanceState != null) {
            isHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED)
            editorStarted = savedInstanceState.getBoolean(KEY_EDITOR_STARTED)
            isEditorDidMount = savedInstanceState.getBoolean(KEY_EDITOR_DID_MOUNT)
            mFeaturedImageId = savedInstanceState.getLong(ARG_FEATURED_IMAGE_ID)
        }
    }

    private fun initializeFragmentListeners() {
        // Set up history change listener
        historyChangeListener = object : HistoryChangeListener {
            override fun onHistoryChanged(hasUndo: Boolean, hasRedo: Boolean) {
                mEditorFragmentListener.onToggleUndo(!hasUndo)
                mEditorFragmentListener.onToggleRedo(!hasRedo)
            }
        }

        // Set up featured image change listener
        featuredImageChangeListener = object : FeaturedImageChangeListener {
            override fun onFeaturedImageChanged(mediaID: Long) {
                mEditorFragmentListener.onFeaturedImageIdChanged(mediaID, true)
            }
        }

        // Set up media library listener
        openMediaLibraryListener = object : OpenMediaLibraryListener {
            override fun onOpenMediaLibrary(config: GutenbergView.OpenMediaLibraryConfig) {
                mEditorFragmentListener.onOpenMediaLibraryRequested(config)
            }
        }

        // Set up JS exception listener
        onLogJsExceptionListener = object : LogJsExceptionListener {
            override fun onLogJsException(exception: org.wordpress.gutenberg.GutenbergJsException) {
                val stackTraceElements = exception.stackTrace.map { stackTrace ->
                    com.automattic.android.tracks.crashlogging.JsExceptionStackTraceElement(
                        stackTrace.fileName,
                        stackTrace.lineNumber,
                        stackTrace.colNumber,
                        stackTrace.function
                    )
                }

                val jsException = com.automattic.android.tracks.crashlogging.JsException(
                    exception.type,
                    exception.message,
                    stackTraceElements,
                    exception.context,
                    exception.tags,
                    exception.isHandled,
                    exception.handledBy
                )

                val callback = object : com.automattic.android.tracks.crashlogging.JsExceptionCallback {
                    override fun onReportSent(sent: Boolean) {
                        // Do nothing
                    }
                }

                mEditorFragmentListener.onLogJsException(jsException, callback)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        if (arguments != null) {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            settings = requireArguments().getSerializable(ARG_GUTENBERG_KIT_SETTINGS) as Map<String, Any?>?
        }

        // Set up fragment's own listeners before initializing the editor
        initializeFragmentListeners()

        mEditorFragmentListener.onEditorFragmentInitialized()

        rootView = inflater.inflate(R.layout.fragment_gutenberg_kit_editor, container, false)
        val gutenbergViewContainer = rootView!!.findViewById<ViewGroup>(R.id.gutenberg_view_container)

        gutenbergView = GutenbergView.createForEditor(requireContext()).also { gutenbergView ->
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

            // Set up autocomplete listener for user mentions and cross-post suggestions
            gutenbergView.setAutocompleterTriggeredListener(object : GutenbergView.AutocompleterTriggeredListener {
                override fun onAutocompleterTriggered(type: String) {
                    when (type) {
                        "at-symbol" -> mEditorFragmentListener.showUserSuggestions { result ->
                            result?.let {
                                // Appended space completes the autocomplete session
                                gutenbergView.appendTextAtCursor("$it ")
                            }
                        }
                        "plus-symbol" -> {
                            if (isXPostsEnabled) {
                                mEditorFragmentListener.showXpostSuggestions { result ->
                                    result?.let {
                                        // Appended space completes the autocomplete session
                                        gutenbergView.appendTextAtCursor("$it ")
                                    }
                                }
                            }
                        }
                    }
                }
            })

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
        outState.putBoolean(KEY_EDITOR_STARTED, editorStarted)
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

    fun onToggleHtmlMode() {
        if (!isAdded) {
            return
        }

        toggleHtmlMode()
    }

    private fun toggleHtmlMode() {
        isHtmlModeEnabled = !isHtmlModeEnabled
        mEditorFragmentListener.onTrackableEvent(EditorFragmentAbstract.TrackableEvent.HTML_BUTTON_TAPPED)
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

    override fun getTitleOrContentChanged(): LiveData<Editable> {
        return textWatcher.afterTextChanged
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

    override fun onDestroy() {
        gutenbergView?.let { gutenbergView ->
            gutenbergView.destroy()
            historyChangeListener = null
            featuredImageChangeListener = null
        }
        editorStarted = false
        isEditorDidMount = false
        super.onDestroy()
    }

    fun startWithEditorSettings(editorSettings: String) {
        if (gutenbergView == null || editorStarted) {
            return
        }

        val config = buildEditorConfiguration(editorSettings)
        editorStarted = true
        gutenbergView?.start(config)
    }

    fun setXPostsEnabled(enabled: Boolean) {
        isXPostsEnabled = enabled
    }

    private fun buildEditorConfiguration(editorSettings: String): EditorConfiguration {
        val settingsMap = settings!!
        return EditorConfigurationBuilder.build(settingsMap, editorSettings)
    }

    override fun onUndoPressed() {
        gutenbergView?.undo()
    }

    override fun onRedoPressed() {
        gutenbergView?.redo()
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

        private var settings: Map<String, Any?>? = null

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
            GutenbergKitEditorFragment.settings = settings
            db?.addParcel(ARG_GUTENBERG_WEB_VIEW_AUTH_DATA, webViewAuthorizationData)
            return fragment
        }

        /**
         * Simplified factory method that uses GutenbergKitSettingsBuilder for configuration.
         * This reduces the activity's responsibility for detailed fragment setup.
         */
        fun newInstanceWithBuilder(
            context: Context,
            isNewPost: Boolean,
            jetpackFeaturesEnabled: Boolean,
            config: GutenbergKitSettingsBuilder.GutenbergKitConfig
        ): GutenbergKitEditorFragment {
            val authorizationData = GutenbergKitSettingsBuilder.buildAuthorizationData(
                siteConfig = config.siteConfig,
                appConfig = config.appConfig
            )

            val settings = GutenbergKitSettingsBuilder.buildSettings(
                siteConfig = config.siteConfig,
                postConfig = config.postConfig,
                appConfig = config.appConfig,
                featureConfig = config.featureConfig
            )

            return newInstance(
                context,
                isNewPost,
                authorizationData,
                jetpackFeaturesEnabled,
                settings
            )
        }
    }
}
