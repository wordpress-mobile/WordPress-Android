package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import java.io.File
import javax.inject.Inject
import javax.inject.Named

/**
 * Helper class for retrieving/creating MediaModel from the provided data.
 */
class GetMediaModelUseCase @Inject constructor(
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val mediaStore: MediaStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage = _toastMessage as LiveData<ToastMessageHolder>

    suspend fun loadMediaModelFromDb(mediaModelLocalIds: Iterable<Int>): List<MediaModel> {
        return withContext(bgDispatcher) {
            mediaModelLocalIds
                    .mapNotNull {
                        mediaStore.getMediaWithLocalId(it)
                    }
        }
    }

    suspend fun loadMediaModelFromDb(site: SiteModel, mediaModelsRemoteIds: Iterable<Long>): List<MediaModel> {
        return withContext(bgDispatcher) {
            mediaModelsRemoteIds
                    // TODO should we show a toast or log a message when getSiteMediaWithId returns null?
                    .mapNotNull {
                        mediaStore.getSiteMediaWithId(site, it)
                    }
        }
    }

    suspend fun createMediaModelFromUri(localSiteId: Int, uri: Uri): MediaModel? {
        return createMediaModelFromUri(localSiteId, listOf(uri)).firstOrNull()
    }

    // TODO consider moving toastMessages somewhere else
    suspend fun createMediaModelFromUri(localSiteId: Int, uris: List<Uri>): List<MediaModel> {
        return withContext(bgDispatcher) {
            uris.mapNotNull { uri ->
                when (val result = verifyFileExists(uri)) {
                    is FileExistsResult.Error -> {
                        // TODO FIx: this will show a toast for each error
                        _toastMessage.postValue(ToastMessageHolder(result.resId, result.msgDuration))
                        null
                    }
                    is FileExistsResult.Success -> createNewMediaModel(localSiteId, uri)
                }
            }
        }
    }

    private fun createNewMediaModel(
        localSiteId: Int,
        uri: Uri
    ): MediaModel? {
        val mimeType = mediaUtilsWrapper.getMimeType(uri)
        return fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, localSiteId)?.let { media ->
            setThumbnailIfAvailable(media, uri)
            media
        }
    }

    private fun setThumbnailIfAvailable(
        media: MediaModel,
        uri: Uri
    ) {
        if (mediaUtilsWrapper.isVideoMimeType(media.mimeType)) {
            media.thumbnailUrl = createVideoThumbnail(uri)
        }
    }

    private fun createVideoThumbnail(uri: Uri): String? {
        val path = mediaUtilsWrapper.getRealPathFromURI(uri)
        return path?.let { mediaUtilsWrapper.getVideoThumbnail(it) }
    }

    private fun verifyFileExists(uri: Uri): FileExistsResult {
        return mediaUtilsWrapper.getRealPathFromURI(uri)?.let { path ->
            val file = File(path)
            return if (file.exists()) {
                FileExistsResult.Success
            } else {
                FileExistsResult.Error(R.string.file_not_found, SHORT)
            }
        } ?: FileExistsResult.Error(R.string.editor_toast_invalid_path, SHORT)
    }

    sealed class FileExistsResult {
        data class Error(@StringRes val resId: Int, val msgDuration: Duration) : FileExistsResult()
        object Success : FileExistsResult()
    }
}
