package org.wordpress.android.util.encryption

import android.content.Context
import org.wordpress.android.util.AppLog

class LogsFileProvider {

    fun getMostRecentLogs(context: Context): List<String> {
        // TODO: Implement a specific method to retrieve logs from a file
        return AppLog.toHtmlList(context)
    }
}