package org.wordpress.android.ui.posts.editor.media

import android.text.TextUtils
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_UPLOAD_MEDIA_FAILED
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject
import javax.inject.Named

@Reusable
class HandleMediaUploadErrorUseCase @Inject constructor(
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun onMediaUploadError(editorMediaListener: EditorMediaListener, media: MediaModel, error: MediaError) {
        withContext(bgDispatcher) {
            val localMediaId = media.id.toString()
            val mf = fluxCUtilsWrapper.mediaFileFromMediaModel(media)?.let {
                trackMediaUploadError(it, error)
                it
            }
            val mimeType = EditorFragmentAbstract.getEditorMimeType(mf)
            withContext(mainDispatcher) {
                editorMediaListener.onMediaUploadFailed(localMediaId, mimeType)
            }
        }
    }

    private fun trackMediaUploadError(it: MediaFile, error: MediaError) {
        val properties: MutableMap<String, Any?> =
                analyticsUtilsWrapper.getMediaProperties(it.isVideo, null, it.filePath)
        properties["error_type"] = error.type.name
        analyticsTrackerWrapper.track(EDITOR_UPLOAD_MEDIA_FAILED, properties)
    }
}
