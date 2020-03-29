package org.wordpress.android.viewmodel.gif

import android.content.Context
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.WPMediaUtils
import javax.inject.Inject

/**
 * Downloads [GifMediaViewModel.largeImageUri] objects and saves them as [MediaModel]
 *
 * The download happens concurrently and primarily uses [Dispatchers.IO]. This means that we are limited by the number
 * of threads backed by that `CoroutineDispatcher`. In the future, we should probably add a limit to the number of
 * GIFs that users can select to reduce the likelihood of OOM exceptions.
 */
class GifMediaFetcher @Inject constructor(
    private val context: Context,
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher
) {
    /**
     * Inherits the [CoroutineScope] from the call site and traps all exceptions from launched coroutines inside this
     * [coroutineScope]. All coroutines executed under this [coroutineScope] will be cancelled if one of them throws
     * an exception.
     *
     * There is no need to log the [Exception] thrown by this method because the underlying methods already do that.
     */
    @Throws
    suspend fun fetchAndSave(
        gifMediaViewModels: List<GifMediaViewModel>,
        site: SiteModel
    ): List<MediaModel> = coroutineScope {
        // Execute [fetchAndSave] for all gifMediaViewModels first so that they are queued and executed in the
        // background. We'll call `await()` once they are queued.
        return@coroutineScope gifMediaViewModels.map {
            fetchAndSave(scope = this, gifMediaViewModel = it, site = site)
        }.map { it.await() }
    }

    private fun fetchAndSave(
        scope: CoroutineScope,
        gifMediaViewModel: GifMediaViewModel,
        site: SiteModel
    ): Deferred<MediaModel> = scope.async(Dispatchers.IO) {
        val uri = gifMediaViewModel.largeImageUri
        // No need to log the Exception here. The underlying method that is used, [MediaUtils.downloadExternalMedia]
        // already logs any errors.
        val downloadedUri = WPMediaUtils.fetchMedia(context, uri) ?: throw Exception("Failed to download the image.")

        // Exit if the parent coroutine has already been cancelled
        yield()

        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)

        val mediaModel = FluxCUtils.mediaModelFromLocalUri(context, downloadedUri, mimeType, mediaStore, site.id)
        mediaModel.title = gifMediaViewModel.title
        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel))

        return@async mediaModel
    }
}
