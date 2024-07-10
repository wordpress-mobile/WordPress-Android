package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences.Theme
import org.wordpress.android.ui.reader.repository.ReaderReadingPreferencesRepository

@ExperimentalCoroutinesApi
class ReaderSaveReadingPreferencesUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var repository: ReaderReadingPreferencesRepository

    @Test
    fun `invoke should save reading preferences to repository`() = test {
        // Given
        val readingPreferences = ReaderReadingPreferences(Theme.OLED)
        val useCase = ReaderSaveReadingPreferencesUseCase(repository)

        // When
        useCase(readingPreferences)

        // Then
        verify(repository).saveReadingPreferences(readingPreferences)
    }
}
