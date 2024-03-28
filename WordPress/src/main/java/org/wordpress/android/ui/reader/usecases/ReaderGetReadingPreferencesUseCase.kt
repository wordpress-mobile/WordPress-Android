package org.wordpress.android.ui.reader.usecases

import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.repository.ReaderReadingPreferencesRepository
import javax.inject.Inject

class ReaderGetReadingPreferencesUseCase @Inject constructor(
    private val repository: ReaderReadingPreferencesRepository
) {
    suspend operator fun invoke(): ReaderReadingPreferences {
        return repository.getReadingPreferences()
    }
}
