package org.wordpress.android.ui.stories.media

import org.wordpress.android.util.helpers.MediaFile

interface StoryEditorMediaSaveListener {
    fun onMediaSaveReattached(localId: String?, currentProgress: Float)
    fun onMediaSaveSucceeded(localId: String?, mediaFile: MediaFile?)
    fun onMediaSaveProgress(localId: String?, progress: Float)
    fun onMediaSaveFailed(localId: String?)
}
