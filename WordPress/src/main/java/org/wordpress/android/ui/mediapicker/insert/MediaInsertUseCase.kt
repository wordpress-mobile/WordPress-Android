package org.wordpress.android.ui.mediapicker.insert

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.R
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel.Success

interface MediaInsertUseCase {
    val actionTitle: Int
        get() = R.string.media_uploading_default

    suspend fun insert(identifiers: List<Identifier>): Flow<InsertModel> = flowOf(Success(identifiers))
}
