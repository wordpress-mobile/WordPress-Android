package org.wordpress.android.util.helpers.logfile

/**
 * Prunes the Log File Store by retaining only the last `maxLogFileCount` log files.
 *
 * The file list is created upon instantiation – any files added
 * afterwards won't be modified.
 *
 * @param logFileProvider: An interface where the log files will be retrieved from
 * @param maxLogFileCount: The number of log files to retain
 */
class LogFileCleaner(private val logFileProvider: LogFileProviderInterface, private val maxLogFileCount: Int) {
    /**
     * Immediately removes all log files known to exist by this instance except for
     * the most recent `maxLogFileCount` items.
     */
    fun clean() {
        logFileProvider.getLogFiles()
                .dropLast(maxLogFileCount)
                .forEach { it.delete() }
    }
}
