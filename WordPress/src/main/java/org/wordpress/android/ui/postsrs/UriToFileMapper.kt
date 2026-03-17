package org.wordpress.android.ui.postsrs

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class UriToFileMapper @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    @Suppress("Recycle") // stream is closed by .use {}
    fun copyUriToTempFile(uri: Uri): File {
        val mime = appContext.contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime)
            ?.let { ".$it" }
            ?: ".jpg"
        val tempFile = File.createTempFile(
            "featured_img_",
            ext,
            appContext.cacheDir
        )
        val inputStream = appContext.contentResolver
            .openInputStream(uri)
            ?: throw IOException("Cannot read URI")
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
