package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.DELETED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Locale

private const val DELETED_STATUS = "deleted"

data class MediaWPRESTResponse(
    val id: Long,
    @SerializedName("date_gmt") val dateGmt: String,
    val guid: Attribute,
    val slug: String,
    val status: String,
    val type: String,
    val link: String,
    val title: Attribute,
    val author: Long,
    val post: Long? = null,
    val description: Attribute,
    val caption: Attribute,
    @SerializedName("alt_text") val altText: String,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("media_details") val mediaDetails: MediaDetails,
    @SerializedName("source_url") val sourceURL: String
) {
    data class Attribute(
        val rendered: String
    )

    data class MediaDetails(
        val width: Int,
        val height: Int,
        val file: String,
        val sizes: Sizes?
    )

    data class Sizes(
        val medium: ImageSize?,
        val thumbnail: ImageSize?,
        val full: ImageSize?
    )

    data class ImageSize(
        val path: String?,
        val file: String?,
        val width: Long,
        val height: Long,
        val virtual: Boolean?,
        @SerializedName("media_type") val mimeType: String,
        @SerializedName("source_url") val sourceURL: String,
        val uncropped: Boolean? = null
    )
}

fun MediaWPRESTResponse.toMediaModel(localSiteId: Int): MediaModel {
    val mediaModel = MediaModel()
    mediaModel.localSiteId = localSiteId
    mediaModel.mediaId = id
    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).parse(dateGmt)
    mediaModel.uploadDate = DateTimeUtils.iso8601FromDate(date)
    mediaModel.postId = post ?: 0L
    mediaModel.authorId = author
    mediaModel.url = sourceURL
    mediaModel.guid = guid.rendered
    mediaModel.fileName = mediaDetails.file
    mediaModel.fileExtension = mediaDetails.file.substringAfterLast('.', "")
    mediaModel.mimeType = mimeType
    mediaModel.title = StringEscapeUtils.unescapeHtml4(title.rendered)
    mediaModel.caption = StringEscapeUtils.unescapeHtml4(caption.rendered)
    mediaModel.description = StringEscapeUtils.unescapeHtml4(description.rendered)
    mediaModel.alt = StringEscapeUtils.unescapeHtml4(altText)
    mediaModel.thumbnailUrl = mediaDetails.sizes?.thumbnail?.sourceURL
    mediaModel.height = mediaDetails.height
    mediaModel.width = mediaDetails.width
    mediaModel.deleted = DELETED_STATUS == status
    mediaModel.setUploadState(if (mediaModel.deleted) DELETED else UPLOADED)
    return mediaModel
}
