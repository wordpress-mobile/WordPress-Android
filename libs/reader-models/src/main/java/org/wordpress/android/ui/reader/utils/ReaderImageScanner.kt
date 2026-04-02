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
     * returns true if there is at least `minImageCount` images in the post content that are at
     * least `minImageWidth` in size
     */
    fun hasUsableImageCount(minImageCount: Int, minImageWidth: Int): Boolean {
        return getImageList(minImageCount, minImageWidth).size == minImageCount
    }

    companion object {
        private val IMG_TAG_PATTERN: Pattern = Pattern.compile(
            "<img[^>]* src=\\\"([^\\\"]*)\\\"[^>]*>",
            Pattern.CASE_INSENSITIVE
        )
    }
}
