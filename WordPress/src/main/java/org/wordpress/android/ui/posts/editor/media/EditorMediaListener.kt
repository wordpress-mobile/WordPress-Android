package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.ui.posts.EditPostActivity.OnPostUpdatedFromUIListener
import org.wordpress.android.util.helpers.MediaFile

interface EditorMediaListener {
    fun appendMediaFiles(mediaFiles: Map<String, MediaFile>)
    fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener? = null)
    fun advertiseImageOptimization(listener: () -> Unit)
    fun onMediaModelsCreatedFromOptimizedUris(oldUriToMediaFiles: Map<Uri, MediaModel>)
    fun getImmutablePost(): PostImmutableModel
}
