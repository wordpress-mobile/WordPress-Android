package org.wordpress.android.ui.reader.repository

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import javax.inject.Inject
import javax.inject.Named

class ReaderReadingPreferencesRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
) {
    private val gson = Gson()

    suspend fun getReadingPreferences(): ReaderReadingPreferences = withContext(ioDispatcher) {
        appPrefsWrapper.readerReadingPreferencesJson?.let {
            gson.fromJson(it, ReaderReadingPreferences::class.java)
        } ?: ReaderReadingPreferences(
            theme = ReaderReadingPreferences.Theme.Light,
        )
    }

    suspend fun saveReadingPreferences(preferences: ReaderReadingPreferences): Unit = withContext(ioDispatcher) {
        appPrefsWrapper.readerReadingPreferencesJson = gson.toJson(preferences)
    }

    companion object {
        private const val READER_READING_PREFERENCES = "reader_reading_preferences"
    }
}
