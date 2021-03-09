package org.wordpress.android.ui.posts.chat

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.fluxc.model.MediaModel
import kotlin.math.min

private const val MAX_COLUMNS = 3

val String.wordCount: Int
    get() = replace("\n", " ").split(" ").size

private val String.brForNewLines: String
    get() = this.replace("\n", "<br>")

val String.paragraphBlock: String
    get() = "<!-- wp:paragraph --><p>$brForNewLines</p><!-- /wp:paragraph -->"

val String.headingBlock: String
    get() = "<!-- wp:heading --><h2>$brForNewLines</h2><!-- /wp:heading -->"

fun imageBlock(image: MediaModel) =
        """<!-- wp:image {"id":${image.mediaId},"sizeSlug":"large"} --><figure class="wp-block-image size-large">
            |<img src="${image.fileUrlLargeSize}" alt="" class="wp-image-${image.mediaId}"/>
            |</figure><!-- /wp:image -->""".trimMargin()

fun videoBlock(video: MediaModel) =
        """<!-- wp:video {"id":${video.mediaId}} --><figure class="wp-block-video">
            |<video controls src="${video.url}"></video></figure><!-- /wp:video -->""".trimMargin()

fun mediaTextBlock(image: MediaModel, text: String) =
        """<!-- wp:media-text {"mediaId":${image.mediaId},"mediaType":"image"} -->
            |<div class="wp-block-media-text alignwide is-stacked-on-mobile"><figure class="wp-block-media-text__media">
            |<img src="${image.url}" class="wp-image-${image.mediaId} size-full"/></figure>
            |<div class="wp-block-media-text__content">${text.paragraphBlock}</div>
            |</div><!-- /wp:media-text -->""".trimMargin()

fun galleryBlock(images: List<MediaModel>): String {
    val imageIds = images.map { it.mediaId }.joinToString(",")
    val galleryImages = images.joinToString("") { galleryImage(it) }
    val columns = min(MAX_COLUMNS, images.size)
    return """<!-- wp:gallery {"ids":[$imageIds],"linkTo":"none"} -->
        |<figure class="wp-block-gallery columns-$columns is-cropped">
        |<ul class="blocks-gallery-grid">$galleryImages</ul></figure><!-- /wp:gallery -->""".trimMargin()
}

private fun galleryImage(image: MediaModel) =
        """<li class="blocks-gallery-item"><figure>
            |<img src="${image.url}" data-id="${image.mediaId}" class="wp-image-${image.mediaId}"/>
            |</figure></li>""".trimMargin()

fun ViewGroup.findByContentDescription(contentDescription: String): View? {
    for (i in 0 until childCount) {
        val v = getChildAt(i)
        if (v.contentDescription == contentDescription) {
            return v
        } else if (v is ViewGroup) {
            return v.findByContentDescription(contentDescription)
        }
    }
    return null
}
