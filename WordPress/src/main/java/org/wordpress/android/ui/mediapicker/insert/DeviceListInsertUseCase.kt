package org.wordpress.android.ui.mediapicker.insert

import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalUri
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class DeviceListInsertUseCase
@Inject constructor(
    private val copyMediaToAppStorageUseCase: CopyMediaToAppStorageUseCase
) : MediaInsertUseCase {
    override suspend fun insert(identifiers: List<Identifier>) = flow {
        val localUris = identifiers.mapNotNull { it as? LocalUri }.map { it.value.uri }
        val copyResult = copyMediaToAppStorageUseCase.copyFilesToAppStorage(localUris)

        val result = if (copyResult.copyingSomeMediaFailed) {
            InsertModel.Error("Failed to fetch local media")
        } else {
            InsertModel.Success(copyResult.permanentlyAccessibleUris.map { LocalUri(UriWrapper(it)) })
        }
        emit(result)
    }
}
