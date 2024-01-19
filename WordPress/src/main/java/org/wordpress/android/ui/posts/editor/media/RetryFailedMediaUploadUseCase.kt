package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import javax.inject.Inject

@Reusable
class RetryFailedMediaUploadUseCase @Inject constructor(
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val tracker: AnalyticsTrackerWrapper,
    private val siteStore: SiteStore
) {
    suspend fun retryFailedMediaAsync(
        editorMediaListener: EditorMediaListener,
        failedMediaLocalIds: List<Int>
    ) {
        getMediaModelUseCase
            .loadMediaByLocalId(failedMediaLocalIds)
            .map { media ->
                updateMediaModelUseCase.updateMediaModel(
                    media,
                    editorMediaListener.getImmutablePost(),
                    QUEUED
                )
                media
            }
            .let { mediaModels ->
                if (mediaModels.isNotEmpty()) {
                    uploadMediaUseCase.saveQueuedPostAndStartUpload(
                        editorMediaListener,
                        mediaModels
                    )
                    val siteId = editorMediaListener.getImmutablePost().localSiteId
                    val site: SiteModel? = siteStore.getSiteByLocalId(siteId)
                    AnalyticsUtils.trackWithSiteDetails(tracker, Stat.EDITOR_UPLOAD_MEDIA_RETRIED, site, null);
                }
            }
    }
}
