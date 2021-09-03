package org.wordpress.android.ui.mediapicker.insert

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier

class MediaInsertHandler(private val mediaInsertUseCase: MediaInsertUseCase) {
    suspend fun insertMedia(identifiers: List<Identifier>): Flow<InsertModel> {
        return mediaInsertUseCase.insert(identifiers)
    }

    sealed class InsertModel {
        data class Success(val identifiers: List<Identifier>) : InsertModel()
        data class Error(val error: String) : InsertModel()
        data class Progress(val title: Int) : InsertModel()
    }
}
