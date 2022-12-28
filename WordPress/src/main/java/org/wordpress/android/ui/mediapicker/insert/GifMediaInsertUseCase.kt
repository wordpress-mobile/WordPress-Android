package org.wordpress.android.ui.mediapicker.insert

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.GifMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalId
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MimeTypeMapUtilsWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
class GifMediaInsertUseCase(
    private val context: Context,
    private val site: SiteModel,
    private val dispatcher: Dispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val mimeTypeMapUtilsWrapper: MimeTypeMapUtilsWrapper
) : MediaInsertUseCase {
    override val actionTitle: Int
        get() = R.string.media_uploading_gif_library_photo

    @Suppress("SwallowedException")
    override suspend fun insert(identifiers: List<Identifier>) = flow {
        emit(InsertModel.Progress(actionTitle))
        emit(coroutineScope {
            return@coroutineScope try {
                val mediaIdentifiers = identifiers.mapNotNull { identifier ->
                    (identifier as? GifMediaIdentifier)?.let {
                        fetchAndSaveAsync(this, it, site)
                    }
                }

                InsertModel.Success(mediaIdentifiers.awaitAll().filterNotNull())
            } catch (e: CancellationException) {
                InsertModel.Success(listOf<GifMediaIdentifier>())
            } catch (e: Exception) {
                InsertModel.Error(context.getString(R.string.error_downloading_image))
            }
        }
        )
    }

    private fun fetchAndSaveAsync(
        scope: CoroutineScope,
        gifIdentifier: GifMediaIdentifier,
        site: SiteModel
    ): Deferred<LocalId?> = scope.async(ioDispatcher) {
        return@async gifIdentifier.largeImageUri.let { mediaUri ->
            // No need to log the Exception here. The underlying method that is used, [MediaUtils.downloadExternalMedia]
            // already logs any errors.
            val downloadedUri = wpMediaUtilsWrapper.fetchMediaToUriWrapper(mediaUri)
                ?: throw Exception("Failed to download the image.")

            // Exit if the parent coroutine has already been cancelled
            yield()

            val fileExtension = mimeTypeMapUtilsWrapper.getFileExtensionFromUrl(mediaUri.toString())
            val mimeType = mimeTypeMapUtilsWrapper.getSingleton().getMimeTypeFromExtension(fileExtension)

            val mediaModel = fluxCUtilsWrapper.mediaModelFromLocalUri(downloadedUri.uri, mimeType, site.id)
            mediaModel?.title = gifIdentifier.title
            dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel))

            mediaModel?.id?.let { LocalId(it) }
        }
    }

    class GifMediaInsertUseCaseFactory
    @Inject constructor(
        private val context: Context,
        private val dispatcher: Dispatcher,
        @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
        private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
        private val fluxCUtilsWrapper: FluxCUtilsWrapper,
        private val mimeTypeMapUtilsWrapper: MimeTypeMapUtilsWrapper
    ) {
        fun build(site: SiteModel): GifMediaInsertUseCase {
            return GifMediaInsertUseCase(
                context,
                site,
                dispatcher,
                ioDispatcher,
                wpMediaUtilsWrapper,
                fluxCUtilsWrapper,
                mimeTypeMapUtilsWrapper
            )
        }
    }
}
