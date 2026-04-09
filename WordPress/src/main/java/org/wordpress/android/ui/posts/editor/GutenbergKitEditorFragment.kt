package org.wordpress.android.ui.posts.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.FrameLayout
import androidx.core.os.BundleCompat
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.editor.BuildConfig
import org.wordpress.android.editor.EditorEditMediaListener
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.editor.EditorImagePreviewListener
import org.wordpress.android.editor.LiveTextWatcher
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ProfilingUtils
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.gutenberg.GutenbergView
import org.wordpress.gutenberg.GutenbergView.ContentChangeListener
import org.wordpress.gutenberg.GutenbergView.FeaturedImageChangeListener
import org.wordpress.gutenberg.GutenbergView.HistoryChangeListener
import org.wordpress.gutenberg.GutenbergView.LogJsExceptionListener
import org.wordpress.gutenberg.GutenbergView.OpenMediaLibraryListener
import org.wordpress.gutenberg.GutenbergView.TitleAndContentCallback
import org.wordpress.gutenberg.Media
import org.wordpress.gutenberg.model.EditorConfiguration
import java.util.concurrent.CountDownLatch

class GutenbergKitEditorFragment : GutenbergKitEditorFragmentBase() {
    private var gutenbergView: GutenbergView? = null
    private var isHtmlModeEnabled = false

    private val textWatcher = LiveTextWatcher()
    private var historyChangeListener: HistoryChangeListener? = null
    private var featuredImageChangeListener: FeaturedImageChangeListener? = null
    private var openMediaLibraryListener: OpenMediaLibraryListener? = null
    private var onLogJsExceptionListener: LogJsExceptionListener? = null
    private var modalDialogStateListener: GutenbergView.ModalDialogStateListener? = null
    private var networkRequestListener: GutenbergView.NetworkRequestListener? = null
    private var rootView: View? = null
    private var isXPostsEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProfilingUtils.start("Visual Editor Startup")
        ProfilingUtils.split("EditorFragment.onCreate")

        if (savedInstanceState != null) {
            isHtmlModeEnabled = savedInstanceState.getBoolean(KEY_HTML_MODE_ENABLED)
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
            override fun onOpenMediaLibrary(
                config: GutenbergView.OpenMediaLibraryConfig
            ) {
                mEditorFragmentListener.onOpenMediaLibraryRequested(config)
            }
        }

        // Set up JS exception listener
        onLogJsExceptionListener = object : LogJsExceptionListener {
            override fun onLogJsException(
                exception: org.wordpress.gutenberg.GutenbergJsException
            ) {
                val stackTraceElements = exception.stackTrace.map { stackTrace ->
                    com.automattic.android.tracks.crashlogging
                        .JsExceptionStackTraceElement(
                            stackTrace.fileName,
                            stackTrace.lineNumber,
                            stackTrace.colNumber,
                            stackTrace.function
                        )
                }

                val jsException =
                    com.automattic.android.tracks.crashlogging.JsException(
                        exception.type,
                        exception.message,
                        stackTraceElements,
                        exception.context,
                        exception.tags,
                        exception.isHandled,
                        exception.handledBy
                    )

                val callback = object :
                    com.automattic.android.tracks.crashlogging.JsExceptionCallback {
                    override fun onReportSent(sent: Boolean) {
                        // Do nothing
                    }
                }

                mEditorFragmentListener.onLogJsException(jsException, callback)
            }
        }

