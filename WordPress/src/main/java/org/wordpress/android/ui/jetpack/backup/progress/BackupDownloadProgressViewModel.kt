package org.wordpress.android.ui.jetpack.backup.progress

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class BackupDownloadProgressViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
    }
}
