package org.wordpress.android.ui.reader.utils

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the Subscriptions feed and site previews show posts as a gallery (an image grid) rather than a list.
 * Observable, so an open feed switches as soon as the Reader's top bar toggles it.
 */
@Singleton
class ReaderLayoutPreference @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val _isGallery = MutableStateFlow(prefs.getBoolean(KEY_GALLERY_LAYOUT, false))
    val isGallery: StateFlow<Boolean> = _isGallery.asStateFlow()

    fun toggle() {
        val isGallery = !_isGallery.value
        prefs.edit().putBoolean(KEY_GALLERY_LAYOUT, isGallery).apply()
        _isGallery.value = isGallery
    }

    companion object {
        private const val KEY_GALLERY_LAYOUT = "reader_gallery_layout"
    }
}
