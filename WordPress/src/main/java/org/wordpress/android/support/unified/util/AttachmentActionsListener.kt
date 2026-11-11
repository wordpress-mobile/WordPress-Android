package org.wordpress.android.support.unified.util

import android.net.Uri

interface AttachmentActionsListener {
    fun onAddImageClick()
    fun onRemoveImage(uri: Uri)
}
