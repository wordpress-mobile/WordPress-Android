package org.wordpress.android.util.helpers.logfile

import android.content.Context
import java.io.File

private const val LOG_FILE_DIRECTORY = "logs"

/**
 * A collection of helpers for Log Files.
 */
class LogFileProvider(private val logFileDirectoryPath: String) : LogFileProviderInterface {
    /**
     * Provides a {@link java.io.File} directory in which to store log files.
     *
     * If the directory doesn't already exist, it will be created.
     */
    override fun getLogFileDirectory(): File {
        val logFileDirectory = File(logFileDirectoryPath, LOG_FILE_DIRECTORY)

        if (!logFileDirectory.exists()) {
            logFileDirectory.mkdir()
        }

        return logFileDirectory
    }

    /**
     * Provides a list of stored log files, ordered oldest to newest.
     */
    override fun getLogFiles(): List<File> {
        return getLogFileDirectory()
                .listFiles()
                .sortedBy { it.lastModified() }
    }

    companion object {
        @JvmStatic
        fun fromContext(context: Context): LogFileProvider {
            return LogFileProvider(context.applicationInfo.dataDir)
        }
    }
}
