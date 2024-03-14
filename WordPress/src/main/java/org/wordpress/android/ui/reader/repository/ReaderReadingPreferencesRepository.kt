package org.wordpress.android.ui.reader.repository

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import javax.inject.Inject
import javax.inject.Named

class ReaderReadingPreferencesRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    private val gson = Gson()

    suspend fun getReadingPreferences(): ReaderReadingPreferences = withContext(ioDispatcher) {
        getReadingPreferencesSync()
    }

    fun getReadingPreferencesSync(): ReaderReadingPreferences {
        return appPrefsWrapper.readerReadingPreferencesJson?.let {
            gson.fromJson(it, ReaderReadingPreferences::class.java)
        } ?: ReaderReadingPreferences(ReaderReadingPreferences.Theme.SYSTEM)
    }

    suspend fun saveReadingPreferences(preferences: ReaderReadingPreferences): Unit = withContext(ioDispatcher) {
        appPrefsWrapper.readerReadingPreferencesJson = gson.toJson(preferences)
    }
}
