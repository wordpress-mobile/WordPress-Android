package org.wordpress.android.ui.stories

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.AuthenticationHeadersProvider
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.MetadataProvider
import com.wordpress.stories.compose.NotificationIntentLoader
import com.wordpress.stories.compose.SnackbarProvider
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.util.KEY_STORY_INDEX
import org.wordpress.android.R.id
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.ProgressDialogHelper
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddExistingMediaSource.WP_MEDIA_LIBRARY
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddMediaToPostUiState
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.posts.editor.media.EditorType.STORY_EDITOR
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ListUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.widgets.WPSnackbar
import java.util.Objects
import javax.inject.Inject

class StoryComposerActivity : ComposeLoopFrameActivity(),
        SnackbarProvider,
        MediaPickerProvider,
        EditorMediaListener,
        AuthenticationHeadersProvider,
        NotificationIntentLoader,
        MetadataProvider {
    private var site: SiteModel? = null

    @Inject lateinit var editorMedia: EditorMedia
    @Inject lateinit var progressDialogHelper: ProgressDialogHelper
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var authenticationUtils: AuthenticationUtils
    @Inject lateinit var editPostRepository: EditPostRepository

    private var addingMediaToEditorProgressDialog: ProgressDialog? = null

    companion object {
        // arbitrary post format for Stories. Will be used in Posts lists for filtering.
        // See https://wordpress.org/support/article/post-formats/
        private val POST_FORMAT_WP_STORY_KEY = "wpstory"
        private const val STATE_KEY_POST_LOCAL_ID = "stateKeyPostModelLocalId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
        setAuthenticationProvider(this)
        setNotificationExtrasLoader(this)
        setMetadataProvider(this)

        if (savedInstanceState == null) {
            site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            // Create a new post
            editPostRepository.set {
                val post: PostModel = postStore.instantiatePostModel(site, false, null, null)
                post.setStatus(PUBLISHED.toString())
                post
            }
        } else {
            site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
            if (savedInstanceState.containsKey(STATE_KEY_POST_LOCAL_ID)) {
                editPostRepository.loadPostByLocalPostId(savedInstanceState.getInt(STATE_KEY_POST_LOCAL_ID))
            }
        }

        editorMedia.start(site!!, this, STORY_EDITOR)
        startObserving()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
        outState.putInt(STATE_KEY_POST_LOCAL_ID, editPostRepository.id)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            when (requestCode) {
                RequestCodes.MULTI_SELECT_MEDIA_PICKER, RequestCodes.SINGLE_SELECT_MEDIA_PICKER -> {
                    handleMediaPickerResult(it)
                }
                RequestCodes.PHOTO_PICKER -> {
                    if (it.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)) {
                        val uriList: List<Uri> = convertStringArrayIntoUrisList(
                                it.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)
                        )
                        editorMedia.onPhotoPickerMediaChosen(uriList)
                    } else if (it.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                        handleMediaPickerResult(it)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        editorMedia.cancelAddMediaToEditorActions()
        super.onDestroy()
    }

    override fun showProvidedSnackbar(message: String, actionLabel: String?, callback: () -> Unit) {
        // no op
        // no provided snackbar here given we're not using snackbars from within the Story Creation experience
        // in WPAndroid
    }

    override fun setupRequestCodes(requestCodes: ExternalMediaPickerRequestCodesAndExtraKeys) {
        requestCodes.PHOTO_PICKER = RequestCodes.PHOTO_PICKER
        requestCodes.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED =
                PhotoPickerActivity.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED
        requestCodes.EXTRA_MEDIA_URIS = PhotoPickerActivity.EXTRA_MEDIA_URIS
    }

    override fun showProvidedMediaPicker() {
        ActivityLauncher.showPhotoPickerForResult(
                this,
                MediaBrowserType.WP_STORIES_MEDIA_PICKER,
                site,
                null // this is not required, only used for featured image in normal Posts
        )
    }

    override fun providerHandlesOnActivityResult(): Boolean {
        // lets the super class know we're handling media picking OnActivityResult
        return true
    }

    fun handleMediaPickerResult(data: Intent) {
        // TODO move this to EditorMedia
        val ids = ListUtils.fromLongArray(
                data.getLongArrayExtra(
                        MediaBrowserActivity.RESULT_IDS
                )
        )
        if (ids == null || ids.size == 0) {
            return
        }

        editorMedia.addExistingMediaToEditorAsync(WP_MEDIA_LIBRARY, ids)
    }

    private fun startObserving() {
        editorMedia.uiState.observe(this,
                Observer { uiState: AddMediaToPostUiState? ->
                    if (uiState != null) {
                        updateAddingMediaToEditorProgressDialogState(uiState.progressDialogUiState)
                        if (uiState.editorOverlayVisibility) {
                            showLoading()
                        } else {
                            hideLoading()
                        }
                    }
                }
        )
        editorMedia.snackBarMessage.observe(this,
                Observer<Event<SnackbarMessageHolder>> { event: Event<SnackbarMessageHolder?> ->
                    val messageHolder = event.getContentIfNotHandled()
                    if (messageHolder != null) {
                        WPSnackbar
                                .make(
                                        findViewById(id.editor_activity),
                                        messageHolder.messageRes,
                                        Snackbar.LENGTH_SHORT
                                )
                                .show()
                    }
                }
        )
        editorMedia.toastMessage.observe(this,
                Observer<Event<ToastMessageHolder>> { event: Event<ToastMessageHolder?> ->
                    val contentIfNotHandled = event.getContentIfNotHandled()
                    contentIfNotHandled?.show(this)
                }
        )
    }

    // EditorMediaListener
    override fun appendMediaFiles(mediaFiles: Map<String, MediaFile>) {
        val uriList = ArrayList<Uri>()
        for ((key) in mediaFiles.entries) {
            val url = if (URLUtil.isNetworkUrl(key)) {
                key
            } else {
                "file://$key"
            }
            uriList.add(Uri.parse(url))
        }

        addFramesToStoryFromMediaUriList(uriList)
        setDefaultSelectionAndUpdateBackgroundSurfaceUI()
    }

    override fun getImmutablePost(): PostImmutableModel {
        return Objects.requireNonNull(editPostRepository.getPost()!!)
    }

    override fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener?) {
        // TODO will implement when we support StoryPost editing
        // updateAndSavePostAsync(listener)
    }

    override fun advertiseImageOptimization(listener: () -> Unit) {
        WPMediaUtils.advertiseImageOptimization(this) { listener.invoke() }
    }

    private fun updateAddingMediaToEditorProgressDialogState(uiState: ProgressDialogUiState) {
        addingMediaToEditorProgressDialog = progressDialogHelper
                .updateProgressDialogState(this, addingMediaToEditorProgressDialog, uiState, uiHelpers)
    }

    override fun getAuthHeaders(url: String): Map<String, String> {
        return authenticationUtils.getAuthHeaders(url)
    }

    // NotificationIntentLoader
    override fun loadIntentForErrorNotification(): Intent {
        val notificationIntent = Intent(applicationContext, StoryComposerActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        notificationIntent.putExtra(WordPress.SITE, site)
        // TODO WPSTORIES add TRACKS
        // add NotificationType.MEDIA_SAVE_ERROR param later when integrating with WPAndroid
//        val notificationType = NotificationType.MEDIA_SAVE_ERROR
//        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType)
        return notificationIntent
    }

    override fun loadMetadataForStory(index: StoryIndex): Bundle? {
        val bundle = Bundle()
        bundle.putSerializable(WordPress.SITE, site)
        bundle.putInt(KEY_STORY_INDEX, index)
        return bundle
    }
}
