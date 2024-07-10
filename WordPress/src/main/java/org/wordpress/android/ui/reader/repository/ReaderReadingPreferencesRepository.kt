package org.wordpress.android.ui.reader.repository

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.util.EnumWithFallbackValueTypeAdapterFactory
import org.wordpress.android.util.config.ReaderReadingPreferencesFeatureConfig
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReaderReadingPreferencesRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readingPreferencesFeatureConfig: ReaderReadingPreferencesFeatureConfig,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(EnumWithFallbackValueTypeAdapterFactory())
        .create()

    // the preferences never change during the app lifecycle, so we can cache them safely for better performance
    private var readingPreferences: ReaderReadingPreferences? = null

    suspend fun getReadingPreferences(): ReaderReadingPreferences = withContext(ioDispatcher) {
        getReadingPreferencesSync()
    }

    fun getReadingPreferencesSync(): ReaderReadingPreferences {
        if (!readingPreferencesFeatureConfig.isEnabled()) {
            return ReaderReadingPreferences()
        }

        return readingPreferences ?: loadReadingPreferences().also {
            readingPreferences = it
        }
    }

    suspend fun saveReadingPreferences(preferences: ReaderReadingPreferences): Unit = withContext(ioDispatcher) {
        appPrefsWrapper.readerReadingPreferencesJson = gson.toJson(preferences)
        readingPreferences = preferences
    }

    private fun loadReadingPreferences(): ReaderReadingPreferences {
        return appPrefsWrapper.readerReadingPreferencesJson?.let {
            gson.fromJson(it, ReaderReadingPreferences::class.java)
        } ?: ReaderReadingPreferences()
    }
}
