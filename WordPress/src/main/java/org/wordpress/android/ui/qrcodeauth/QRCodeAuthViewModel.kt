package org.wordpress.android.ui.qrcodeauth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QRCodeAuthViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
    }

    companion object {
        const val TAG_DISMISS_DIALOG = "TAG_DISMISS_DIALOG"
    }
}
