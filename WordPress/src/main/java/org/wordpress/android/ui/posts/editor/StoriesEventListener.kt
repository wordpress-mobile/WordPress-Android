package org.wordpress.android.ui.posts.editor

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AlertDialog.Builder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveCompleted
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveFailed
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveProgress
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveProcessStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryIndex
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_UPLOAD_MEDIA_RETRIED
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.editor.gutenberg.StorySaveMediaListener
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.media.StoryMediaSaveUploadBridge.StoryFrameMediaModelCreatedEvent
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.usecase.LoadStoryFromStoriesPrefsUseCase
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.StringUtils
import javax.inject.Inject

class StoriesEventListener @Inject constructor(
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore,
    private val eventBusWrapper: EventBusWrapper,
    private val editorMedia: EditorMedia,
    private val loadStoryFromStoriesPrefsUseCase: LoadStoryFromStoriesPrefsUseCase,
    private val storiesPrefs: StoriesPrefs,
    private val storyRepositoryWrapper: StoryRepositoryWrapper
) : DefaultLifecycleObserver {
    private lateinit var lifecycle: Lifecycle
    private lateinit var site: SiteModel
    private lateinit var editPostRepository: EditPostRepository
    private var storySaveMediaListener: StorySaveMediaListener? = null
    var storiesSavingInProgress = HashSet<StoryIndex>()
        private set

    data class StoryFrameMediaUploadedEvent(
        val localId: LocalId,
        val assignedMediaId: RemoteId,
        val oldUrl: String,
        val newUrl: String
    )

    fun startListening() {
        if (!eventBusWrapper.isRegistered(this)) {
            dispatcher.register(this)
            eventBusWrapper.register(this)
        }
    }

    fun pauseListening() {
        if (eventBusWrapper.isRegistered(this)) {
            dispatcher.unregister(this)
            eventBusWrapper.unregister(this)
        }
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle   .
     */
    override fun onDestroy(owner: LifecycleOwner) {
        lifecycle.removeObserver(this)
        pauseListening()
    }

    fun start(
        lifecycle: Lifecycle,
        site: SiteModel,
        editPostRepository: EditPostRepository,
        editorMediaListener: EditorMediaListener
    ) {
        this.site = site
        this.editPostRepository = editPostRepository
        this.lifecycle = lifecycle
        this.lifecycle.addObserver(this)
        this.editorMedia.start(site, editorMediaListener)
    }

    fun setSaveMediaListener(newListener: StorySaveMediaListener) {
        storySaveMediaListener = newListener
    }

    fun postStoryMediaUploadedEvent(mediaModel: MediaModel) {
        // if this is a Story media item, then make sure to keep up with the StoriesPrefs serialized slides
        // this looks for the slide saved with the local id key (media.getId()), and re-converts it to
        // mediaId.
        // Also: we don't need to worry about checking if this mediaModel corresponds to a media upload
        // within a story block in this post: we will only replace items for which a local-keyed frame has
        // been created before, which can only happen when using the Story Creator.
        storiesPrefs.replaceLocalMediaIdKeyedSlideWithRemoteMediaIdKeyedSlide(
            mediaModel.getId(),
            mediaModel.getMediaId(),
            mediaModel.localSiteId.toLong()
        )

        // also, let's post a sticky event for StoriesEventListener to catch - if the listener is not yet
        // registered, it will be listening to this event as needed when Gutenberg is re-attached and ready
        // to process events
        // see subscriber method fun onStoryFrameMediaUploadedEvent(event: StoryFrameMediaUploadedEvent) below
        eventBusWrapper.postSticky(
            StoryFrameMediaUploadedEvent(
                LocalId(mediaModel.id),
                RemoteId(mediaModel.mediaId),
                "",
                mediaModel.url
            )
        )
    }

    // Story Frame Save Service events
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveStart(event: FrameSaveStart) {
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val localMediaId = event.frameId.toString()
        val progress = storyRepositoryWrapper.getCurrentStorySaveProgress(event.storyIndex, 0.0f)
        storySaveMediaListener?.onMediaSaveReattached(localMediaId, progress)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveProgress(event: FrameSaveProgress) {
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val localMediaId = event.frameId.toString()
        val progress: Float = storyRepositoryWrapper.getCurrentStorySaveProgress(
            event.storyIndex,
            event.progress
        )
        storySaveMediaListener?.onMediaSaveProgress(localMediaId, progress)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveCompleted(event: FrameSaveCompleted) {
        eventBusWrapper.removeStickyEvent(event)
        // in onStoryFrameSaveCompleted we should always get a frameId that has the TEMPORARY_ID_PREFIX prefix
        // for Stories being edited only
        // otherwise we can exit
        if (event.frameId == null) return

        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val localMediaId = event.frameId

        val (frames) = storyRepositoryWrapper.getStoryAtIndex(event.storyIndex)

        // first, update the media's url
        val frame = frames[event.frameIndex]
        storySaveMediaListener?.onMediaSaveSucceeded(
            localMediaId,
            Uri.fromFile(frame.composedFrameFile).toString()
        )

        // now update progress
        val totalProgress: Float = storyRepositoryWrapper.getCurrentStorySaveProgress(
            event.storyIndex,
            0.0f
        )
        storySaveMediaListener?.onMediaSaveProgress(localMediaId, totalProgress)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStoryFrameMediaIdChanged(event: StoryFrameMediaModelCreatedEvent) {
        eventBusWrapper.removeStickyEvent(event)
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        storySaveMediaListener?.onMediaModelCreatedForFile(event.oldId, event.newId.toString(), event.oldUrl)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveFailed(event: FrameSaveFailed) {
        eventBusWrapper.removeStickyEvent(event)
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val localMediaId = event.frameId.toString()
        // just update progress, we may have still some other frames in this story that need be saved.
        // we will send the Failed signal once all the Story frames have been processed (see onStorySaveProcessFinished)
        val progress: Float = storyRepositoryWrapper.getCurrentStorySaveProgress(event.storyIndex, 0.0f)
        storySaveMediaListener?.onMediaSaveReattached(localMediaId, progress)
        // storySaveMediaListener?.onMediaSaveFailed(localMediaId)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveProcessFinished(event: StorySaveResult) {
        eventBusWrapper.removeStickyEvent(event)
        storiesSavingInProgress.remove(event.storyIndex)
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val story = storyRepositoryWrapper.getStoryAtIndex(event.storyIndex)
        if (!event.isRetry && event.frameSaveResult.size == story.frames.size) {
            // take the first frame IDs and mediaUri
            val localMediaId = story.frames[0].id.toString()
            storySaveMediaListener?.onStorySaveResult(localMediaId, event.isSuccess())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStorySaveStart(event: StorySaveProcessStart) {
        storiesSavingInProgress.add(event.storyIndex)
    }

    // Story media Upload specific event - we trigger this from within this class so it can be handled appropriately
    // and doesn't miss the target as regular FluxC upload events would if Gutenberg is not yet prepared
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStoryFrameMediaUploadedEvent(event: StoryFrameMediaUploadedEvent) {
        eventBusWrapper.removeStickyEvent(event)
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        storySaveMediaListener?.onStoryMediaSavedToRemote(
            event.localId.value.toString(),
            event.assignedMediaId.value.toString(),
            event.oldUrl,
            event.newUrl
        )
    }

    // Editor load / cancel events
    fun onRequestMediaFilesEditorLoad(
        activity: Activity,
        postId: LocalId,
        networkErrorOnLastMediaFetchAttempt: Boolean,
        mediaFiles: ArrayList<Any>,
        blockId: String
    ): Boolean {
        if (mediaFiles.isEmpty()) {
            ActivityLauncher.editEmptyStoryForResult(
                activity,
                site,
                postId,
                storyRepositoryWrapper.getCurrentStoryIndex(),
                blockId
            )
            return false
        }

        val reCreateStoryResult = loadStoryFromStoriesPrefsUseCase
            .loadStoryFromMemoryOrRecreateFromPrefs(site, mediaFiles)
        if (!reCreateStoryResult.noSlidesLoaded) {
            // Story instance loaded or re-created! Load it onto the StoryComposer for editing now
            ActivityLauncher.editStoryForResult(
                activity,
                site,
                postId,
                reCreateStoryResult.storyIndex,
                reCreateStoryResult.allStorySlidesAreEditable,
                true,
                blockId
            )
        } else {
            // unfortunately we couldn't even load the remote media Ids indicated by the StoryBlock so we can't allow
            // editing at this time :(
            if (networkErrorOnLastMediaFetchAttempt) {
                // there was an error fetching media when we were loading the editor,
                // we *may* still have a possibility, tell the user they may try refreshing the media again
                val builder: Builder = MaterialAlertDialogBuilder(
                    activity
                )
                builder.setTitle(activity.getString(R.string.dialog_edit_story_unavailable_title))
                builder.setMessage(activity.getString(R.string.dialog_edit_story_unavailable_message))
                builder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            } else {
                // unrecoverable error, nothing we can do, inform the user :(.
                val builder: Builder = MaterialAlertDialogBuilder(
                    activity
                )
                builder.setTitle(activity.getString(R.string.dialog_edit_story_unrecoverable_title))
                builder.setMessage(activity.getString(R.string.dialog_edit_story_unrecoverable_message))
                builder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ -> dialog.dismiss() }
                val dialog = builder.create()
                dialog.show()
            }
        }
        return reCreateStoryResult.noSlidesLoaded
    }

    @Suppress("UNCHECKED_CAST")
    fun onCancelUploadForMediaCollection(mediaFiles: ArrayList<Any>) {
        // just cancel upload for each media
        for (mediaFile in mediaFiles) {
            val localMediaId = StringUtils.stringToInt(
                (mediaFile as HashMap<String?, Any?>)["id"].toString(), 0
            )
            if (localMediaId != 0) {
                editorMedia.cancelMediaUploadAsync(localMediaId, false)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun onRetryUploadForMediaCollection(
        activity: Activity,
        mediaFiles: ArrayList<Any>,
        editorMediaUploadListener: EditorMediaUploadListener?
    ) {
        val mediaIdsToRetry = ArrayList<Int>()
        for (mediaFile in mediaFiles) {
            val localMediaId = StringUtils.stringToInt(
                (mediaFile as HashMap<String?, Any?>)["id"].toString(), 0
            )
            if (localMediaId != 0) {
                val media: MediaModel? = mediaStore.getMediaWithLocalId(localMediaId)
                // if we find at least one item in the mediaFiles collection passed
                // for which we don't have a local MediaModel, just tell the user and bail
                if (media == null) {
                    AppLog.e(
                        MEDIA,
                        "Can't find media with local id: $localMediaId"
                    )
                    val builder: Builder = MaterialAlertDialogBuilder(
                        activity
                    )
                    builder.setTitle(activity.getString(R.string.cannot_retry_deleted_media_item_fatal))
                    builder.setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    val dialog = builder.create()
                    dialog.show()
                    return
                }
                if (media.url != null && media.uploadState == UPLOADED.toString()) {
                    // Note: we should actually do this when the editor fragment starts instead of waiting for user
                    // input.
                    // Notify the editor fragment upload was successful and it should replace the local url by the
                    // remote url.
                    editorMediaUploadListener?.onMediaUploadSucceeded(
                        media.id.toString(),
                        FluxCUtils.mediaFileFromMediaModel(media)
                    )
                } else {
                    UploadService.cancelFinalNotification(
                        activity,
                        editPostRepository.getPost()
                    )
                    UploadService.cancelFinalNotificationForMedia(activity, site)
                    mediaIdsToRetry.add(localMediaId)
                }
            }
        }

        if (!mediaIdsToRetry.isEmpty()) {
            editorMedia.retryFailedMediaAsync(mediaIdsToRetry)
        }
        AnalyticsTracker.track(EDITOR_UPLOAD_MEDIA_RETRIED)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onCancelSaveForMediaCollection(mediaFiles: ArrayList<Any>) {
        // TODO implement cancelling save process for media collection
    }
}
