package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.DELETED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED

private const val DELETED_STATUS = "deleted"

data class MediaWPRESTResponse(
    val id: Long,
    val date: String,
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
    val altText: String,
    val mediaType: String,
    val mimeType: String,
    val mediaDetails: MediaDetails,
    val sourceURL: String
) {
    data class Attribute(
        val rendered: String
    )

    data class MediaDetails(
        val width: Int,
        val height: Int,
        val file: String,
        val sizes: Sizes?,
        val imageMeta: ImageMeta
    )

    data class ImageMeta(
        val aperture: String,
        val credit: String,
        val camera: String,
        val caption: String,
        val createdTimestamp: String,
        val copyright: String,
        val focalLength: String,
        val iso: String,
        val shutterSpeed: String,
        val title: String,
        val orientation: String,
        val keywords: List<Any?>
    )

    data class Sizes(
        val medium: ImageSize?,
        val thumbnail: ImageSize?,
        val newspackArticleBlockLandscapeSmall: ImageSize?,
        val newspackArticleBlockPortraitSmall: ImageSize?,
        val newspackArticleBlockSquareSmall: ImageSize?,
        val newspackArticleBlockLandscapeTiny: ImageSize?,
        val newspackArticleBlockPortraitTiny: ImageSize?,
        val newspackArticleBlockSquareTiny: ImageSize?,
        val woocommerceThumbnail: ImageSize?,
        val woocommerceSingle: ImageSize?,
        val woocommerceGalleryThumbnail: ImageSize?,
        val shopCatalog: ImageSize?,
        val shopSingle: ImageSize?,
        val shopThumbnail: ImageSize?,
        val full: ImageSize?
    )

    data class ImageSize(
        val path: String?,
        val file: String?,
        val width: Long,
        val height: Long,
        val virtual: Boolean?,
        val mimeType: String,
        val sourceURL: String,
        val uncropped: Boolean? = null
    )
}

fun MediaWPRESTResponse.toMediaModel(): MediaModel {
    val mediaModel = MediaModel()
    mediaModel.mediaId = id
    mediaModel.uploadDate = date
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
