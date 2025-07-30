package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.posts.PostSettingsListDialogFragment.DialogType
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

/**
 * ViewModel for EditPostSettingsFragment that manages dialog communication and settings state.
 * Handles both dialog results and featured image management.
 */
class EditPostSettingsViewModel @Inject constructor() : ViewModel() {
    private val _dialogResult = MutableLiveData<Event<DialogResult>>()
    val dialogResult: LiveData<Event<DialogResult>> = _dialogResult

    private val _clearFeaturedImage = MutableLiveData<Event<Unit>>()
    val clearFeaturedImage: LiveData<Event<Unit>> = _clearFeaturedImage

    /**
     * Called when a dialog (status, format, author) returns a result.
     * Emits the result as an event for the fragment to handle.
     */
    fun onDialogResult(dialogType: DialogType, checkedIndex: Int, selectedItem: String?) {
        _dialogResult.value = Event(DialogResult(dialogType, checkedIndex, selectedItem))
    }

    /**
     * Called when the featured image should be cleared.
     * Emits an event for the activity to handle editor updates.
     */
    fun onClearFeaturedImage() {
        _clearFeaturedImage.value = Event(Unit)
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
