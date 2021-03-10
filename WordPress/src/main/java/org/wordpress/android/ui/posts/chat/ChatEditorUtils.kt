package org.wordpress.android.ui.posts.chat

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.fluxc.model.MediaModel
import kotlin.math.min

private const val MAX_COLUMNS = 3
private const val SPACER_STEP = 30
private const val HEADER_LENGTH_THRESHOLD = 4
private const val QUOTE_PREFIX = ">"
private const val PREFORMATTED_QUOTE = "`"

val String.wordCount: Int
    get() = replace("\n", " ").split(" ").size

val String.isBigText: Boolean
    get() = wordCount > HEADER_LENGTH_THRESHOLD

val String.isWhitespace: Boolean
    get() = length > 0 && trim().isEmpty()

val String.isQuote: Boolean
    get() = startsWith(QUOTE_PREFIX)

val String.isPreformatted: Boolean
    get() = startsWith(PREFORMATTED_QUOTE) && endsWith(PREFORMATTED_QUOTE)

private val String.brForNewLines: String
    get() = this.replace("\n", "<br>")

val String.paragraphBlock: String
    get() = "<!-- wp:paragraph --><p>$brForNewLines</p><!-- /wp:paragraph -->"

val String.headingBlock: String
    get() = "<!-- wp:heading --><h2>$brForNewLines</h2><!-- /wp:heading -->"

val String.quoteBlock: String
    get() = """<!-- wp:quote --><blockquote class="wp-block-quote"><p>${substring(1).brForNewLines}</p>
        |</blockquote><!-- /wp:quote -->""".trimMargin()

val String.preformattedBlock: String
    get() = """<!-- wp:preformatted --><pre class="wp-block-preformatted">
    |${substring(1, length - 1).brForNewLines}</pre><!-- /wp:preformatted -->""".trimMargin()

val String.spacerBlock: String
    get() = """<!-- wp:spacer {"height":${SPACER_STEP * split("\n").size}} -->
        |<div style="height:${SPACER_STEP * split("\n").size}px" aria-hidden="true" class="wp-block-spacer">
        |</div><!-- /wp:spacer -->""".trimMargin()

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
