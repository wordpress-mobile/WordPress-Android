package org.wordpress.android.ui.mediapicker.insert

import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.insert.MediaInsertUseCase.MediaInsertResult.Success

interface MediaInsertUseCase {
    suspend fun insert(identifiers: List<Identifier>): MediaInsertResult = Success(identifiers)

    sealed class MediaInsertResult {
        data class Success(val identifiers: List<Identifier>) : MediaInsertResult()
        data class Failure(val message: String) : MediaInsertResult()
    }
}
