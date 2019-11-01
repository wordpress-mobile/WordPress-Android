package org.wordpress.android.ui.posts.editor.media

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.EditorMedia.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.ui.posts.editor.EditorTracker
import javax.inject.Inject

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
                .loadMediaModelFromDb(site, mediaIdList)
                .onEach { media ->
                    editorTracker.trackAddMediaEvent(site, source, media.isVideo)
                }
                .let {
                    appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, it)
                    editorMediaListener.savePostAsyncFromEditorMedia()
                }
    }
}
