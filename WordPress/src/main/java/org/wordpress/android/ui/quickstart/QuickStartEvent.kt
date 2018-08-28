package org.wordpress.android.ui.quickstart

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

/**
 * Container for passing around QuickStartTask to destinations and retaining it there
 **/
@Parcelize
@SuppressLint("ParcelCreator")
class QuickStartEvent(val task: QuickStartTask) : Parcelable {
    companion object {
        const val KEY = "quick_start_event"
    }
}
