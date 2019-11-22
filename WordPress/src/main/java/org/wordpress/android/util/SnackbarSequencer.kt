package org.wordpress.android.util

import android.app.Activity
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.widgets.WPSnackbar
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

private const val DURATION_EXTRA_MARGIN = 500L

private const val QUEUE_SIZE_LIMIT: Int = 10

// Taken from com.google.android.material.snackbar.SnackbarManager.java
// Did not find a way to get them directly from the android framework for now
private const val SHORT_DURATION_MS = 1500L
private const val LONG_DURATION_MS = 2750L

@Singleton
class SnackbarSequencer @Inject constructor(
    private val uiHelper: UiHelpers,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : Snackbar.Callback(), CoroutineScope {
    private var job: Job = Job()

    private val snackBarQueue: Queue<SnackbarSequencerInfo> = LinkedList()

    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    fun enqueue(item: SnackbarSequencerInfo) {
        synchronized(this@SnackbarSequencer) {
            AppLog.d(AppLog.T.UTILS,"SnackbarSequencer > New item added")
            if(snackBarQueue.size == QUEUE_SIZE_LIMIT) {
                snackBarQueue.remove()
            }
            snackBarQueue.add(item)
            if (snackBarQueue.size == 1) {
                launch {
                    AppLog.d(AppLog.T.UTILS,"SnackbarSequencer > invoking start()")
                    start()
                }
            }
        }
    }

    private suspend fun start() {
        while (true) {
            val item = snackBarQueue.peek()
            val context = item.context.get()
            if (context != null && isContextAlive(context)) {
                withContext(mainDispatcher) {
                    prepareSnackBar(context, item)?.show()
                }
                AppLog.e(AppLog.T.UTILS,"SnackbarSequencer > before delay")
                delay(getSnackbarDurationMs(item) + DURATION_EXTRA_MARGIN)
                AppLog.e(AppLog.T.UTILS,"SnackbarSequencer > after delay")
            }
            synchronized(this@SnackbarSequencer) {
                if (snackBarQueue.peek() == item) {
                    AppLog.d(T.UTILS, "SnackbarSequencer > item removed from the queue")
                    snackBarQueue.remove()
                }
                if (snackBarQueue.isEmpty()) {
                    AppLog.d(AppLog.T.UTILS,"SnackbarSequencer > finishing start()")
                    return
                }
            }
        }
    }

    private fun isContextAlive(context: Context): Boolean {
        return !(context as Activity).isFinishing
    }

    private fun prepareSnackBar(context: Context, item: SnackbarSequencerInfo): WPSnackbar? {
        return item.snackbarInfo.view.get()?.let { view ->
            val message = uiHelper.getTextOfUiString(context, item.snackbarInfo.textRes)

            val snackbar = WPSnackbar.make(view, message, item.snackbarInfo.duration)

            item.snackbarActionInfo?.let { actionInfo ->
                snackbar.setAction(
                        uiHelper.getTextOfUiString(context, actionInfo.textRes),
                        actionInfo.clickListener.get()
                )
            }

            item.snackbarCallbackInfo?.let { callbackinfo ->
                snackbar.addCallback(callbackinfo.snackbarCallback.get())
            }

            AppLog.d(T.UTILS, "SnackbarSequencer > showSnackBar Showing snackbar [$message]")

            return snackbar
        } ?: null.also {
            AppLog.e(T.UTILS, "SnackbarSequencer > showSnackBar Unexpected null view")
        }
    }

    private fun getSnackbarDurationMs(snackbarSequencerInfo: SnackbarSequencerInfo): Long {
        return when (snackbarSequencerInfo.snackbarInfo.duration) {
            Snackbar.LENGTH_INDEFINITE ->
                throw IllegalArgumentException("Snackbar.LENGTH_INDEFINITE not allowed in sequencer.")
            Snackbar.LENGTH_LONG -> LONG_DURATION_MS
            Snackbar.LENGTH_SHORT -> SHORT_DURATION_MS
            else -> snackbarSequencerInfo.snackbarInfo.duration.toLong()
        }
    }
}
