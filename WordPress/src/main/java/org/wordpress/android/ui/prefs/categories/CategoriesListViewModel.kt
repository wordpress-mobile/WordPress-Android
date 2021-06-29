package org.wordpress.android.ui.prefs.categories

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class CategoriesListViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
    }
}