        // Set up modal dialog state listener
        modalDialogStateListener = object : GutenbergView.ModalDialogStateListener {
            override fun onModalDialogOpened(dialogType: String) {
                mEditorFragmentListener.onModalDialogOpened(dialogType)
            }

            override fun onModalDialogClosed(dialogType: String) {
                mEditorFragmentListener.onModalDialogClosed(dialogType)
            }
        }
    }

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Set up fragment's own listeners before initializing the editor
        initializeFragmentListeners()

        mEditorFragmentListener.onEditorFragmentInitialized()

        rootView = inflater.inflate(
            R.layout.fragment_gutenberg_kit_editor, container, false
        )
        val gutenbergViewContainer =
            rootView!!.findViewById<ViewGroup>(R.id.gutenberg_view_container)

        val configuration = requireNotNull(
            BundleCompat.getParcelable(
                requireArguments(),
                ARG_GUTENBERG_KIT_SETTINGS,
                EditorConfiguration::class.java
            )
        )

        val gutenbergView = GutenbergView(
            configuration = configuration,
            dependencies = null,
            coroutineScope = this.lifecycleScope,
            context = requireContext()
        )

        gutenbergViewContainer.addView(
            gutenbergView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        gutenbergView.setOnFileChooserRequestedListener {
                intent: Intent?, requestCode: Int? ->
            @Suppress("DEPRECATION")
            startActivityForResult(intent!!, requestCode!!)
            null
        }
        gutenbergView.setContentChangeListener(object : ContentChangeListener {
            override fun onContentChanged() {
                textWatcher.postTextChanged()
            }
        })
        historyChangeListener?.let(gutenbergView::setHistoryChangeListener)
        featuredImageChangeListener?.let(
            gutenbergView::setFeaturedImageChangeListener
        )
        openMediaLibraryListener?.let(
            gutenbergView::setOpenMediaLibraryListener
        )
        onLogJsExceptionListener?.let(
            gutenbergView::setLogJsExceptionListener
        )
        modalDialogStateListener?.let(
            gutenbergView::setModalDialogStateListener
        )
        networkRequestListener?.let(
            gutenbergView::setNetworkRequestListener
        )

        // Set up content provider for WebView refresh recovery
        gutenbergView.setLatestContentProvider(
            object : GutenbergView.LatestContentProvider {
                override fun getLatestContent(): GutenbergView.LatestContent {
                    return GutenbergView.LatestContent(
                        mEditorFragmentListener.persistedTitle,
                        mEditorFragmentListener.persistedContent
                    )
                }
            }
        )

        // Set up autocomplete listener for user mentions and cross-post suggestions
        gutenbergView.setAutocompleterTriggeredListener(
            object : GutenbergView.AutocompleterTriggeredListener {
                override fun onAutocompleterTriggered(type: String) {
                    when (type) {
                        "at-symbol" ->
                            mEditorFragmentListener.showUserSuggestions { result ->
                                result?.let {
                                    // Appended space completes the autocomplete session
                                    gutenbergView.appendTextAtCursor("$it ")
                                }
                            }
                        "plus-symbol" -> {
                            if (isXPostsEnabled) {
                                mEditorFragmentListener
                                    .showXpostSuggestions { result ->
                                        result?.let {
                                            // Appended space completes the autocomplete session
                                            gutenbergView
                                                .appendTextAtCursor("$it ")
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        )

        gutenbergView.setEditorDidBecomeAvailable {
            mEditorFragmentListener.onEditorFragmentContentReady(
                ArrayList<Any?>(), false
            )
        }

        this.gutenbergView = gutenbergView

        return rootView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        gutenbergView?.invalidate()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        gutenbergView?.let { gutenbergView ->
            if (requestCode == gutenbergView.pickImageRequestCode) {
                handleFileChooserResult(gutenbergView, resultCode, data)
            }
        }
    }

    private fun handleFileChooserResult(
        gutenbergView: GutenbergView, resultCode: Int, data: Intent?
    ) {
        val filePathCallback = gutenbergView.filePathCallback ?: return

        if (resultCode != Activity.RESULT_OK) {
            filePathCallback.onReceiveValue(null)
            gutenbergView.resetFilePathCallback()
            return
        }

        lifecycleScope.launch {
            val uris = gutenbergView.extractUrisFromIntent(data)
            val processedUris =
                gutenbergView.processFileUris(requireContext(), uris)
            filePathCallback.onReceiveValue(processedUris)
            gutenbergView.resetFilePathCallback()
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

        mEditorImagePreviewListener =
            requireActivityImplements<EditorImagePreviewListener>(activity)
        mEditorEditMediaListener =
            requireActivityImplements<EditorEditMediaListener>(activity)
    }

    private inline fun <reified T> requireActivityImplements(
        activity: Activity
    ): T? {
        return try {
            activity as T?
        } catch (e: ClassCastException) {
            throw ClassCastException(
                "$activity must implement ${T::class.simpleName}: $e"
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_HTML_MODE_ENABLED, isHtmlModeEnabled)
        outState.putLong(ARG_FEATURED_IMAGE_ID, mFeaturedImageId)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(
            org.wordpress.android.editor.R.menu.menu_gutenberg, menu
        )
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onPrepareOptionsMenu(menu: Menu) {
        val debugMenuItem =
            menu.findItem(org.wordpress.android.editor.R.id.debugmenu)
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
        mEditorFragmentListener.onTrackableEvent(
            EditorFragmentAbstract.TrackableEvent.HTML_BUTTON_TAPPED
        )
        mEditorFragmentListener.onHtmlModeToggledInToolbar()
        gutenbergView?.textEditorEnabled = isHtmlModeEnabled
    }

    @Throws(EditorFragmentNotAddedException::class)
    override fun getTitleAndContent(
        originalContent: CharSequence
    ): Pair<CharSequence, CharSequence> {
        return getTitleAndContent(originalContent, false)
    }

    @Throws(EditorFragmentNotAddedException::class)
    fun getTitleAndContent(
        originalContent: CharSequence, completeComposition: Boolean
    ): Pair<CharSequence, CharSequence> {
        val gutenbergView = gutenbergView ?: return Pair("", "")

        val result: Array<Pair<CharSequence, CharSequence>?> =
            arrayOfNulls(1)
        val latch = CountDownLatch(1)

        gutenbergView.getTitleAndContent(
            originalContent,
            object : TitleAndContentCallback {
                override fun onResult(
                    title: CharSequence, content: CharSequence
                ) {
                    result[0] = Pair(title, content)
                    latch.countDown()
                }
            },
            completeComposition
        )

        val finalResult = try {
            latch.await()
            result[0]
        } catch (e: InterruptedException) {
            AppLog.w(
                AppLog.T.EDITOR,
                "Thread interrupted while waiting for title and " +
                    "content from Gutenberg editor: $e"
            )
            Thread.currentThread().interrupt()
            null
        }

        return finalResult ?: Pair("", "")
    }

    override fun getEditorName(): String {
        return GUTENBERG_EDITOR_NAME
    }

    @Throws(EditorFragmentNotAddedException::class)
    override fun getContent(originalContent: CharSequence?): CharSequence {
        if (!isAdded) {
            throw EditorFragmentNotAddedException()
        }

        return getTitleAndContent(originalContent ?: "").second
    }

    override fun getTitleOrContentChanged(): LiveData<Editable> {
        return textWatcher.afterTextChanged
    }

    override fun appendMediaFiles(
        mediaList: MutableMap<String?, MediaFile?>
    ) {
        // appendMediaFile may be called from a background thread
        // (example: EditPostActivity.java#L2165) and Activity may
        // have already be gone.
        // Ticket: https://github.com/wordpress-mobile/WordPress-Android/issues/7386
        if (activity == null) {
            AppLog.d(
                AppLog.T.MEDIA,
                "appendMediaFiles() called but Activity is null!"
            )
            return
        }

        // Get media URL of first of media first to check
        // if it is network or local one.
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

        val processedMediaList =
            mediaList.entries.map { (url, mediaFile) ->
                val mediaId = mediaFile!!.mediaId.toInt()
                Media.createMediaUsingMimeType(
                    mediaId, url!!, mediaFile.mimeType,
                    mediaFile.caption, mediaFile.title, mediaFile.alt
                )
            }

        val mediaString = Gson().toJson(processedMediaList)
        gutenbergView?.setMediaUploadAttachment(mediaString)
    }

    override fun onDestroy() {
        gutenbergView?.let {
            historyChangeListener = null
            featuredImageChangeListener = null
        }
        super.onDestroy()
    }

    fun setXPostsEnabled(enabled: Boolean) {
        isXPostsEnabled = enabled
    }

    fun setNetworkRequestListener(
        listener: GutenbergView.NetworkRequestListener
    ) {
        networkRequestListener = listener
        gutenbergView?.setNetworkRequestListener(listener)
    }

    override fun onUndoPressed() {
        gutenbergView?.undo()
    }

    override fun onRedoPressed() {
        gutenbergView?.redo()
    }

    fun dismissTopModal() {
        gutenbergView?.dismissTopModal()
    }

    companion object {
        private const val GUTENBERG_EDITOR_NAME = "gutenberg"
        private const val KEY_HTML_MODE_ENABLED = "KEY_HTML_MODE_ENABLED"
        const val ARG_FEATURED_IMAGE_ID: String = "featured_image_id"
        const val ARG_GUTENBERG_KIT_SETTINGS: String =
            "gutenberg_kit_settings"

        private const val CAPTURE_PHOTO_PERMISSION_REQUEST_CODE = 101
        private const val CAPTURE_VIDEO_PERMISSION_REQUEST_CODE = 102

        fun newInstance(
            configuration: EditorConfiguration
        ): GutenbergKitEditorFragment {
            val fragment = GutenbergKitEditorFragment()
            val args = Bundle()
            args.putParcelable(ARG_GUTENBERG_KIT_SETTINGS, configuration)
            fragment.arguments = args
            return fragment
        }
    }
}
