package org.wordpress.android.ui.jetpack.backup

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class BackupDownloadViewModel @Inject constructor() : ViewModel() {
    private var isStarted: Boolean = false
    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true
    }
}
