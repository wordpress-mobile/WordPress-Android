package org.wordpress.android.ui.mediapicker.insert

import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalUri
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler.InsertModel
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject

class DeviceListInsertUseCase(
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
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
                InsertModel.Success(fetchedUris.map { LocalUri(UriWrapper(it)) })
            }
        } else {
            trackLocalItemsSelected(localUris)
            InsertModel.Success(localUris)
        }
        emit(result)
    }

    private fun trackLocalItemsSelected(identifiers: List<LocalUri>) {
        val isMultiselection = identifiers.size > 1
        for (identifier in identifiers) {
            val isVideo = org.wordpress.android.util.MediaUtils.isVideo(identifier.toString())
            val properties = analyticsUtilsWrapper.getMediaProperties(
                    isVideo,
                    identifier.value,
                    null
            )
            properties["is_part_of_multiselection"] = isMultiselection
            if (isMultiselection) {
                properties["number_of_media_selected"] = identifiers.size
            }
            analyticsTrackerWrapper.track(MEDIA_PICKER_RECENT_MEDIA_SELECTED, properties)
        }
    }

    class DeviceListInsertUseCaseFactory
    @Inject constructor(
        private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
        private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
        private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
    ) {
        fun build(queueResults: Boolean): DeviceListInsertUseCase {
            return DeviceListInsertUseCase(
                    wpMediaUtilsWrapper,
                    analyticsTrackerWrapper,
                    analyticsUtilsWrapper,
                    queueResults
            )
        }
    }
}
