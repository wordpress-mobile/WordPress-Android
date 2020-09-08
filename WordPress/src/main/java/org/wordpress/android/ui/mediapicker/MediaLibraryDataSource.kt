package org.wordpress.android.ui.mediapicker

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.RemoteId
import org.wordpress.android.ui.mediapicker.MediaLibraryDataSource.PartialResult.Data
import org.wordpress.android.ui.mediapicker.MediaLibraryDataSource.PartialResult.Loading
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MediaLibraryDataSource(
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteModel: SiteModel
) : MediaSource {
    init {
        dispatcher.register(this)
    }

    private var loadContinuations = mutableMapOf<MimeType.Type, Continuation<OnMediaListFetched>>()

    override suspend fun load(
        mediaTypes: Set<MediaType>,
        forced: Boolean,
        loadMore: Boolean
    ): MediaLoadingResult {
        return withContext(bgDispatcher) {
            val result = mutableListOf<MediaItem>()
            val deferredJobs = mediaTypes.map { mediaType ->
                when (mediaType) {
                    IMAGE -> async {
                        mediaStore.getSiteImages(siteModel).toMediaItems(mediaType)
                    }
                    VIDEO -> async {
                        mediaStore.getSiteVideos(siteModel).toMediaItems(mediaType)
                    }
                    AUDIO -> async {
                        mediaStore.getSiteAudio(siteModel).toMediaItems(mediaType)
                    }
                    DOCUMENT -> async {
                        mediaStore.getSiteDocuments(siteModel).toMediaItems(mediaType)
                    }
                }
            }
            deferredJobs.forEach { result.addAll(it.await()) }
            result.sortByDescending { (it.identifier as? RemoteId)?.value }
            MediaLoadingResult.Success(result, false)
        }
    }

    private fun List<MediaModel>.toMediaItems(mediaType: MediaType): List<MediaItem> {
        return this.map { mediaModel ->
            MediaItem(
                    RemoteId(mediaModel.mediaId),
                    mediaModel.url,
                    mediaModel.title,
                    mediaType,
                    mediaModel.mimeType,
                    0
            )
        }
    }

    private suspend fun loadMediaItems(mediaType: MediaType, forced: Boolean, loadMore: Boolean) = flow {
        emit(Loading)
        if (!forced) {
            emit(Data(getFromDatabase(mediaType)))
        }
        val loadingResult = loadPage(siteModel, loadMore, mediaType.toMimeType())
        if (loadingResult.isError) {
            emit(PartialResult.Error(loadingResult.error.message))
        } else {
            emit(Data(getFromDatabase(mediaType)))
        }
    }

    private fun getFromDatabase(mediaType: MediaType): List<MediaItem> {
        return when (mediaType) {
            IMAGE -> mediaStore.getSiteImages(siteModel)
            VIDEO -> mediaStore.getSiteVideos(siteModel)
            AUDIO -> mediaStore.getSiteAudio(siteModel)
            DOCUMENT -> mediaStore.getSiteDocuments(siteModel)
        }.toMediaItems(mediaType)
    }

    private suspend fun loadPage(siteModel: SiteModel, loadMore: Boolean, filter: MimeType.Type): OnMediaListFetched =
            suspendCoroutine { cont ->
                loadContinuations[filter] = cont
                val payload = FetchMediaListPayload(
                        siteModel,
                        NUM_MEDIA_PER_FETCH,
                        loadMore,
                        filter
                )
                dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
            }

    private fun MediaType.toMimeType(): MimeType.Type {
        return when (this) {
            IMAGE -> MimeType.Type.IMAGE
            VIDEO -> MimeType.Type.VIDEO
            AUDIO -> MimeType.Type.AUDIO
            DOCUMENT -> MimeType.Type.APPLICATION
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        loadContinuations[event.mimeType]?.resume(event)
        loadContinuations.remove(event.mimeType)
    }

    sealed class PartialResult {
        object Loading : PartialResult()
        data class Data(val data: List<MediaItem>) : PartialResult()
        data class Error(val message: String) : PartialResult()
    }

    companion object {
        const val NUM_MEDIA_PER_FETCH = 24
    }

    class MediaLibraryDataSourceFactory
    @Inject constructor(
        private val mediaStore: MediaStore,
        private val dispatcher: Dispatcher,
        @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
    ) {
        fun build(siteModel: SiteModel) = MediaLibraryDataSource(mediaStore, dispatcher, bgDispatcher, siteModel)
    }
}
