package org.wordpress.android.ui.posts.prepublishing.publishing

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.uploads.PostEvents
import org.wordpress.android.ui.uploads.ProgressEvent
import org.wordpress.android.ui.uploads.UploadActionUseCase
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase
import javax.inject.Inject
import javax.inject.Named

class PublishingViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val uploadUiStateUseCase: PostModelUploadUiStateUseCase,
    uploadActionUseCase: UploadActionUseCase,
    uploadStore: UploadStore
) : ScopedViewModel(bgDispatcher) {

    private val _uiState = MutableLiveData<PublishingEvent>()
    val uiState: LiveData<PublishingEvent> = _uiState

    private val _uiStateFlow = MutableStateFlow<PublishingEvent>(PublishingEvent.ReadyToUpload)
    val uiStateFlow: MutableStateFlow<PublishingEvent> = _uiStateFlow

    init {
        dispatcher.register(this)
        EventBus.getDefault().register(this)
    }

    /**
     * Handles the [Lifecycle.Event.ON_DESTROY] event to cleanup the registration for dispatcher and removing the
     * observer for lifecycle.
     */

    private val uploadStatusTracker = PostModelUploadStatusTracker(
        uploadStore = uploadStore,
        uploadActionUseCase = uploadActionUseCase
    )

    private val listOfMediaInProgress = ArrayList<String>()

    private val lifecycleOwner = object : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = lifecycleRegistry
    }

    init {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused", "SpreadOperator")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaChanged(event: MediaStore.OnMediaChanged) {
        Log.e("PublishingViewModel", "onMediaChanged: media size ${event.mediaList.size}")
        Log.e("PublishingViewModel", "onMediaChanged cause: ${event.cause}")
        Log.e("PublishingViewModel", "onMediaChanged: upload state ${event.mediaList[0].uploadState}")
    }

    // T
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPostUploaded(event: PostStore.OnPostUploaded) {
        Log.e("PublishingViewModel", "onPostUploaded id: ${event.post.id}")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMediaUploaded(event: MediaStore.OnMediaUploaded) {
        Log.e("PublishingViewModel", "onMediaUploaded: ${event.progress}")
        Log.e("PublishingViewModel", "canceled: ${event.canceled}")
        Log.e("PublishingViewModel", "onMediaUploaded: ${event.completed}")
    }

    // EventBus Events

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThread(event: UploadService.UploadErrorEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        Log.e("PublishingViewModel", "UploadErrorEvent: $event")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThread(event: UploadService.UploadMediaSuccessEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        Log.e("PublishingViewModel", "UploadMediaSuccessEvent: $event")
    }

    /**
     * Upload started, reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThread(event: PostEvents.PostUploadStarted) {
        Log.e("PublishingViewModel", "PostUploadStarted: $event")
    }

    private fun uploadStatusChanged(id: Int) {
        Log.e("PublishingViewModel", "uploadStatusChanged: $id")
    }

    /**
     * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThread(event: PostEvents.PostUploadCanceled) {
        Log.e("PublishingViewModel", "PostUploadCanceled: $event")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    // only called in case when media is optimized
    fun onEventBackgroundThread(event: ProgressEvent) {
        Log.e("PublishingViewModel", "ProgressEvent: $event")
        Log.e("PublishingViewModel", "ProgressEvent: ${event.progress}")
        uploadStatusChanged(event.media.localPostId)

    }

    @Suppress("unused", "SpreadOperator")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventBackgroundThread(event: UploadService.UploadMediaRetryEvent) {
        Log.e("PublishingViewModel", "UploadMediaRetryEvent: $event")
    }

    fun onMediaUploadInProgress(localMediaId: String, progress: Float) {
        if (progress == 100f) {
            listOfMediaInProgress.remove(localMediaId)

            return emitPublishingEvent(PublishingEvent.MediaUploadInProgress(listOfMediaInProgress))
//            return _uiState.postValue(
//                PublishingEvent.MediaUploadInProgress(listOfMediaInProgress)
//            )
        }
        if (!listOfMediaInProgress.contains(localMediaId)) {
            listOfMediaInProgress.add(localMediaId)
        }
//        _uiState.postValue(
//            PublishingEvent.MediaUploadInProgress(listOfMediaInProgress)
//        )
        emitPublishingEvent(PublishingEvent.MediaUploadInProgress(listOfMediaInProgress))
    }

    fun onMediaUploadedSuccessfully(media: MediaModel) {
        if (listOfMediaInProgress.contains(media.id.toString())) {
            listOfMediaInProgress.remove(media.id.toString())
            if (listOfMediaInProgress.isEmpty()) {
                emitPublishingEvent(PublishingEvent.ReadyToUpload)
//                _uiState.postValue(
//                    PublishingEvent.ReadyToUpload
//                )
            } else emitPublishingEvent(
                    PublishingEvent.MediaUploadInProgress(listOfMediaInProgress))
                //                _uiState.postValue(
//                    PublishingEvent.MediaUploadInProgress(listOfMediaInProgress)
//                )
        }
    }

    fun onMediaUploadError(media: MediaModel) {
        if (listOfMediaInProgress.contains(media.id.toString())) {
            listOfMediaInProgress.remove(media.id.toString())
            if (listOfMediaInProgress.isEmpty()) {
//                _uiState.postValue(
//                    PublishingEvent.ReadyToUpload
//                )
                emitPublishingEvent(PublishingEvent.ReadyToUpload)
            } else emitPublishingEvent(
                    PublishingEvent.MediaUploadInProgress(listOfMediaInProgress))
//                _uiState.postValue(
//                    PublishingEvent.MediaUploadInProgress(listOfMediaInProgress)
//                )
        }
    }

    fun onMediaRemoved() {
        // todo:
    }

    fun onPostUploadInProgress(post: PostImmutableModel) {
        Log.e("PublishingViewModel", "onPostUploadInProgress")
        emitPublishingEvent(
            PublishingEvent.PostUploadInProgress(post)
        )
//        _uiState.postValue(
//            PublishingEvent.PostUploadInProgress(post)
//        )
    }

    fun onPostUploadSuccess(post: PostModel) {
        Log.e("PublishingViewModel", "onPostUploadSuccess")
        emitPublishingEvent(
            PublishingEvent.PostUploadSuccess(post))
//        _uiState.postValue(
//            PublishingEvent.PostUploadSuccess(post)
//        )
    }

    fun onPostPublishingStarted() {
        Log.e("PublishingViewModel", "onPostPublishingStarted")
        emitPublishingEvent(
            PublishingEvent.PostUploadStarted(PostModel())
        )
//        _uiState.postValue(
//            PublishingEvent.PostUploadStarted(PostModel())
//        )
    }

    fun onPostUploadError() {
        Log.e("PublishingViewModel", "onPostUploadError")
        emitPublishingEvent(
            PublishingEvent.PostUploadError(PostModel())
        )
//        _uiState.postValue(
//            PublishingEvent.PostUploadError(PostModel())
//        )
    }

    private fun emitPublishingEvent(event: PublishingEvent) {
        launch {
            withContext(Dispatchers.Main) {
                _uiStateFlow.emit(event)
            }
        }
    }
}

sealed class PublishingEvent {
    data class PostUploadStarted(val post: PostModel) : PublishingEvent()
    data class PostUploadError(val post: PostModel) : PublishingEvent()
    data class PostUploadSuccess(val post: PostModel) : PublishingEvent()
    data class PostUploadInProgress(val post: PostImmutableModel) : PublishingEvent()
    data class MediaUploadInProgress(val listOfMedia: ArrayList<String>) : PublishingEvent()
    data object ReadyToUpload : PublishingEvent()
}







