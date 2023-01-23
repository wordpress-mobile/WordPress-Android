package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.FluxCUtilsWrapper
import javax.inject.Inject

/**
 * Appends  media items to a content of a post. If the media item exists in the remote, its source is set to
 * MediaModel.url, if it's a local image its source is set to MediaModel.filePath.
 */
@Reusable
class AppendMediaToEditorUseCase @Inject constructor(private val fluxCUtilsWrapper: FluxCUtilsWrapper) {
    fun addMediaToEditor(
        editorMediaListener: EditorMediaListener,
        mediaModels: List<MediaModel>
    ) {
        mediaModels
            .mapNotNull { media ->
                media.urlToUse?.let { urlToUse ->
                    val mediaFile = fluxCUtilsWrapper.mediaFileFromMediaModel(media)
                    if (mediaFile == null) {
                        AppLog.e(
                            AppLog.T.MEDIA, "Media with remote id ${media.mediaId} not " +
                                    "added to editor."
                        )
                    }
                    mediaFile?.let { Pair(urlToUse, it) }
                }
            }
            .toMap()
            .let { mediaList -> editorMediaListener.appendMediaFiles(mediaList) }
    }
}

private val MediaModel.urlToUse
    get() = if (url.isNullOrBlank()) filePath else url
