package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.repository.ReaderReadingPreferencesRepository

@ExperimentalCoroutinesApi
class ReaderGetReadingPreferencesUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var repository: ReaderReadingPreferencesRepository

    @Test
    fun `invoke should return reading preferences from repository`() = test {
        // Given
        val useCase = ReaderGetReadingPreferencesUseCase(repository)

        // When
        useCase()

        // Then
        verify(repository).getReadingPreferences()
    }
}
