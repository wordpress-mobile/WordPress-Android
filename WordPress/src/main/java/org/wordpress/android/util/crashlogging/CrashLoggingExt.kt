package org.wordpress.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLogging
import org.wordpress.android.util.AppLog

fun CrashLogging.sendReportWithTag(exception: Throwable, tag: AppLog.T) {
    sendReport(exception = exception, tags = mapOf("tag" to tag.name))
}
