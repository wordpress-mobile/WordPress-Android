package org.wordpress.android.ui.mediapicker.insert

import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalUri
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import javax.inject.Inject

class DeviceListInsertUseCase(
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val queueResults: Boolean
) : MediaInsertUseCase {
    override suspend fun insert(identifiers: List<Identifier>) = flow {
        val localUris = identifiers.mapNotNull { it as? LocalUri }
        val result = if (queueResults) {
            emit(InsertModel.Progress(actionTitle))
            var failed = false
            val fetchedUris = localUris.mapNotNull { localUri ->
                val fetchedUri = wpMediaUtilsWrapper.fetchMedia(localUri.value.uri)
                if (fetchedUri == null) {
                    failed = true
                }
                fetchedUri
            }
            if (failed) {
                InsertModel.Error("Failed to fetch local media")
            } else {
                InsertModel.Success(fetchedUris.map { LocalUri(UriWrapper(it), queueResults) })
            }
        } else {
            InsertModel.Success(localUris)
        }
        emit(result)
    }

    class DeviceListInsertUseCaseFactory
    @Inject constructor(
        private val wpMediaUtilsWrapper: WPMediaUtilsWrapper
    ) {
        fun build(queueResults: Boolean): DeviceListInsertUseCase {
            return DeviceListInsertUseCase(
                    wpMediaUtilsWrapper,
                    queueResults
            )
        }
    }
}
