package org.wordpress.android.ui.jetpack.backup.download.complete

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class BackupDownloadCompleteViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
    }
}
