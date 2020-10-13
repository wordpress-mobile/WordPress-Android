package org.wordpress.android.ui.posts.editor

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveCompleted
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveFailed
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveProgress
import com.wordpress.stories.compose.frame.StorySaveEvents.FrameSaveStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.editor.gutenberg.StorySaveMediaListener
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.stories.SaveStoryGutenbergBlockUseCase.Companion.TEMPORARY_ID_PREFIX
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.media.StoryMediaSaveUploadBridge.StoryFrameMediaModelCreatedEvent
import org.wordpress.android.ui.stories.prefs.StoriesPrefs
import org.wordpress.android.ui.stories.prefs.StoriesPrefs.TempId
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject

class StoriesEventListener @Inject constructor(
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore,
    private val storyRepositoryWrapper: StoryRepositoryWrapper
) : LifecycleObserver {
    private lateinit var lifecycle: Lifecycle
    private lateinit var site: SiteModel
    private var storySaveMediaListener: StorySaveMediaListener? = null
    @Inject lateinit var storiesPrefs: StoriesPrefs

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        dispatcher.register(this)
        EventBus.getDefault().register(this)
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        lifecycle.removeObserver(this)
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    fun start(lifecycle: Lifecycle, site: SiteModel) {
        this.site = site
        this.lifecycle = lifecycle
        this.lifecycle.addObserver(this)
    }

    fun setSaveMediaListener(newListener: StorySaveMediaListener) {
        storySaveMediaListener = newListener
    }

    // Story Frame Save Service events
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveStart(event: FrameSaveStart) {
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val localMediaId = event.frameId.toString()
        val progress: Float = storyRepositoryWrapper.getCurrentStorySaveProgress(event.storyIndex, 0.0f)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveCompleted(event: FrameSaveCompleted) {
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val localMediaId = requireNotNull(event.frameId)

        // check whether this is a temporary file being just saved (so we don't have a proper local MediaModel yet)
        // catch ( NumberFormatException e)
        if (localMediaId.startsWith(TEMPORARY_ID_PREFIX)) {
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
        } else {
            val mediaModel: MediaModel = mediaStore.getSiteMediaWithId(site, localMediaId.toLong())
            if (mediaModel != null) {
                val mediaFile: MediaFile = FluxCUtils.mediaFileFromMediaModel(mediaModel)
                storySaveMediaListener?.onMediaSaveSucceeded(localMediaId, mediaFile.getFileURL())
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryFrameMediaIdChanged(event: StoryFrameMediaModelCreatedEvent) {
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        storySaveMediaListener?.onMediaModelCreatedForFile(event.oldId, event.newId.toString(), event.oldUrl)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStoryFrameSaveFailed(event: FrameSaveFailed) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStorySaveProcessFinished(event: StorySaveResult) {
        if (!lifecycle.currentState.isAtLeast(CREATED)) {
            return
        }
        val story = storyRepositoryWrapper.getStoryAtIndex(event.storyIndex)
        if (event.frameSaveResult.size == story.frames.size) {
            // take the first frame IDs and mediaUri
            val localMediaId = story.frames[0].id.toString()
            storySaveMediaListener?.onStorySaveResult(localMediaId, event.isSuccess())
        }
    }
}
