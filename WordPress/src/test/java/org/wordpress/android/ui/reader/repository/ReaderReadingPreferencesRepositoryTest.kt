package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences.FontFamily
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences.FontSize
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences.Theme
import org.wordpress.android.util.config.ReaderReadingPreferencesFeatureConfig

@ExperimentalCoroutinesApi
class ReaderReadingPreferencesRepositoryTest : BaseUnitTest() {
    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var readingPreferencesFeatureConfig: ReaderReadingPreferencesFeatureConfig

    private lateinit var repository: ReaderReadingPreferencesRepository

    @Before
    fun setUp() {
        repository = ReaderReadingPreferencesRepository(
            appPrefsWrapper,
            readingPreferencesFeatureConfig,
            testDispatcher()
        )
    }

    @Test
    fun `getReadingPreferencesSync should return default preferences if feature is disabled`() {
        // Given
        whenever(readingPreferencesFeatureConfig.isEnabled()).thenReturn(false)

        // When
        val result = repository.getReadingPreferencesSync()

        // Then
        assertThat(result).isEqualTo(ReaderReadingPreferences())
    }

    @Test
    fun `getReadingPreferencesSync should return saved preferences the first time it's called`() {
        // Given
        whenever(readingPreferencesFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.readerReadingPreferencesJson).thenReturn(READER_PREFERENCES_JSON)

        // When
        val result = repository.getReadingPreferencesSync()

        // Then
        verify(appPrefsWrapper).readerReadingPreferencesJson
        assertThat(result).isEqualTo(READER_PREFERENCES)
    }

    @Test
    fun `getReadingPreferencesSync should return cached preferences the second time it's called`() {
        // Given
        whenever(readingPreferencesFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.readerReadingPreferencesJson).thenReturn(READER_PREFERENCES_JSON)

        // When
        repository.getReadingPreferencesSync()
        clearInvocations(appPrefsWrapper)
        val result = repository.getReadingPreferencesSync()

        // Then
        verifyNoInteractions(appPrefsWrapper)
        assertThat(result).isEqualTo(READER_PREFERENCES)
    }

    @Test
    fun `getReadingPreferences delegates to getReadingPreferencesSync`() = test {
        // Given
        val spyRepository = spy(repository)
        whenever(readingPreferencesFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.readerReadingPreferencesJson).thenReturn(READER_PREFERENCES_JSON)

        // When
        spyRepository.getReadingPreferences()

        // Then
        verify(spyRepository).getReadingPreferencesSync()
    }

    @Test
    fun `saveReadingPreferences should save preferences`() = test {
        // Given
        val preferences = READER_PREFERENCES

        // When
        repository.saveReadingPreferences(preferences)

        // Then
        verify(appPrefsWrapper).readerReadingPreferencesJson = READER_PREFERENCES_JSON
    }

    companion object {
        private val READER_PREFERENCES = ReaderReadingPreferences(Theme.SEPIA, FontFamily.MONO, FontSize.SMALL)
        private const val READER_PREFERENCES_JSON = """{"theme":"SEPIA","fontFamily":"MONO","fontSize":"SMALL"}"""
    }
}
