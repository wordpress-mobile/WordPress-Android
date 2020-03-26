package org.wordpress.android.util.helpers.logfile

import android.content.Context
import java.io.File

/**
 * A collection of helpers for Log Files.
 */
class LogFileHelpers {
    companion object {
        private const val LOG_FILE_DIRECTORY = "logs"

        /**
         * Provides a {@link java.io.File} directory in which to store log files.
         *
         * If the directory doesn't already exist, it will be created.
         *
         * @param context: The application context
         */
        @JvmStatic
        fun logFileDirectory(context: Context): File {
            val logFileDirectory = File(context.applicationInfo.dataDir, LOG_FILE_DIRECTORY)

            if (!logFileDirectory.exists()) {
                logFileDirectory.mkdir()
            }

            return logFileDirectory
        }

        /**
         * Provides a list of stored log files, ordered oldest to newest.
         *
         * @param context: The application context
         */
        @JvmStatic
        fun logFiles(context: Context): List<File> {
            return logFileDirectory(context)
                    .listFiles()
                    .sortedBy { it.lastModified() }
        }
    }
}
