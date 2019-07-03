package org.wordpress.android.ui.posts

import android.content.Context
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.StringUtils
import java.util.ArrayList
import javax.inject.Inject

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
    private val dispatcher: Dispatcher
) {
    private fun getFailedFeaturedImageUpload(post: PostModel): MediaModel? {
        val failedMediaForPost = uploadStore.getFailedMediaForPost(post)
        for (item in failedMediaForPost) {
            if (item.markedLocallyAsFeatured) {
                return item
            }
        }
        return null
    }

    fun retryFeaturedImageUpload(
        context: Context,
        site: SiteModel,
        post: PostModel
    ): MediaModel? {
        val mediaModel = getFailedFeaturedImageUpload(post)
        if (mediaModel != null) {
            UploadService.cancelFinalNotification(context, post)
            UploadService.cancelFinalNotificationForMedia(context, site)
            mediaModel.setUploadState(MediaUploadState.QUEUED)
            dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(mediaModel))
            startUploadService(context, mediaModel)
        }
        return mediaModel
    }

    private fun startUploadService(context: Context, media: MediaModel) {
        val mediaList = ArrayList<MediaModel>()
        mediaList.add(media)
        UploadService.uploadMedia(context, mediaList)
    }

    fun cancelFeaturedImageUpload(context: Context, site: SiteModel, post: PostModel, cancelFailedOnly: Boolean) {
        var mediaModel: MediaModel? = getFailedFeaturedImageUpload(post)
        if (!cancelFailedOnly && mediaModel == null) {
            mediaModel = UploadService.getPendingOrInProgressFeaturedImageUploadForPost(post)
        }
        if (mediaModel != null) {
            val payload = CancelMediaPayload(site, mediaModel, true)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
            UploadService.cancelFinalNotification(context, post)
            UploadService.cancelFinalNotificationForMedia(context, site)
        }
    }

    fun createCurrentFeaturedImageState(context: Context, site: SiteModel, post: PostModel): FeaturedImageData {
        var uploadModel: MediaModel? = UploadService.getPendingOrInProgressFeaturedImageUploadForPost(post)
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
        val maxDimen = context.resources.getDimension(R.dimen.post_settings_featured_image_height_min).toInt()

        val mediaUri = StringUtils.notNullStr(media.thumbnailUrl)
        val photonUrl = ReaderUtils.getResizedImageUrl(mediaUri, maxDimen, maxDimen, !SiteUtils.isPhotonCapable(site))
        return FeaturedImageData(FeaturedImageState.IMAGE_SET, photonUrl)
    }

    internal data class FeaturedImageData(val uiState: FeaturedImageState, val mediaUri: String?)


    internal enum class FeaturedImageState(
        val buttonVisible: Boolean = false,
        val imageViewVisible: Boolean = false,
        val progressOverlayVisible: Boolean = false,
        val retryOverlayVisible: Boolean = false
    ) {
        IMAGE_EMPTY(buttonVisible = true),
        IMAGE_SET(imageViewVisible = true),
        IMAGE_UPLOAD_IN_PROGRESS(imageViewVisible = true, progressOverlayVisible = true),
        IMAGE_UPLOAD_FAILED(imageViewVisible = true, retryOverlayVisible = true);
    }
}