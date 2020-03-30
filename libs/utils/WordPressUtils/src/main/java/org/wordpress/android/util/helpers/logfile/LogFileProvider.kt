package org.wordpress.android.util.helpers.logfile

import java.io.File

/**
 * An interface to retrieve log files
 */
interface LogFileProvider {
    fun getLogFiles(): List<File>
}
