package org.wordpress.android.ui.quickstart

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask

@Parcelize
class QuickStartEvent(val task: QuickStartTask) : Parcelable {
    companion object {
        const val KEY = "quick_start_event"
    }
}
