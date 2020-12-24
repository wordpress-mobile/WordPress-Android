package org.wordpress.android.ui.jetpack.scan.details

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class ThreatDetailsViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false
    private var threatId: Long = 0

    fun start(threatId: Long) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.threatId = threatId
    }
}
