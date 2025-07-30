package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.posts.PostSettingsListDialogFragment.DialogType
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

/**
 * ViewModel for EditPostSettingsFragment that manages dialog communication and settings state.
 * Currently handles dialog results, with plans to expand for other settings logic.
 */
class EditPostSettingsViewModel @Inject constructor() : ViewModel() {
    private val _dialogResult = MutableLiveData<Event<DialogResult>>()
    val dialogResult: LiveData<Event<DialogResult>> = _dialogResult

    /**
     * Called when a dialog (status, format, author) returns a result.
     * Emits the result as an event for the fragment to handle.
     */
    fun onDialogResult(dialogType: DialogType, checkedIndex: Int, selectedItem: String?) {
        _dialogResult.value = Event(DialogResult(dialogType, checkedIndex, selectedItem))
    }
}

/**
 * Data class representing a dialog result with all necessary information
 * for the fragment to process the user's selection.
 */
data class DialogResult(
    val dialogType: DialogType,
    val checkedIndex: Int,
    val selectedItem: String?
)
