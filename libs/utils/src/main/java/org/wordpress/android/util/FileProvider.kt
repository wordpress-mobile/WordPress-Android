package org.wordpress.android.util

import dagger.Reusable
import java.io.File
import javax.inject.Inject

@Reusable
class FileProvider @Inject constructor() {
    fun createFile(path: String) = File(path)
}
