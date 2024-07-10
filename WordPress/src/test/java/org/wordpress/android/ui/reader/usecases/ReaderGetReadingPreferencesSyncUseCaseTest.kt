package org.wordpress.android.ui.reader.usecases

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.ui.reader.repository.ReaderReadingPreferencesRepository

@RunWith(MockitoJUnitRunner::class)
class ReaderGetReadingPreferencesSyncUseCaseTest {
    @Mock
    lateinit var repository: ReaderReadingPreferencesRepository

    @Test
    fun `invoke should return reading preferences from repository`() {
        // Given
        val useCase = ReaderGetReadingPreferencesSyncUseCase(repository)

        // When
        useCase()

        // Then
        verify(repository).getReadingPreferencesSync()
    }
}
