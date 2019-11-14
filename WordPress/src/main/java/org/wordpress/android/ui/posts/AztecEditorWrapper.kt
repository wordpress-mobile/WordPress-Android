package org.wordpress.android.ui.posts

import android.content.Context
import org.wordpress.android.editor.AztecEditorFragment
import javax.inject.Inject

class AztecEditorWrapper
@Inject constructor() {
    fun getMediaMarkedUploadingInPostContent(
        context: Context,
        newContent: String
    ) = AztecEditorFragment.getMediaMarkedUploadingInPostContent(
            context,
            newContent
    )
}
