package org.wordpress.android.util

import android.app.Activity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.widgets.WPSnackbar
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

private const val DELAY_LOWER_MARGIN_MS: Long = 1000
private const val DELAY_HIGHER_MARGIN_MS: Long = 20000
private const val CLOCK_BEHIND_MARGIN_MS: Long = 2 * DELAY_HIGHER_MARGIN_MS

// Taken from com.google.android.material.snackbar.SnackbarManager.java
// Did not find a way to get them directly from the android framework for now
private const val SHORT_DURATION_MS = 1500
private const val LONG_DURATION_MS = 2750

@Singleton
class SnackbarSequencer @Inject constructor(
    private val uiHelper: UiHelpers,
    private val contextProvider: ContextProvider
) : Snackbar.Callback(), CoroutineScope {
    private var job: Job = Job()

    private var nextAvailableTimeSlot: Long = 0
    private var nextDelay: Long = 0

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private suspend fun schedule(snackbarSequencerInfo: SnackbarSequencerInfo, delay: Long) {
        delay(delay)
        withContext(Dispatchers.Main) {
            val context = snackbarSequencerInfo.context.get()

            context?.let {
                AppLog.d(T.UTILS, "SnackbarSequencer > schedule context [$it]")

                if ((context as? Activity)?.isFinishing == false) {
                    snackbarSequencerInfo.snackbarInfo.view.get()?.let {view ->
                        snackbarSequencerInfo.snackbarInfo.let { snackbarInfo ->
                            val message = uiHelper.getTextOfUiString(contextProvider.getContext(), snackbarInfo.textRes)

                            val snackbar = WPSnackbar.make(
                                    view,
                                    message,
                                    snackbarInfo.duration
                            )

                            snackbarSequencerInfo.snackbarActionInfo?.let { actionInfo ->
                                snackbar.setAction(
                                        uiHelper.getTextOfUiString(contextProvider.getContext(), actionInfo.textRes),
                                        actionInfo.clickListener.get()
                                )
                            }

                            snackbarSequencerInfo.snackbarCallbackInfo?.let { callbackinfo ->
                                snackbar.addCallback(
                                        callbackinfo.snackbarCallback.get()
                                )
                            }

                            AppLog.d(T.UTILS, "SnackbarSequencer > schedule Showing snackbar [$message]")

                            snackbar.show()
                        }
                    } ?: AppLog.d(
                            T.UTILS,
                            "SnackbarSequencer > schedule " +
                                    "skipping snackbar [${uiHelper.getTextOfUiString(
                                            contextProvider.getContext(),
                                            snackbarSequencerInfo.snackbarInfo.textRes)}]"
                    )
                }
            } ?: AppLog.d(T.UTILS, "SnackbarSequencer > schedule got null context (garbaged?)")
        }
    }

    @Synchronized
    private fun estimateNextAvailableTimeSlot(durationInMs: Int, snackbarCreationTimestamp: Long): Long {
        val currTime = System.currentTimeMillis()
        val lastAvailableTimeSlot = nextAvailableTimeSlot
        val lastDelay = nextDelay

        AppLog.d(T.UTILS, "SnackbarSequencer > estimateNextAvailableTimeSlot BEGIN currTime [$currTime] " +
                "lastDelay [$lastDelay] lastAvailableTimeSlot [$lastAvailableTimeSlot] " +
                "lastAvailableTimeSlot - currTime [${lastAvailableTimeSlot - currTime}] " +
                "lastDelay + CLOCK_BEHIND_MARGIN_MS [${lastDelay + CLOCK_BEHIND_MARGIN_MS}]")

        if (
            (currTime >= lastAvailableTimeSlot) ||
            (lastAvailableTimeSlot - currTime > lastDelay + CLOCK_BEHIND_MARGIN_MS) ||
            (currTime - snackbarCreationTimestamp > lastDelay + CLOCK_BEHIND_MARGIN_MS)
        ) {
            nextDelay = DELAY_LOWER_MARGIN_MS
        } else {
            nextDelay = lastAvailableTimeSlot - currTime

            if (nextDelay < DELAY_LOWER_MARGIN_MS) nextDelay = DELAY_LOWER_MARGIN_MS
            if (nextDelay > DELAY_HIGHER_MARGIN_MS) nextDelay = DELAY_HIGHER_MARGIN_MS
        }

        nextAvailableTimeSlot = currTime + nextDelay + durationInMs

        AppLog.d(T.UTILS, "SnackbarSequencer > estimateNextAvailableTimeSlot END " +
                "delay [$nextDelay] and last for ms [$durationInMs] nextAvailableTimeSlot [$nextAvailableTimeSlot]")
        return nextDelay
    }

    private fun getSnackbarDurationMs(snackbarSequencerInfo: SnackbarSequencerInfo): Int {
        return when (snackbarSequencerInfo.snackbarInfo.duration) {
            Snackbar.LENGTH_INDEFINITE -> throw IllegalArgumentException("Snackbar.LENGTH_INDEFINITE not allowed in sequencer.")
            Snackbar.LENGTH_LONG -> LONG_DURATION_MS
            Snackbar.LENGTH_SHORT -> SHORT_DURATION_MS
            else -> snackbarSequencerInfo.snackbarInfo.duration
        }
    }

    fun enqueueSnackbar(snackbarSequencerInfo: SnackbarSequencerInfo) {
        val delay = estimateNextAvailableTimeSlot(
                getSnackbarDurationMs(snackbarSequencerInfo),
                snackbarSequencerInfo.creationTimestamp
        )

        launch {
            schedule(snackbarSequencerInfo, 2 * delay)
        }
    }
}
