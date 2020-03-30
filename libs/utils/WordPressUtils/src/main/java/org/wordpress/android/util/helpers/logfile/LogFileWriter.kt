package org.wordpress.android.util.helpers.logfile

import org.jetbrains.annotations.TestOnly
import java.io.File
import java.io.FileWriter
import java.util.Date
import org.wordpress.android.util.DateTimeUtils

/**
 * A class that manages writing to a log file.
 *
 * This class creates and writes to a log file, and will typically persist for the entire lifecycle
 * of its host application.
 */
class LogFileWriter @JvmOverloads constructor(
    logFileProvider: LogFileProvider,
    fileId: String = DateTimeUtils.iso8601FromDate(Date())
) {
    private val file = File(logFileProvider.getLogFileDirectory(), "$fileId.log")
    private val fileWriter: FileWriter = FileWriter(file)

    /**
     * A reference to the underlying {@link Java.IO.File} file.
     * Should only be used for testing.
     */
    @TestOnly
    fun getFile(): File = file

    /**
     * Writes the provided string to the log file synchronously
     */
    fun write(data: String) {
        fileWriter.write(data)
        fileWriter.flush()
    }
}
