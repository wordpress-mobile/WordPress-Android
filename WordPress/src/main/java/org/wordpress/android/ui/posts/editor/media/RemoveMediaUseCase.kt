package org.wordpress.android.ui.posts.editor.media

import android.text.TextUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.MediaUtilsWrapper
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class RemoveMediaUseCase @Inject constructor(
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher,
    private val mediaUtils: MediaUtilsWrapper,
    private val uploadService: UploadServiceFacade,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    @Suppress("LoopWithTooManyJumpStatements")
    suspend fun removeMediaIfNotUploading(mediaIds: List<String>) = withContext(bgDispatcher) {
        for (mediaId in mediaIds) {
            if (!TextUtils.isEmpty(mediaId)) {
                // make sure the MediaModel exists
                val mediaModel = try {
                    mediaStore.getMediaWithLocalId(Integer.valueOf(mediaId)) ?: continue
                } catch (e: NumberFormatException) {
                    AppLog.e(AppLog.T.MEDIA, "Invalid media id: $mediaId")
                    continue
                }

                // also make sure it's not being uploaded anywhere else (maybe on some other Post,
                // simultaneously)
                if (mediaModel.uploadState != null &&
                    mediaUtils.isLocalFile(mediaModel.uploadState.lowercase(Locale.ROOT)) &&
                    !uploadService.isPendingOrInProgressMediaUpload(mediaModel)
                ) {
                    dispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel))
                }
            }
        }
    }
}
