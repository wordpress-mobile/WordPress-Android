package org.wordpress.android.ui.stories

import android.app.Activity
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.AuthenticationHeadersProvider
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.FrameSaveErrorDialog
import com.wordpress.stories.compose.FrameSaveErrorDialogOk
import com.wordpress.stories.compose.GenericAnnouncementDialogProvider
import com.wordpress.stories.compose.MediaPickerProvider
import com.wordpress.stories.compose.MetadataProvider
import com.wordpress.stories.compose.NotificationIntentLoader
import com.wordpress.stories.compose.PermanentPermissionDenialDialogProvider
import com.wordpress.stories.compose.PrepublishingEventProvider
import com.wordpress.stories.compose.SnackbarProvider
import com.wordpress.stories.compose.StoryDiscardListener
import com.wordpress.stories.compose.frame.StoryLoadEvents.StoryLoadEnd
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.FileBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItem.BackgroundSource.UriBackgroundSource
import com.wordpress.stories.compose.story.StoryFrameItemType.VIDEO
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.compose.story.StoryRepository.DEFAULT_NONE_SELECTED
import com.wordpress.stories.util.KEY_STORY_EDIT_MODE
import com.wordpress.stories.util.KEY_STORY_INDEX
import com.wordpress.stories.util.KEY_STORY_SAVE_RESULT
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.PREPUBLISHING_BOTTOM_SHEET_OPENED
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment.ARG_STORY_BLOCK_ID
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment.ARG_STORY_BLOCK_UPDATED_CONTENT
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.push.NotificationType
import org.wordpress.android.push.NotificationsProcessingService
import org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession
import org.wordpress.android.ui.posts.PrepublishingBottomSheetFragment
import org.wordpress.android.ui.posts.ProgressDialogHelper
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.PublishPost
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaSource.WP_MEDIA_LIBRARY
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetListener
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.StoryMediaFileData
import org.wordpress.android.ui.stories.media.StoryEditorMedia
import org.wordpress.android.ui.stories.media.StoryEditorMedia.AddMediaToStoryPostUiState
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.ListUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.viewmodel.observeEvent
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
        PrepublishingBottomSheetListener,
        PermanentPermissionDenialDialogProvider,
        GenericAnnouncementDialogProvider {
    private var site: SiteModel? = null

    @Inject lateinit var storyEditorMedia: StoryEditorMedia
    @Inject lateinit var progressDialogHelper: ProgressDialogHelper
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var postStore: PostStore
    @Inject lateinit var authenticationUtils: AuthenticationUtils
    @Inject internal lateinit var editPostRepository: EditPostRepository
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Inject lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var mediaPickerLauncher: MediaPickerLauncher
    @Inject lateinit var saveStoryGutenbergBlockUseCase: SaveStoryGutenbergBlockUseCase
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper
    @Inject lateinit var storyRepositoryWrapper: StoryRepositoryWrapper
    @Inject lateinit var storiesPrefs: StoriesPrefs

    private lateinit var viewModel: StoryComposerViewModel

    private var addingMediaToEditorProgressDialog: ProgressDialog? = null
    private val frameIdsToRemove = ArrayList<String>()

    override fun getSite() = site
    override fun getEditPostRepository() = editPostRepository

    companion object {
        protected const val FRAGMENT_ANNOUNCEMENT_DIALOG = "story_announcement_dialog"
        const val STATE_KEY_POST_LOCAL_ID = "state_key_post_model_local_id"
        const val STATE_KEY_EDITOR_SESSION_DATA = "stateKeyEditorSessionData"
        const val STATE_KEY_ORIGINAL_STORY_SAVE_RESULT = "stateKeyOriginalSaveResult"
        const val KEY_POST_LOCAL_ID = "key_post_model_local_id"
        const val KEY_LAUNCHED_FROM_GUTENBERG = "key_launched_from_gutenberg"
        const val KEY_ALL_UNFLATTENED_LOADED_SLIDES = "key_all_unflattened_laoded_slides"
        const val UNUSED_KEY = "unused_key"
        const val BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID: Int = 72300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // convert our WPAndroid KEY_LAUNCHED_FROM_GUTENBERG flag into Stories general purpose EDIT_MODE flag
        intent.putExtra(KEY_STORY_EDIT_MODE, intent.getBooleanExtra(KEY_LAUNCHED_FROM_GUTENBERG, false))
        setMediaPickerProvider(this)
        (application as WordPress).component().inject(this)
        initSite(savedInstanceState)
        setSnackbarProvider(this)
        setAuthenticationProvider(this)
        setNotificationExtrasLoader(this)
        setMetadataProvider(this)
        setStoryDiscardListener(this)
        setStoriesAnalyticsListener(StoriesAnalyticsReceiver())
        setNotificationTrackerProvider((application as WordPress).storyNotificationTrackerProvider)
        setPrepublishingEventProvider(this)
        setPermissionDialogProvider(this)
        setGenericAnnouncementDialogProvider(this)

        initViewModel(savedInstanceState)
        super.onCreate(savedInstanceState)

        setUseTempCaptureFile(false) // we need to keep the captured files for later Story editing
    }

    private fun initSite(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        var localPostId = 0
        var notificationType: NotificationType? = null
        var originalStorySaveResult: StorySaveResult? = null

        if (savedInstanceState == null) {
            localPostId = getBackingPostIdFromIntent()
            originalStorySaveResult = intent.getParcelableExtra(KEY_STORY_SAVE_RESULT) as StorySaveResult?

            if (intent.hasExtra(ARG_NOTIFICATION_TYPE)) {
                notificationType = intent.getSerializableExtra(ARG_NOTIFICATION_TYPE) as NotificationType
            }
        } else {
            if (savedInstanceState.containsKey(STATE_KEY_POST_LOCAL_ID)) {
                localPostId = savedInstanceState.getInt(STATE_KEY_POST_LOCAL_ID)
            }
            if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_STORY_SAVE_RESULT)) {
                originalStorySaveResult =
                        savedInstanceState.getParcelable(STATE_KEY_ORIGINAL_STORY_SAVE_RESULT) as StorySaveResult?
            }
        }

        val postEditorAnalyticsSession = savedInstanceState?.let { bundle ->
            PostEditorAnalyticsSession.fromBundle(bundle, STATE_KEY_EDITOR_SESSION_DATA, analyticsTrackerWrapper)
        }

        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(StoryComposerViewModel::class.java)

        site?.let {
            val postInitialized = viewModel.start(
                    it,
                    editPostRepository,
                    LocalId(localPostId),
                    postEditorAnalyticsSession,
                    notificationType,
                    originalStorySaveResult
            )

            // Ensure we have a valid post
            if (!postInitialized) {
                showErrorAndFinish(R.string.post_not_found)
                return@let
            }
        }

        storyEditorMedia.start(requireNotNull(site), this)
        setupStoryEditorMediaObserver()
        setupViewModelObservers()
    }

    private fun setupViewModelObservers() {
        viewModel.mediaFilesUris.observe(this, { uriList ->
            val filteredList = uriList.filterNot { MediaUtils.isGif(it.toString()) }
            if (filteredList.isNotEmpty()) {
                addFramesToStoryFromMediaUriList(filteredList)
                setDefaultSelectionAndUpdateBackgroundSurfaceUI(filteredList)
                // generally speaking, adding media will happen at the beginning of loading the StoryComposer, so once
                // it's done adding media the StoryComposer will be ready to render the newly loaded / created Story.
                // Hence, it makes sense to start the editor session tracking at this point - note subsequent calls
                // will have no effect, given PostEditorAnalyticsSession has a flag so it can only be started once.
                viewModel.onStoryComposerStartAnalyticsSession()
            }

            // finally if any of the files was a gif, warn the user
            if (filteredList.size != uriList.size) {
                FrameSaveErrorDialog.newInstance(
                        title = getString(R.string.dialog_edit_story_unsupported_format_title),
                        message = getString(R.string.dialog_edit_story_unsupported_format_message),
                        hideCancelButton = true,
                        listener = object : FrameSaveErrorDialogOk {
                            override fun OnOkClicked(dialog: DialogFragment) {
                                if (filteredList.isEmpty()) {
                                    onStoryDiscarded()
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                }
                            }
                        }
                ).show(supportFragmentManager, FRAGMENT_ANNOUNCEMENT_DIALOG)
            }
        })

        viewModel.openPrepublishingBottomSheet.observeEvent(this, {
            analyticsTrackerWrapper.track(PREPUBLISHING_BOTTOM_SHEET_OPENED)
            openPrepublishingBottomSheet()
        })

        viewModel.submitButtonClicked.observeEvent(this, {
            analyticsTrackerWrapper.track(Stat.STORY_POST_PUBLISH_TAPPED)
            processStorySaving()
        })

        viewModel.trackEditorCreatedPost.observeEvent(this, {
            site?.let {
                analyticsUtilsWrapper.trackEditorCreatedPost(
                        intent.action,
                        intent,
                        it,
                        editPostRepository.getPost()
                )
            }
        })
    }

    private fun showErrorAndFinish(errorMessageId: Int) {
        ToastUtils.showToast(
                this,
                errorMessageId,
                ToastUtils.Duration.LONG
        )
        finish()
    }

    override fun onLoadFromIntent(intent: Intent) {
        super.onLoadFromIntent(intent)
        // now see if we need to handle information coming from the MediaPicker to populate
        handleMediaPickerIntentData(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    @Suppress("NestedBlockDepth")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.onStoryComposerAnalyticsSessionStartTimeReset()
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            when (requestCode) {
                RequestCodes.MULTI_SELECT_MEDIA_PICKER, RequestCodes.SINGLE_SELECT_MEDIA_PICKER -> {
                    handleMediaPickerIntentData(it)
                }
                RequestCodes.PHOTO_PICKER, RequestCodes.STORIES_PHOTO_PICKER -> {
                    when {
                        it.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS) -> {
                            data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)?.let {
                                val uriList: List<Uri> = convertStringArrayIntoUrisList(it)
                                storyEditorMedia.onPhotoPickerMediaChosen(uriList)
                            }
                        }
                        it.hasExtra(MediaBrowserActivity.RESULT_IDS) -> {
                            handleMediaPickerIntentData(it)
                        }
                        else -> {
                            // handleMediaPickerIntentData(it)?
                        }
                    }
                }
                else -> {
                    // handleMediaPickerIntentData(it)?
                }
            }
        }
    }

    override fun onDestroy() {
        storyEditorMedia.cancelAddMediaToEditorActions()
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
                MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED
        requestCodes.EXTRA_LAUNCH_WPSTORIES_MEDIA_PICKER_REQUESTED =
                MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_MEDIA_PICKER_REQUESTED
        // we're handling EXTRA_MEDIA_URIS at the app level (not at the Stories library level)
        // hence we set the requestCode to UNUSED
        requestCodes.EXTRA_MEDIA_URIS = UNUSED_KEY
    }

    override fun showProvidedMediaPicker() {
        mediaPickerLauncher.showStoriesPhotoPickerForResult(
                this,
                site
        )
    }

    override fun providerHandlesOnActivityResult(): Boolean {
        // lets the super class know we're handling media picking OnActivityResult
        return true
    }

    private fun handleMediaPickerIntentData(data: Intent) {
        if (permissionsRequestForCameraInProgress) {
            return
        }

        when {
            data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS) -> {
                data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)?.let {
                    val uriList: List<Uri> = convertStringArrayIntoUrisList(it)

                    if (uriList.isNotEmpty()) {
                        storyEditorMedia.onPhotoPickerMediaChosen(uriList)
                    }
                }
            }
            data.hasExtra(MediaBrowserActivity.RESULT_IDS) -> {
                val ids = ListUtils.fromLongArray(
                        data.getLongArrayExtra(
                                MediaBrowserActivity.RESULT_IDS
                        )
                )
                if (ids == null || ids.size == 0) {
                    return
                }
                storyEditorMedia.addExistingMediaToEditorAsync(WP_MEDIA_LIBRARY, ids)
            }
            data.hasExtra(MediaPickerConstants.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED) -> {
                // when coming from this entry point, we can start the analytics session
                viewModel.onStoryComposerStartAnalyticsSession()
            }
        }
    }

    private fun setupStoryEditorMediaObserver() {
        storyEditorMedia.uiState.observe(this,
                Observer { uiState: AddMediaToStoryPostUiState? ->
                    if (uiState != null) {
                        updateAddingMediaToStoryComposerProgressDialogState(uiState.progressDialogUiState)
                        if (uiState.editorOverlayVisibility) {
                            showLoading()
                        } else {
                            hideLoading()
                        }
                    }
                }
        )
        storyEditorMedia.snackBarMessage.observeEvent(this,
                { messageHolder ->
                    findViewById<View>(R.id.compose_loop_frame_layout)?.let {
                        WPSnackbar
                                .make(
                                        it,
                                        uiHelpers.getTextOfUiString(this, messageHolder.message),
                                        Snackbar.LENGTH_SHORT
                                )
                                .show()
                    }
                }
        )
    }

    // EditorMediaListener
    override fun appendMediaFiles(mediaFiles: Map<String, MediaFile>) {
        viewModel.appendMediaFiles(mediaFiles)
    }

    override fun getImmutablePost(): PostImmutableModel {
        return Objects.requireNonNull(editPostRepository.getPost()!!)
    }

    override fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener?) {
        // TODO will implement when we support StoryPost editing
        // updateAndSavePostAsync(listener)
        // Ignore the result as we want to invoke the listener even when the PostModel was up-to-date
        listener?.onPostUpdatedFromUI(null)
    }

    override fun advertiseImageOptimization(listener: () -> Unit) {
        WPMediaUtils.advertiseImageOptimization(this) { listener.invoke() }
    }

    override fun onMediaModelsCreatedFromOptimizedUris(oldUriToMediaFiles: Map<Uri, MediaModel>) {
        // no op - we're not doing any special handling while composing, only when saving in the UploadBridge
    }

    override fun showVideoDurationLimitWarning(fileName: String) {
        ToastUtils.showToast(this, string.error_media_video_duration_exceeds_limit, LONG)
    }

    private fun updateAddingMediaToStoryComposerProgressDialogState(uiState: ProgressDialogUiState) {
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
        val launchedFromGutenberg = intent.getBooleanExtra(KEY_LAUNCHED_FROM_GUTENBERG, false)
        val storyDiscardedFromRetry = viewModel.onStoryDiscarded(!launchedFromGutenberg)

        if (launchedFromGutenberg || storyDiscardedFromRetry) {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onFrameRemove(storyIndex: StoryIndex, storyFrameIndex: Int) {
        // keep record of the frames users deleted.
        // But we'll only actually do cleanup once they tap on the DONE/SAVE button, because they could
        // still bail out of the StoryComposer by tapping back or the cross and then admitting they want to lose
        // the changes they made (this means, they'd want to keep the stories).
        val story = storyRepositoryWrapper.getStoryAtIndex(storyIndex)
        if (storyFrameIndex < story.frames.size) {
            story.frames[storyFrameIndex].id?.let {
                frameIdsToRemove.add(it)
            }
        }
    }

    private fun openPrepublishingBottomSheet() {
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
        if (intent.getBooleanExtra(KEY_LAUNCHED_FROM_GUTENBERG, false)) {
            // first of all, remove any StoriesPref for removed slides
            site?.let {
                val siteLocalId = it.id.toLong()
                for (frameId in frameIdsToRemove) {
                    if (storiesPrefs.checkSlideIdExists(siteLocalId, RemoteId(frameId.toLong()))) {
                        storiesPrefs.deleteSlideWithRemoteId(siteLocalId, RemoteId(frameId.toLong()))
                    } else {
                        // shouldn't happen but just in case the story frame has just been created but not yet uploaded
                        // let's delete the local slide pref.
                        storiesPrefs.deleteSlideWithLocalId(siteLocalId, LocalId(frameId.toInt()))
                    }
                }
            }

            viewModel.onStorySaved()
            // TODO add tracks
            processStorySaving()

            val savedContentIntent = Intent()
            val blockId = intent.extras?.getString(ARG_STORY_BLOCK_ID)
            savedContentIntent.putExtra(ARG_STORY_BLOCK_ID, blockId)

            // check if story index has been passed through intent
            var storyIndex = intent.getIntExtra(KEY_STORY_INDEX, DEFAULT_NONE_SELECTED)
            if (storyIndex == DEFAULT_NONE_SELECTED) {
                // if not, let's use the current Story
                storyIndex = storyRepositoryWrapper.getCurrentStoryIndex()
            }

            // if we are editing this Story Block, then the id is assured to be a remote media file id, but
            // the frame no longer points to such media Id on the site given we are just about to save a
            // new flattened media. Hence, we need to set a new temporary Id we can use to identify
            // this frame within the Gutenberg Story block inside a Post, and match it to an existing Story frame in
            // our StoryRepository.
            // All of this while still keeping a valid "old" remote URl and mediaId so the block is still
            // rendered as non-empty on mobile gutenberg while the actual flattening happens on the service.
            val updatedStoryBlock =
                    saveStoryGutenbergBlockUseCase.buildJetpackStoryBlockStringFromStoryMediaFileData(
                            buildStoryMediaFileDataListFromStoryFrameIndexes(storyIndex)
                    )

            savedContentIntent.putExtra(ARG_STORY_BLOCK_UPDATED_CONTENT, updatedStoryBlock)
            setResult(Activity.RESULT_OK, savedContentIntent)
            finish()
        } else {
            // assume this is a new Post, and proceed to PrePublish bottom sheet
            viewModel.onStorySaveButtonPressed()
        }
    }

    private fun buildStoryMediaFileDataListFromStoryFrameIndexes(
        storyIndex: StoryIndex
    ): ArrayList<StoryMediaFileData> {
        val storyMediaFileDataList = ArrayList<StoryMediaFileData>() // holds media files
        val story = storyRepositoryWrapper.getStoryAtIndex(storyIndex)
        for ((frameIndex, frame) in story.frames.withIndex()) {
            val newTempId = storiesPrefs.getNewIncrementalTempId()
            val assignedTempId = saveStoryGutenbergBlockUseCase.getTempIdForStoryFrame(
                    newTempId, storyIndex, frameIndex
            )
            when (frame.id) {
                // if the frame.id is null, this is a new frame that has been added to an edited Story
                // so, we don't have much information yet. We do have the background source (not the flattened
                // image yet) so, let's use that for now, and assign the temporaryID we'll use to send
                // save progress events to Gutenberg.
                null -> {
                    val storyMediaFileData = buildStoryMediaFileDataForTemporarySlide(
                            frame,
                            assignedTempId
                    )
                    frame.id = storyMediaFileData.id
                    storyMediaFileDataList.add(storyMediaFileData)
                }
                // if the frame.id is populated and is not a temporary id, this should be an actual MediaModel mediaId so,
                // let's use that to obtain the mediaFile and then replace it with the temporary frame.id
                else -> {
                    frame.id?.let {
                        if (it.startsWith(TEMPORARY_ID_PREFIX)) {
                            val storyMediaFileData = buildStoryMediaFileDataForTemporarySlide(
                                    frame,
                                    it
                            )
                            storyMediaFileDataList.add(storyMediaFileData)
                        } else {
                            val mediaModel = mediaStore.getSiteMediaWithId(site, it.toLong())
                            val mediaFile = fluxCUtilsWrapper.mediaFileFromMediaModel(mediaModel)
                            mediaFile?.let { mediafile ->
                                mediaFile.alt = StoryFrameItem.getAltTextFromFrameAddedViews(frame)
                                mediaModel.alt = mediaFile.alt
                                val storyMediaFileData =
                                        saveStoryGutenbergBlockUseCase.buildMediaFileDataWithTemporaryId(
                                                mediaFile = mediafile,
                                                temporaryId = assignedTempId
                                        )
                                frame.id = storyMediaFileData.id
                                storyMediaFileDataList.add(storyMediaFileData)
                            }
                        }
                    }
                }
            }
        }
        return storyMediaFileDataList
    }

    private fun buildStoryMediaFileDataForTemporarySlide(frame: StoryFrameItem, tempId: String): StoryMediaFileData {
        return saveStoryGutenbergBlockUseCase.buildMediaFileDataWithTemporaryIdNoMediaFile(
                temporaryId = tempId,
                url = if (frame.source is FileBackgroundSource) {
                    (frame.source as FileBackgroundSource).file.toString()
                } else {
                    (frame.source as UriBackgroundSource).contentUri.toString()
                },
                isVideo = (frame.frameItemType is VIDEO)
        )
    }

    override fun onSubmitButtonClicked(publishPost: PublishPost) {
        viewModel.onSubmitButtonClicked()
    }

    override fun showPermissionPermanentlyDeniedDialog(permission: String) {
        WPPermissionUtils.showPermissionAlwaysDeniedDialog(this, permission)
    }

    override fun showGenericAnnouncementDialog() {
        if (intent.getBooleanExtra(KEY_LAUNCHED_FROM_GUTENBERG, false)) {
            if (!intent.getBooleanExtra(KEY_ALL_UNFLATTENED_LOADED_SLIDES, false)) {
                // not all slides in this Story could be unflattened so, show the warning informative dialog
                FrameSaveErrorDialog.newInstance(
                        title = getString(R.string.dialog_edit_story_limited_title),
                        message = getString(R.string.dialog_edit_story_limited_message)
                ).show(supportFragmentManager, FRAGMENT_ANNOUNCEMENT_DIALOG)
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryLoadEnd(event: StoryLoadEnd) {
        // once the Story has been loaded by the Composer, we should mark the composing session start as the
        // UI is ready to receive user's input
        viewModel.onStoryComposerStartAnalyticsSession()
    }
}
