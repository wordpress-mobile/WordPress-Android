package org.wordpress.android.ui.domains

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class DomainsDashboardViewModel @Inject constructor() : ViewModel() {
    private var isStarted: Boolean = false
    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true
    }
}
