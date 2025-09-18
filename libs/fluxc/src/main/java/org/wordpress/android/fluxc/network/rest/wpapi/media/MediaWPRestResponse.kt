@file:Suppress("MatchingDeclarationName")

package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Locale

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
    @SerializedName("source_url") val sourceURL: String?
) {
    data class Attribute(
        val rendered: String
    )

    data class MediaDetails(
        val width: Int,
        val height: Int,
        val file: String?,
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

fun MediaWPRESTResponse.toMediaModel(localSiteId: Int) = MediaModel(
    localSiteId,
    id,
    post ?: 0L,
    author,
    guid.rendered,
    DateTimeUtils.iso8601FromDate(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).parse(dateGmt)
    ),
    sourceURL.orEmpty(),
    mediaDetails.sizes?.thumbnail?.sourceURL,
    mediaDetails.file,
    mediaDetails.file?.substringAfterLast('.', ""),
    mimeType,
    StringEscapeUtils.unescapeHtml4(title.rendered),
    StringEscapeUtils.unescapeHtml4(caption.rendered),
    StringEscapeUtils.unescapeHtml4(description.rendered),
    StringEscapeUtils.unescapeHtml4(altText),
    mediaDetails.width,
    mediaDetails.height,
        0,
        null,
        false,
    if (MediaWPComRestResponse.DELETED_STATUS == status) {
        MediaUploadState.DELETED
    } else {
        MediaUploadState.UPLOADED
    },
    null,
    null,
    null,
    MediaWPComRestResponse.DELETED_STATUS == status
)
