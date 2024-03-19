package org.wordpress.android.ui.posts.prepublishing.home.viewmodel.slice

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState
import org.wordpress.android.ui.posts.prepublishing.publishing.PublishingEvent
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.ProgressEvent
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.viewmodel.Event

class SyncPublishingUiModifier(private val dispatcher: Dispatcher)
    : UiModifier<List<PrepublishingHomeItemUiState>, Event<PrepublishingHomeItemUiState.ActionType.Action>>() {
    init {
        dispatcher.register(this)
        EventBus.getDefault().register(this)
    }

    private val lifecycleOwner = object : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = lifecycleRegistry
    }

    init {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    private val listOfMediaInProgress = ArrayList<String>()

    override fun onCleared() {
        super.onCleared()
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused", "SpreadOperator")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaChanged(event: MediaStore.OnMediaChanged) {
        Log.d(TAG, "onMediaChanged: media size ${event.mediaList.size}")
        Log.d(TAG, "onMediaChanged cause: ${event.cause}")
        Log.d(TAG, "onMediaChanged: upload state ${event.mediaList[0].uploadState}")
    }

    // T
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: PostStore.OnPostUploaded) {
        Log.d(TAG, "onPostUploaded id: ${event.post.id}")
        onPostUploadSuccess(event.post)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: MediaStore.OnMediaUploaded) {
        Log.d(TAG, "onMediaUploaded: ${event.progress}")
        Log.d(TAG, "canceled: ${event.canceled}")
        Log.d(TAG, "onMediaUploaded: ${event.completed}")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventBackgroundThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        Log.d(TAG, "UploadErrorEvent: $event")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventBackgroundThread(event: UploadService.UploadMediaSuccessEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        Log.d(TAG, "UploadMediaSuccessEvent: $event")
    }

    /**
     * Upload started, reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventBackgroundThread(event: PostEvents.PostUploadStarted) {
        Log.d(TAG, "PostUploadStarted: $event")
        onPostPublishingStarted()
    }

    private fun uploadStatusChanged(id: Int) {
        Log.d(TAG, "uploadStatusChanged: $id")
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventBackgroundThread(event: PostEvents.PostUploadCanceled) {
        Log.d(TAG, "PostUploadCanceled: $event")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    // only called in case when media is optimized
    fun onEventBackgroundThread(event: ProgressEvent) {
        Log.d(TAG, "ProgressEvent: $event")
        Log.d(TAG, "ProgressEvent: ${event.progress}")
        uploadStatusChanged(event.media.localPostId)
    }

    @Suppress("unused", "SpreadOperator")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventBackgroundThread(event: UploadService.UploadMediaRetryEvent) {
        Log.d(TAG, "UploadMediaRetryEvent: $event")
    }

    fun onMediaUploadInProgress(localMediaId: String, progress: Float) {
        if (progress == 100f) {
            listOfMediaInProgress.remove(localMediaId)
            return updatePublishingState(PublishingEvent.MediaUploadInProgress(listOfMediaInProgress))
        }
        if (!listOfMediaInProgress.contains(localMediaId)) {
            listOfMediaInProgress.add(localMediaId)
        }
        updatePublishingState(PublishingEvent.MediaUploadInProgress(listOfMediaInProgress))
    }

    fun onMediaUploadedSuccessfully(media: MediaModel) {
        if (listOfMediaInProgress.contains(media.id.toString())) {
            listOfMediaInProgress.remove(media.id.toString())
            if (listOfMediaInProgress.isEmpty()) {
                updatePublishingState(PublishingEvent.ReadyToUpload)
            } else updatePublishingState(
                PublishingEvent.MediaUploadInProgress(listOfMediaInProgress)
            )
        }
    }

    fun onMediaUploadError(media: MediaModel) {
        if (listOfMediaInProgress.contains(media.id.toString())) {
            listOfMediaInProgress.remove(media.id.toString())
            if (listOfMediaInProgress.isEmpty()) {
                updatePublishingState(PublishingEvent.ReadyToUpload)
            } else updatePublishingState(
                PublishingEvent.MediaUploadInProgress(listOfMediaInProgress)
            )
        }
    }

    fun onMediaRemoved() {
        // todo:
    }

    fun onPostUploadInProgress(post: PostImmutableModel) {
        Log.d(TAG, "onPostUploadInProgress")
        updatePublishingState(
            PublishingEvent.PostUploadInProgress(post)
        )
    }

    fun onPostUploadSuccess(post: PostModel) {
        Log.d(TAG, "onPostUploadSuccess")
        updatePublishingState(
            PublishingEvent.PostUploadSuccess(post)
        )
    }

    fun onPostPublishingStarted() {
        Log.d(TAG, "onPostPublishingStarted")
        updatePublishingState(
            PublishingEvent.PostUploadStarted(PostModel())
        )
    }

    fun onPostUploadError() {
        Log.d(TAG, "onPostUploadError")
        updatePublishingState(
            PublishingEvent.PostUploadError(PostModel())
        )
    }

    private fun updatePublishingState(publishingEvent: PublishingEvent) {
        Log.d(javaClass.simpleName, "***=> updatePublishingState: $publishingEvent")
        val newButtonState = when (publishingEvent) {
            is PublishingEvent.MediaUploadInProgress -> PrepublishingHomeItemUiState.ButtonUiState.InProgressButtonUiState(
                null
            )
            is PublishingEvent.PostUploadError -> PrepublishingHomeItemUiState.ButtonUiState.ErrorButtonUiState(null)
            is PublishingEvent.PostUploadInProgress -> PrepublishingHomeItemUiState.ButtonUiState.InProgressButtonUiState(
                null
            )
            is PublishingEvent.PostUploadStarted -> PrepublishingHomeItemUiState.ButtonUiState.InProgressButtonUiState(
                null
            )
            is PublishingEvent.PostUploadSuccess -> PrepublishingHomeItemUiState.ButtonUiState.DoneButtonUiState {
                Log.d(TAG, "Done button clicked")
                postEvent(Event(PrepublishingHomeItemUiState.ActionType.Action.Close))
            }
            is PublishingEvent.ReadyToUpload -> {
                PrepublishingHomeItemUiState.ButtonUiState.InProgressButtonUiState(null)
            }
        }

        val updatedUiState = uiState?.value?.map { item ->
            if (item is PrepublishingHomeItemUiState.ButtonUiState) {
                // update button state
                newButtonState
            } else {
                item
            }
        }

        // Post the updated state
        updatedUiState?.let { updatedValue ->
            updateUiState(updatedValue)
        }
    }

    companion object {
        const val TAG = "SyncPublishingViewModelSlice"
    }
}
