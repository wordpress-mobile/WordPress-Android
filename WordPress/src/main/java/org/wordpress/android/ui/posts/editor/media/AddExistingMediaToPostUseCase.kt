package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.EditorTracker
import javax.inject.Inject

enum class AddExistingMediaSource {
    WP_MEDIA_LIBRARY,
    STOCK_PHOTO_LIBRARY
}

/**
 * Loads existing media items (they must have a valid url) from the local db and adds them to the editor.
 */
@Reusable
class AddExistingMediaToPostUseCase @Inject constructor(
    private val editorTracker: EditorTracker,
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val appendMediaToEditorUseCase: AppendMediaToEditorUseCase
) {
    suspend fun addMediaExistingInRemoteToEditorAsync(
        site: SiteModel,
        source: AddExistingMediaSource,
        mediaIdList: List<Long>,
        editorMediaListener: EditorMediaListener
    ) {
        getMediaModelUseCase
                .loadMediaByRemoteId(site, mediaIdList)
                .onEach { media ->
                    editorTracker.trackAddMediaEvent(site, source, media.isVideo)
                }
                .let {
                    appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, it)
                    editorMediaListener.syncPostObjectWithUiAndSaveIt()
                }
    }
}
