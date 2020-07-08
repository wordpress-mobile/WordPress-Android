package org.wordpress.android.ui.stories

import android.app.PendingIntent
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
import com.wordpress.stories.compose.PrepublishingEventProvider
import com.wordpress.stories.compose.SnackbarProvider
import com.wordpress.stories.compose.StoryDiscardListener
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.util.KEY_STORY_INDEX
import com.wordpress.stories.util.KEY_STORY_SAVE_RESULT
import org.wordpress.android.R.id
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Outcome.CANCEL
import org.wordpress.android.ui.posts.PrepublishingBottomSheetFragment
import org.wordpress.android.ui.posts.ProgressDialogHelper
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.PublishPost
import org.wordpress.android.ui.posts.SavePostToDbUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddExistingMediaSource.WP_MEDIA_LIBRARY
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddMediaToPostUiState
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.posts.editor.media.EditorType.STORY_EDITOR
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetListener
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ListUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
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
        MetadataProvider,
        StoryDiscardListener,
        EditPostActivityHook,
        PrepublishingEventProvider,
        PrepublishingBottomSheetListener {
    private var site: SiteModel? = null

    @Inject lateinit var editorMedia: EditorMedia
    @Inject lateinit var progressDialogHelper: ProgressDialogHelper
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var authenticationUtils: AuthenticationUtils
    @Inject internal lateinit var editPostRepository: EditPostRepository
    @Inject lateinit var savePostToDbUseCase: SavePostToDbUseCase
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var systemNotificationsTracker: SystemNotificationsTracker
    private var postEditorAnalyticsSession: PostEditorAnalyticsSession? = null

    private var addingMediaToEditorProgressDialog: ProgressDialog? = null

    override fun getSite() = site
    override fun getEditPostRepository() = editPostRepository

    companion object {
        // arbitrary post format for Stories. Will be used in Posts lists for filtering.
        // See https://wordpress.org/support/article/post-formats/
        const val POST_FORMAT_WP_STORY_KEY = "wpstory"
        private const val STATE_KEY_POST_LOCAL_ID = "state_key_post_model_local_id"
        private const val STATE_KEY_EDITOR_SESSION_DATA = "stateKeyEditorSessionData"
        const val KEY_POST_LOCAL_ID = "key_post_model_local_id"
        const val BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID: Int = 72300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setSnackbarProvider(this)
        setMediaPickerProvider(this)
        setAuthenticationProvider(this)
        setNotificationExtrasLoader(this)
        setMetadataProvider(this)
        setStoryDiscardListener(this)
        setNotificationTrackerProvider((application as WordPress).getStoryNotificationTrackerProvider())
        setPrepublishingEventProvider(this)

        if (savedInstanceState == null) {
            site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            var localPostId = getBackingPostIdFromIntent()
            if (localPostId == 0) {
                // Create a new post
                saveInitialPost()
                // Bump post created analytics only once, first time the editor is opened
                AnalyticsUtils.trackEditorCreatedPost(
                        intent.action,
                        intent,
                        site,
                        editPostRepository.getPost()
                )
            } else {
                editPostRepository.loadPostByLocalPostId(localPostId)
            }
            createPostEditorAnalyticsSessionTracker(editPostRepository.getPost(), site)

            if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
                val notificationType = intent.getSerializableExtra(ARG_NOTIFICATION_TYPE) as NotificationType
                systemNotificationsTracker.trackTappedNotification(notificationType)
            }
        } else {
            site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
            if (savedInstanceState.containsKey(STATE_KEY_POST_LOCAL_ID)) {
                editPostRepository.loadPostByLocalPostId(savedInstanceState.getInt(STATE_KEY_POST_LOCAL_ID))
            }
            postEditorAnalyticsSession =
                    savedInstanceState.getSerializable(STATE_KEY_EDITOR_SESSION_DATA) as PostEditorAnalyticsSession
        }

        editorMedia.start(requireNotNull(site), this, STORY_EDITOR)
        postEditorAnalyticsSession?.start(null)
        startObserving()
        updateStoryPostWithChanges()
    }

    override fun onLoadFromIntent(intent: Intent) {
        super.onLoadFromIntent(intent)
        // now see if we need to handle information coming from the MediaPicker to populate
        handleMediaPickerIntentData(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
        outState.putInt(STATE_KEY_POST_LOCAL_ID, editPostRepository.id)
        outState.putSerializable(STATE_KEY_EDITOR_SESSION_DATA, postEditorAnalyticsSession)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            when (requestCode) {
                RequestCodes.MULTI_SELECT_MEDIA_PICKER, RequestCodes.SINGLE_SELECT_MEDIA_PICKER -> {
                    handleMediaPickerIntentData(it)
                }
                RequestCodes.PHOTO_PICKER -> {
                    if (it.hasExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)) {
                        val uriList: List<Uri> = convertStringArrayIntoUrisList(
                                it.getStringArrayExtra(PhotoPickerActivity.EXTRA_MEDIA_URIS)
                        )
                        editorMedia.onPhotoPickerMediaChosen(uriList)
                    } else if (it.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                        handleMediaPickerIntentData(it)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        editorMedia.cancelAddMediaToEditorActions()
        postEditorAnalyticsSession?.end()
        super.onDestroy()
    }

    private fun getBackingPostIdFromIntent(): Int {
        var localPostId = intent.getIntExtra(KEY_POST_LOCAL_ID, 0)
        if (localPostId == 0) {
            if (intent.hasExtra(KEY_STORY_SAVE_RESULT)) {
                val storySaveResult =
                        intent.getParcelableExtra(KEY_STORY_SAVE_RESULT) as StorySaveResult?
                storySaveResult?.let {
                    localPostId = it.metadata?.getInt(KEY_POST_LOCAL_ID, 0) ?: 0
                }
            }
        }
        return localPostId
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

    fun handleMediaPickerIntentData(data: Intent) {
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

    private fun saveInitialPost() {
        editPostRepository.set {
            val post: PostModel = postStore.instantiatePostModel(site, false, null, null)
            post.setStatus(PostStatus.DRAFT.toString())
            post
        }
        editPostRepository.savePostSnapshot()
        // this is an artifact to be able to call savePostToDb()
        editPostRepository.getEditablePost()?.setPostFormat(POST_FORMAT_WP_STORY_KEY)
        site?.let {
            savePostToDbUseCase.savePostToDb(this, editPostRepository, it)
        }
    }

    private fun updateStoryPostWithChanges() {
        editPostRepository.postChanged.observe(this, Observer {
            savePostToDbUseCase.savePostToDb(this, editPostRepository, requireNotNull(site))
        })
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
        setDefaultSelectionAndUpdateBackgroundSurfaceUI(uriList)
    }

    override fun getImmutablePost(): PostImmutableModel {
        return Objects.requireNonNull(editPostRepository.getPost()!!)
    }

    override fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener?) {
        // TODO will implement when we support StoryPost editing
        // updateAndSavePostAsync(listener)
        // Ignore the result as we want to invoke the listener even when the PostModel was up-to-date
        listener?.onPostUpdatedFromUI()
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

    // region NotificationIntentLoader
    override fun loadIntentForErrorNotification(): Intent {
        val notificationIntent = Intent(applicationContext, StoryComposerActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        notificationIntent.putExtra(WordPress.SITE, site)
        // setup tracks NotificationType for Notification tracking. Note this doesn't use our interface.
        val notificationType = NotificationType.STORY_SAVE_ERROR
        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType)
        return notificationIntent
    }

    override fun loadPendingIntentForErrorNotificationDeletion(notificationId: Int): PendingIntent? {
        return NotificationsProcessingService
                .getPendingIntentForNotificationDismiss(
                        applicationContext,
                        notificationId,
                        NotificationType.STORY_SAVE_ERROR
                )
    }

    override fun setupErrorNotificationBaseId(): Int {
        return BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID
    }
    // endregion

    override fun loadMetadataForStory(index: StoryIndex): Bundle? {
        val bundle = Bundle()
        bundle.putSerializable(WordPress.SITE, site)
        bundle.putInt(KEY_STORY_INDEX, index)
        bundle.putInt(KEY_POST_LOCAL_ID, editPostRepository.id)
        return bundle
    }

    override fun onStoryDiscarded() {
        // delete empty post from database
        dispatcher.dispatch(PostActionBuilder.newRemovePostAction(editPostRepository.getEditablePost()))
        postEditorAnalyticsSession?.setOutcome(CANCEL)
    }

    private fun createPostEditorAnalyticsSessionTracker(
        post: PostImmutableModel?,
        site: SiteModel?
    ) {
        if (postEditorAnalyticsSession == null) {
            postEditorAnalyticsSession = PostEditorAnalyticsSession.getNewPostEditorAnalyticsSession(
                    PostEditorAnalyticsSession.Editor.WP_STORIES_CREATOR,
                    post, site, true
            )
        }
    }

    private fun showPrepublishingBottomSheet() {
        val fragment = supportFragmentManager.findFragmentByTag(PrepublishingBottomSheetFragment.TAG)
        if (fragment == null) {
            val prepublishingFragment = PrepublishingBottomSheetFragment.newInstance(
                    site = requireNotNull(site),
                    isPage = editPostRepository.isPage,
                    isStoryPost = true
            )
            prepublishingFragment.show(supportFragmentManager, PrepublishingBottomSheetFragment.TAG)
        }
    }

    override fun onStorySaveButtonPressed() {
        showPrepublishingBottomSheet()
    }

    override fun onSubmitButtonClicked(publishPost: PublishPost) {
        processStorySaving()
    }
}
