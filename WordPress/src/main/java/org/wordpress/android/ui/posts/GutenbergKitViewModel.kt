package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.posts.editor.GutenbergKitSettings
import javax.inject.Inject

/**
 * ViewModel for managing GutenbergKit editor settings and state.
 * Handles communication between EditPostActivity and GutenbergKitEditorFragment.
 */
class GutenbergKitViewModel @Inject constructor() : ViewModel() {
    private val _editorSettings = MutableLiveData<GutenbergKitSettings>()
    val editorSettings: LiveData<GutenbergKitSettings> = _editorSettings

    /**
     * Updates the editor settings. Called by EditPostActivity when creating the fragment.
     */
    fun updateEditorSettings(settings: GutenbergKitSettings) {
        _editorSettings.value = settings
    }
}
