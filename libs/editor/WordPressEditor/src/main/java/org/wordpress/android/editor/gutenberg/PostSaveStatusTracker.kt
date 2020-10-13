package org.wordpress.android.editor.gutenberg

import androidx.lifecycle.LiveData

enum class SaveState { NotSaving, SaveInProgress }

interface PostSaveStatusTracker {
    val saveInProgressData: LiveData<SaveState>
}
