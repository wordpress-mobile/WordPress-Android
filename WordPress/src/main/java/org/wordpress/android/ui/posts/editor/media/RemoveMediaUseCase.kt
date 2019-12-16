package org.wordpress.android.ui.posts.editor.media

import android.text.TextUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.StringUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class RemoveMediaUseCase @Inject constructor(
    private val mediaStore: MediaStore,
    private val dispatcher: Dispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun removeMediaIfNotUploading(mediaIds: List<String>) = withContext(bgDispatcher) {
        for (mediaId in mediaIds) {
            if (!TextUtils.isEmpty(mediaId)) {
                // make sure the MediaModel exists
                val mediaModel = mediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId))
                        ?: continue

                // also make sure it's not being uploaded anywhere else (maybe on some other Post,
                // simultaneously)
                if (mediaModel.uploadState != null
                        && MediaUtils.isLocalFile(mediaModel.uploadState.toLowerCase(Locale.ROOT))
                        && !UploadService.isPendingOrInProgressMediaUpload(mediaModel)) {
                    dispatcher.dispatch(MediaActionBuilder.newRemoveMediaAction(mediaModel))
                }
            }
        }
    }
}