package org.wordpress.android.ui.posts

import android.net.Uri
import android.text.TextUtils
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.ArrayList
import javax.inject.Inject

const val EMPTY_LOCAL_POST_ID = -1

/**
 * Helper class for separating logic related to FeaturedImage upload.
 *
 * This class is not testable at the moment, since it uses Static methods and Android dependencies.
 * However, it at least separates this piece of business logic from the view layer.
 */
@Reusable
internal class FeaturedImageHelper @Inject constructor(
    private val uploadStore: UploadStore,
    private val mediaStore: MediaStore,
    private val uploadServiceFacade: UploadServiceFacade,
    private val resourceProvider: ResourceProvider,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun getFailedFeaturedImageUpload(post: PostImmutableModel): MediaModel? {
        val failedMediaForPost = uploadStore.getFailedMediaForPost(post)
        for (item in failedMediaForPost) {
            if (item != null && item.markedLocallyAsFeatured) {
                return item
            }
        }
        return null
    }

    fun retryFeaturedImageUpload(
        site: SiteModel,
        post: PostImmutableModel
    ): MediaModel? {
        val mediaModel = getFailedFeaturedImageUpload(post)
        if (mediaModel != null) {
            uploadServiceFacade.cancelFinalNotification(post)
            uploadServiceFacade.cancelFinalNotificationForMedia(site)
            mediaModel.setUploadState(MediaUploadState.QUEUED)
            dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel))
            startUploadService(mediaModel)

            trackFeaturedImageEvent(TrackableEvent.IMAGE_UPLOAD_RETRY_CLICKED, post.id)
        }
        return mediaModel
    }

    private fun startUploadService(media: MediaModel) {
        val mediaList = ArrayList<MediaModel>()
        mediaList.add(media)
        uploadServiceFacade.uploadMedia(mediaList)
    }

    fun queueFeaturedImageForUpload(
        localPostId: Int,
        site: SiteModel,
        uri: Uri,
        mimeType: String?
    ): EnqueueFeaturedImageResult {
        val media = fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, site.id)
                ?: return EnqueueFeaturedImageResult.FILE_NOT_FOUND
        if (localPostId != EMPTY_LOCAL_POST_ID) {
            media.localPostId = localPostId
        } else {
            AppLog.e(T.MEDIA, "Upload featured image can't be invoked without a valid local post id.")
            return EnqueueFeaturedImageResult.INVALID_POST_ID
        }
        media.markedLocallyAsFeatured = true

        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
        startUploadService(media)
        return EnqueueFeaturedImageResult.SUCCESS
    }

    fun cancelFeaturedImageUpload(
        site: SiteModel,
        post: PostImmutableModel,
        cancelFailedOnly: Boolean
    ) {
        var mediaModel: MediaModel? = getFailedFeaturedImageUpload(post)
        if (!cancelFailedOnly && mediaModel == null) {
            mediaModel = uploadServiceFacade.getPendingOrInProgressFeaturedImageUploadForPost(post)
        }
        if (mediaModel != null) {
            val payload = CancelMediaPayload(site, mediaModel, true)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
            uploadServiceFacade.cancelFinalNotification(post)
            uploadServiceFacade.cancelFinalNotificationForMedia(site)

            trackFeaturedImageEvent(TrackableEvent.IMAGE_UPLOAD_CANCELED, post.id)
        }
    }

    fun createCurrentFeaturedImageState(site: SiteModel, post: PostImmutableModel): FeaturedImageData {
        var uploadModel: MediaModel? = uploadServiceFacade.getPendingOrInProgressFeaturedImageUploadForPost(post)
        if (uploadModel != null) {
            return FeaturedImageData(FeaturedImageState.IMAGE_UPLOAD_IN_PROGRESS, uploadModel.filePath)
        }
        uploadModel = getFailedFeaturedImageUpload(post)
        if (uploadModel != null) {
            return FeaturedImageData(FeaturedImageState.IMAGE_UPLOAD_FAILED, uploadModel.filePath)
        }
        if (!post.hasFeaturedImage()) {
            return FeaturedImageData(FeaturedImageState.IMAGE_EMPTY, null)
        }

        val media = mediaStore.getSiteMediaWithId(site, post.featuredImageId) ?: return FeaturedImageData(
                FeaturedImageState.IMAGE_EMPTY,
                null
        )

        // Get max width/height for photon thumbnail - we load a smaller image so it's loaded quickly
        val maxDimen = resourceProvider.getDimension(R.dimen.post_settings_featured_image_height_min).toInt()

        val mediaUri = StringUtils.notNullStr(
                if (TextUtils.isEmpty(media.thumbnailUrl)) {
                    media.url
                } else {
                    media.thumbnailUrl
                }
        )

        val photonUrl = readerUtilsWrapper.getResizedImageUrl(
                mediaUri,
                maxDimen,
                maxDimen,
                !siteUtilsWrapper.isPhotonCapable(site),
                site.isPrivateWPComAtomic
        )
        return FeaturedImageData(FeaturedImageState.REMOTE_IMAGE_LOADING, photonUrl)
    }

    fun trackFeaturedImageEvent(
        event: TrackableEvent,
        postId: Int
    ) = analyticsTrackerWrapper.track(event.label, mapOf(POST_ID_KEY to postId))

    internal data class FeaturedImageData(val uiState: FeaturedImageState, val mediaUri: String?)

    internal enum class FeaturedImageState(
        val buttonVisible: Boolean = false,
        val imageViewVisible: Boolean = false,
        val localImageViewVisible: Boolean = false,
        val progressOverlayVisible: Boolean = false,
        val retryOverlayVisible: Boolean = false
    ) {
        IMAGE_EMPTY(buttonVisible = true),
        REMOTE_IMAGE_LOADING(localImageViewVisible = true, imageViewVisible = true),
        REMOTE_IMAGE_SET(imageViewVisible = true),
        IMAGE_UPLOAD_IN_PROGRESS(localImageViewVisible = true, progressOverlayVisible = true),
        IMAGE_UPLOAD_FAILED(localImageViewVisible = true, retryOverlayVisible = true);
    }

    internal enum class EnqueueFeaturedImageResult {
        FILE_NOT_FOUND, INVALID_POST_ID, SUCCESS
    }

    internal enum class TrackableEvent(val label: Stat) {
        IMAGE_SET_CLICKED(Stat.FEATURED_IMAGE_SET_CLICKED_POST_SETTINGS),
        IMAGE_PICKED(Stat.FEATURED_IMAGE_PICKED_POST_SETTINGS),
        IMAGE_UPLOAD_CANCELED(Stat.FEATURED_IMAGE_UPLOAD_CANCELED_POST_SETTINGS),
        IMAGE_UPLOAD_RETRY_CLICKED(Stat.FEATURED_IMAGE_UPLOAD_RETRY_CLICKED_POST_SETTINGS),
        IMAGE_REMOVE_CLICKED(Stat.FEATURED_IMAGE_REMOVE_CLICKED_POST_SETTINGS)
    }

    companion object {
        private const val POST_ID_KEY = "post_id"
    }
}
