package org.wordpress.android.ui.posts.editor

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImageEditorFileUtils
@Inject constructor() {
    suspend fun deleteFilesOlderThanDurationFromDirectory(directoryPath: String, duration: Long) = withContext(IO) {
        val directory = File(directoryPath)
        if (directory.exists()) {
            directory.listFiles()?.let { files ->
                for (file in files) {
                    val ageMS: Long = System.currentTimeMillis() - file.lastModified()
                    if (ageMS >= duration) {
                        file.delete()
                    }
                }
            }
        }
    }
}
