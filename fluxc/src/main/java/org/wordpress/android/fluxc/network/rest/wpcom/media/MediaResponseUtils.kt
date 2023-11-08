package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.text.TextUtils
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse
import javax.inject.Inject

class MediaResponseUtils
@Inject constructor() {
    /**
     * Creates a [MediaModel] list from a WP.com REST response to a request for all media.
     */
    fun getMediaListFromRestResponse(
        from: MultipleMediaResponse,
        localSiteId: Int
    ): List<MediaModel> {
        return from.media.mapNotNull {
            getMediaFromRestResponse(it).apply { this.localSiteId = localSiteId }
        }
    }

    /**
     * Creates a [MediaModel] from a WP.com REST response to a fetch request.
     */
    fun getMediaFromRestResponse(from: MediaWPComRestResponse) = MediaModel(
        0,
        from.ID,
        from.post_ID,
        from.author_ID,
        from.guid,
        from.date,
        from.URL,
        from.thumbnails?.let {
            if (!TextUtils.isEmpty(it.fmt_std)) {
                it.fmt_std
            } else {
                it.thumbnail
            }
        },
        from.file,
        from.extension,
        from.mime_type,
        StringEscapeUtils.unescapeHtml4(from.title),
        StringEscapeUtils.unescapeHtml4(from.caption),
        StringEscapeUtils.unescapeHtml4(from.description),
        StringEscapeUtils.unescapeHtml4(from.alt),
        from.width,
        from.height,
        from.length,
        from.videopress_guid,
        from.videopress_processing_done,
        if (MediaWPComRestResponse.DELETED_STATUS == from.status) {
            MediaUploadState.DELETED
        } else {
            MediaUploadState.UPLOADED
        },
        from.thumbnails?.let { if (!TextUtils.isEmpty(it.medium)) it.medium else null },
        null,
        from.thumbnails?.let { if (!TextUtils.isEmpty(it.large)) it.large else null },
        MediaWPComRestResponse.DELETED_STATUS == from.status
    )
}
