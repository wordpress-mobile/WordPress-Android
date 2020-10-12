package org.wordpress.android.editor.gutenberg

import androidx.lifecycle.LiveData

interface PostSaveStatusTracker {
    val getSaveInProgressData: LiveData<Boolean>
}