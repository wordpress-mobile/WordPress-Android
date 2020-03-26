package org.wordpress.android.util.helpers.logfile

import android.content.Context
import java.io.File

/**
 * Prunes the Log File Store by retaining only the last `maxLogFileCount` log files.
 */
class LogFileCleaner {
    private val maxLogFileCount: Int
    private val logFiles: List<File>

    /**
     * Create a new LogFileCleaner.
     *
     * The file list is created upon instantiation – any files added
     * afterwards won't be modified.
     *
     * @param context: The application context
     * @param maxLogFileCount: The number of log files to retain
     */
    constructor(context: Context, maxLogFileCount: Int) {
        this.maxLogFileCount = maxLogFileCount
        this.logFiles = LogFileHelpers.logFiles(context)
    }

    /**
     * Immediately removes all log files known to exist by this instance except for
     * the most recent `maxLogFileCount` items.
     */
    fun clean() {
        logFiles
                .dropLast(maxLogFileCount)
                .forEach { it.delete() }
    }
}
