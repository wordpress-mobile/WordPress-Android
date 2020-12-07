package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
    }
}
