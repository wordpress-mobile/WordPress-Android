package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.text.TextUtils
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.DELETED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse
import javax.inject.Inject

class MediaResponseUtils
@Inject constructor() {
    /**
     * Creates a [MediaModel] list from a WP.com REST response to a request for all media.
     */
    fun getMediaListFromRestResponse(from: MultipleMediaResponse, localSiteId: Int): List<MediaModel> {
        return from.media.mapNotNull {
            getMediaFromRestResponse(it).apply { this.localSiteId = localSiteId }
        }
    }

    /**
     * Creates a [MediaModel] from a WP.com REST response to a fetch request.
     */
    fun getMediaFromRestResponse(from: MediaWPComRestResponse): MediaModel {
        val media = MediaModel()
        media.mediaId = from.ID
        media.uploadDate = from.date
        media.postId = from.post_ID
        media.authorId = from.author_ID
        media.url = from.URL
        media.guid = from.guid
        media.fileName = from.file
        media.fileExtension = from.extension
        media.mimeType = from.mime_type
        media.title = StringEscapeUtils.unescapeHtml4(from.title)
        media.caption = StringEscapeUtils.unescapeHtml4(from.caption)
        media.description = StringEscapeUtils.unescapeHtml4(from.description)
        media.alt = StringEscapeUtils.unescapeHtml4(from.alt)
        from.thumbnails?.let {
            if (!TextUtils.isEmpty(it.fmt_std)) {
                media.thumbnailUrl = it.fmt_std
            } else {
                media.thumbnailUrl = it.thumbnail
            }
            if (!TextUtils.isEmpty(it.large)) {
                media.fileUrlLargeSize = it.large
            }
            if (!TextUtils.isEmpty(it.medium)) {
                media.fileUrlMediumSize = it.medium
            }
        }
        media.height = from.height
        media.width = from.width
        media.length = from.length
        media.videoPressGuid = from.videopress_guid
        media.videoPressProcessingDone = from.videopress_processing_done
        media.deleted = MediaWPComRestResponse.DELETED_STATUS == from.status
        if (!media.deleted) {
            media.setUploadState(UPLOADED)
        } else {
            media.setUploadState(DELETED)
        }
        return media
    }
}
