package org.wordpress.android.ui.posts

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class PrepublishingViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
    }
}
