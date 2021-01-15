package org.wordpress.android.ui.jetpack.restore.details

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class RestoreDetailsViewModel @Inject constructor() : ViewModel() {
    private var isStarted: Boolean = false

    fun start() {
        if (isStarted) return
        isStarted = true
    }
}
