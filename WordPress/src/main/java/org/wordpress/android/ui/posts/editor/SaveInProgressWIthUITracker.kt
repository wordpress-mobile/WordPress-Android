package org.wordpress.android.ui.posts.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.editor.gutenberg.PostSaveStatusTracker
import org.wordpress.android.editor.gutenberg.SaveState
import org.wordpress.android.editor.gutenberg.SaveState.NotSaving
import org.wordpress.android.editor.gutenberg.SaveState.SaveInProgress
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class SaveInProgressWIthUITracker @Inject constructor() : PostSaveStatusTracker {
    private val _saveInProgressData = MutableLiveData<SaveState>().apply {
        postValue(NotSaving)
    }
    override val saveInProgressData: LiveData<SaveState> = _saveInProgressData

    private var currentlyPendingCount = 0
        set(newValue) {
            val prevField = field
            field = if (newValue < 0) {
                AppLog.e(AppLog.T.EDITOR, "Save in progress count should never fall below 0.")
                0
            } else {
                newValue
            }

            if (prevField != field) {
                val newSaveState = _saveInProgressData.value?.let { prevSaveState ->
                    when (prevSaveState) {
                        NotSaving -> {
                            if (field > 0) {
                                SaveInProgress
                            } else null
                        }
                        SaveInProgress -> {
                            if (field == 0) {
                                NotSaving
                            } else null
                        }
                    }
                }

                newSaveState?.let { _saveInProgressData.postValue(it) }
            }
        }

    fun startingSave() {
        currentlyPendingCount++
    }

    fun endingSave() {
        currentlyPendingCount--
    }
}
