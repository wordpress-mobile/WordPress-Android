package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.gutenberg.WebViewGlobal
import java.io.Serializable
import javax.inject.Inject

data class GutenbergKitSettings(
    val postId: Int? = null,
    val postType: String,
    val postTitle: String? = null,
    val postContent: String? = null,
    val siteURL: String,
    val siteApiRoot: String,
    val namespaceExcludedPaths: List<String> = emptyList(),
    val authHeader: String,
    val siteApiNamespace: List<String> = emptyList(),
    val themeStyles: Boolean = false,
    val plugins: Boolean = false,
    val locale: String,
    val cookies: Map<String, String> = emptyMap(),
    val webViewGlobals: List<WebViewGlobal> = emptyList()
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

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
