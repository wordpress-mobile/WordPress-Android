package org.wordpress.android.util

import android.content.Context
import org.wordpress.android.util.helpers.logfile.LogFileProvider
import javax.inject.Inject

class LogFileProviderWrapper @Inject constructor(context: Context) {
    private val logFileProvider = LogFileProvider.fromContext(context)

    fun getLogFilesDirectory() = logFileProvider.getLogFileDirectory()

    fun getLogFiles() = logFileProvider.getLogFiles()
}
