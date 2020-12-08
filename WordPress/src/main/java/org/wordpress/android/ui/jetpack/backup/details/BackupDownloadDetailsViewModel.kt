package org.wordpress.android.ui.jetpack.backup.details

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class BackupDownloadDetailsViewModel @Inject constructor() : ViewModel() {
    private var isStarted: Boolean = false
    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true
    }
}
