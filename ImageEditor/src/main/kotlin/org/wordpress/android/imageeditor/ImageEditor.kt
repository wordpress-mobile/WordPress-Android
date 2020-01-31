package org.wordpress.android.imageeditor

import android.net.Uri
import android.util.Log
import java.io.File
import java.io.Serializable

class ImageEditor(private val loadImageIntoAFile: (suspend (String) -> File?)) : Serializable {
    suspend fun load(mediaUrl: String) {
        val highResImageFile: File? = loadImageIntoAFile.invoke(mediaUrl)
        Log.d("ImageEditor", "Image loaded into file")

        val fileUri: Uri = Uri.fromFile(highResImageFile)
    }

    companion object {
        lateinit var dummyImageEditorSingleton: ImageEditor
    }
}
