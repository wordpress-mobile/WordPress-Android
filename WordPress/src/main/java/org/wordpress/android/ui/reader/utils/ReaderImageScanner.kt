package org.wordpress.android.ui.reader.utils

import org.wordpress.android.ui.reader.models.ReaderImageList
import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils.HtmlScannerListener
import java.util.regex.Pattern
import kotlin.math.max

class ReaderImageScanner(private val content: String, private val isPrivate: Boolean) {
    private val contentContainsImages =
        content.contains("<img")

    /*
    * start scanning the content for images and notify the passed listener about each one
    */
    fun beginScan(listener: HtmlScannerListener) {
        if (!contentContainsImages) {
            return
        }

        val imgMatcher = IMG_TAG_PATTERN.matcher(content)
        while (imgMatcher.find()) {
            val imageTag = imgMatcher.group(0).orEmpty()
            val imageUrl = imgMatcher.group(1).orEmpty()
            listener.onTagFound(imageTag, imageUrl)
        }
    }

    /*
     * returns a list of image URLs in the content up to the max above a certain width.
     * pass zero as the count to include all images regardless of size.
     */
    @Suppress("NestedBlockDepth")
    fun getImageList(maxImageCount: Int, minImageWidth: Int): ReaderImageList {
        val imageList = ReaderImageList(isPrivate)

        if (!contentContainsImages) {
            return imageList
        }

        val imgMatcher = IMG_TAG_PATTERN.matcher(content)
        while (imgMatcher.find()) {
            val imageTag = imgMatcher.group(0).orEmpty()
            val imageUrl = imgMatcher.group(1).orEmpty()

            if (minImageWidth == 0) {
                imageList.addImageUrl(imageUrl)
            } else {
                val width = max(
                    ReaderHtmlUtils.getWidthAttrValue(imageTag).toDouble(),
                    ReaderHtmlUtils.getIntQueryParam(imageUrl, "w").toDouble()
                ).toInt()
                if (width >= minImageWidth) {
                    imageList.addImageUrl(imageUrl)
                    if (maxImageCount > 0 && imageList.size >= maxImageCount) {
                        break
                    }
                }
            }
        }

        return imageList
    }

    /*
     * returns true if there at least `minImageCount` images in the post content that are at
     * least `minImageWidth` in size
     */
    fun hasUsableImageCount(minImageCount: Int, minImageWidth: Int): Boolean {
        return getImageList(minImageCount, minImageWidth).size == minImageCount
    }

    /*
     * used when a post doesn't have a featured image assigned, searches post's content
     * for an image that may be large enough to be suitable as a featured image
     */
    fun getLargestImage(minImageWidth: Int): String? {
        if (!contentContainsImages) {
            return null
        }

        var currentImageUrl: String? = null
        var currentMaxWidth = minImageWidth

        val imgMatcher = IMG_TAG_PATTERN.matcher(content)
        while (imgMatcher.find()) {
            val imageTag = imgMatcher.group(0).orEmpty()
            val imageUrl = imgMatcher.group(1).orEmpty()

            // Primary source: check the width attribute.
            val width = max(
                ReaderHtmlUtils.getWidthAttrValue(imageTag).toDouble(),
                ReaderHtmlUtils.getIntQueryParam(imageUrl, "w").toDouble()
            ).toInt()
            if (width > currentMaxWidth) {
                currentImageUrl = imageUrl
                currentMaxWidth = width
            }

            // Look through the srcset attribute (if set) for the largest available size of this image.
            ReaderHtmlUtils.getLargestSrcsetImageForTag(imageTag)?.let { bestFromSrcset ->
                if (bestFromSrcset.width > currentMaxWidth) {
                    currentMaxWidth = bestFromSrcset.width
                    currentImageUrl = bestFromSrcset.url
                }
            }

            // Check if the image tag's class suggests it's a good enough size.
            // Only do this if we don't already have a winner, since we can't be sure of the width
            // and shouldn't replace an image we know for sure is larger than [minImageWidth].
            if (currentImageUrl == null && hasSuitableClassForFeaturedImage(imageTag)) {
                currentImageUrl = imageUrl
            }

            // Look for a data-large-file attribute if set and use the associated url.
            // Only do this if we don't already have a winner, since we can't be sure of the width
            // and shouldn't replace an image we know for sure is larger than [minImageWidth].
            if (currentImageUrl == null) {
                currentImageUrl = ReaderHtmlUtils.getLargeFileAttr(imageTag)
            }
        }

        return currentImageUrl
    }

    /*
     * returns true if the passed image tag has a "size-" class attribute which would make it
     * suitable for use as a featured image
     */
    private fun hasSuitableClassForFeaturedImage(imageTag: String): Boolean {
        val tagClass = ReaderHtmlUtils.getClassAttrValue(imageTag)
        return (tagClass != null
                && (tagClass.contains("size-full")
                || tagClass.contains("size-large")
                || tagClass.contains("size-medium")))
    }

    companion object {
        private val IMG_TAG_PATTERN: Pattern = Pattern.compile(
            "<img[^>]* src=\\\"([^\\\"]*)\\\"[^>]*>",
            Pattern.CASE_INSENSITIVE
        )
    }
}
