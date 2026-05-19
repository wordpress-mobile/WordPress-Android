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
 * Only safe because this is a root [runBlocking] on the widget worker thread with no
 * parent [kotlinx.coroutines.Job] above it — there is nothing upstream that needs to
 * observe the cancellation. Do NOT call this from inside an existing coroutine: there
 * the swallow would hide a cancellation signal from a surviving parent.
 */
internal fun runBlockingForWidget(block: suspend () -> Unit) {
    try {
        runBlocking { block() }
    } catch (_: InterruptedException) {
        AppLog.w(AppLog.T.STATS, "Widget data fetch interrupted")
        Thread.currentThread().interrupt()
    } catch (_: CancellationException) {
        AppLog.w(AppLog.T.STATS, "Widget data fetch cancelled")
        Thread.currentThread().interrupt()
    }
}
