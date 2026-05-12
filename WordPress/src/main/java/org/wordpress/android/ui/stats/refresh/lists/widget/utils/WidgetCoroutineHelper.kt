package org.wordpress.android.ui.stats.refresh.lists.widget.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.wordpress.android.util.AppLog

/**
 * Runs [block] via [runBlocking] from inside a `RemoteViewsService.onDataSetChanged()`
 * call on a stats widget. Swallows [InterruptedException] / [CancellationException]
 * thrown when the widget host kills the service mid-fetch — the VM can still fall
 * through to its cached read and render whatever data is already on disk.
 *
 * Sentry: JETPACK-ANDROID-1ATH (and the same shape across all stats widget VMs:
 * 1AV2, 1AZW, 1AZQ, 1AWZ, 1B0V, 1C2Y).
 */
@Suppress("TooGenericExceptionCaught")
internal fun runBlockingForWidget(block: suspend () -> Unit) {
    try {
        runBlocking { block() }
    } catch (e: InterruptedException) {
        AppLog.w(AppLog.T.STATS, "Widget data fetch interrupted: ${e.message}")
    } catch (e: CancellationException) {
        AppLog.w(AppLog.T.STATS, "Widget data fetch cancelled: ${e.message}")
    }
}
