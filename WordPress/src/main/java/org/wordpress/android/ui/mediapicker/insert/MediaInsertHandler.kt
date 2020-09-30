package org.wordpress.android.ui.mediapicker.insert

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel.Error
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel.Progress
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel.Success
import org.wordpress.android.ui.mediapicker.insert.MediaInsertUseCase.MediaInsertResult

class MediaInsertHandler(private val mediaInsertUseCase: MediaInsertUseCase) {
    suspend fun insertMedia(identifiers: List<Identifier>): Flow<InsertModel> {
        return flow {
            emit(Progress)
            when (val result = mediaInsertUseCase.insert(identifiers)) {
                is MediaInsertResult.Success -> emit(Success(result.identifiers))
                is MediaInsertResult.Failure -> emit(Error(result.message))
            }
        }
    }

    sealed class InsertModel {
        data class Success(val identifiers: List<Identifier>) : InsertModel()
        data class Error(val error: String) : InsertModel()
        object Progress : InsertModel()
    }
}
