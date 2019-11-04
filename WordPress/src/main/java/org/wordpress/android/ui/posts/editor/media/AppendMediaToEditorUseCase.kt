package org.wordpress.android.ui.posts.editor.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.util.FluxCUtilsWrapper
import javax.inject.Inject

/**
 * Appends  media items to a content of a post. If the media item exists in the remote, its source is set to
 * MediaModel.url, if it's a local image its source is set to MediaModel.filePath.
 */
class AppendMediaToEditorUseCase @Inject constructor(private val fluxCUtilsWrapper: FluxCUtilsWrapper) {
    fun addMediaToEditor(
        editorMediaListener: EditorMediaListener,
        mediaModels: List<MediaModel>
    ) {
        mediaModels
                .mapNotNull { media ->
                    media.urlToUse?.let { urlToUse ->
                        fluxCUtilsWrapper.mediaFileFromMediaModel(media)?.let { mediaFile ->
                            Pair(urlToUse, mediaFile)
                        }
                    }
                }
                .forEach { pair -> editorMediaListener.appendMediaFile(pair.second, pair.first) }
        // TODO this should ideally call .toMap(ArrayMap()).let{editorMediaListener.appendMediaFiles(this)},
        //  but appendMediaFileS doesn't work as expected. Eg. failed media overlay doesn't get shown.
    }
}

private val MediaModel.urlToUse
    get() = if (url.isNullOrBlank()) filePath else url
