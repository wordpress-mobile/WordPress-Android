package org.wordpress.android.util.extensions

import android.webkit.MimeTypeMap
import com.google.common.io.Files
import java.io.File

fun File.mimeType(): String? {
    @Suppress("UnstableApiUsage") val extension = Files.getFileExtension(this.name)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

