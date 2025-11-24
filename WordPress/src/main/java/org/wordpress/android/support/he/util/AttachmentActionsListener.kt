package org.wordpress.android.support.he.util

import android.net.Uri

interface AttachmentActionsListener {
    fun onAddImageClick()
    fun onRemoveImage(uri: Uri)
}
