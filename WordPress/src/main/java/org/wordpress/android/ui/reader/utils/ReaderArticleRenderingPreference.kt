package org.wordpress.android.ui.reader.utils

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the post detail draws posts natively, as WPFolio does, or in the WebView. Native is the default; the
 * detail's More menu switches between them.
 */
@Singleton
class ReaderArticleRenderingPreference @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var isNative: Boolean
        get() = prefs.getBoolean(KEY_NATIVE_RENDERING, true)
        set(value) = prefs.edit { putBoolean(KEY_NATIVE_RENDERING, value) }

    companion object {
        private const val KEY_NATIVE_RENDERING = "reader_native_post_rendering"
    }
}
