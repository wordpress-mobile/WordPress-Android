package org.wordpress.android.ui.reader.usecases

import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.repository.ReaderReadingPreferencesRepository
import javax.inject.Inject

class ReaderGetReadingPreferencesSyncUseCase @Inject constructor(
    private val repository: ReaderReadingPreferencesRepository
) {
    operator fun invoke(): ReaderReadingPreferences {
        return repository.getReadingPreferencesSync()
    }
}
