package org.wordpress.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.ui.reader.utils.ReaderHtmlUtils

class ReaderHtmlUtilsTest {
    @Test
    fun `getOriginalHeightAttrValue extracts original height data correctly`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" data-orig-size=\"500,1000\" " +
                "height=\"1000\" width=\"500\">"
        val result = ReaderHtmlUtils.getOriginalHeightAttrValue(testTag)
        assertEquals(1000, result)
    }

    @Test
    fun `getOriginalHeightAttrValue returns 0 if tag is missing original size attr`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" height=\"1000\" width=\"500\">"
        val result = ReaderHtmlUtils.getOriginalHeightAttrValue(testTag)
        assertEquals(0, result)
    }

    @Test
    fun `getOriginalWidthAttrValue extracts original width data correctly`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" data-orig-size=\"500,1000\" " +
                "height=\"1000\" width=\"500\">"
        val result = ReaderHtmlUtils.getOriginalWidthAttrValue(testTag)
        assertEquals(500, result)
    }

    @Test
    fun `getOriginalWidthAttrValue returns 0 if tag is missing original size attr`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" height=\"1000\" width=\"500\">"
        val result = ReaderHtmlUtils.getOriginalWidthAttrValue(testTag)
        assertEquals(0, result)
    }

    @Test
    fun `getHeightAttrValue extracts height attribute data correctly`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" height=\"1000\" width=\"500\">"
        val result = ReaderHtmlUtils.getHeightAttrValue(testTag)
        assertEquals(1000, result)
    }

    @Test
    fun `getHeightAttrValue returns 0 if tag is missing height attr`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" width=\"500\">"
        val result = ReaderHtmlUtils.getHeightAttrValue(testTag)
        assertEquals(0, result)
    }

    @Test
    fun `getWidthAttrValue extracts width attribute data correctly`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" height=\"1000\" width=\"500\">"
        val result = ReaderHtmlUtils.getWidthAttrValue(testTag)
        assertEquals(500, result)
    }

    @Test
    fun `getWidthAttrValue returns 0 if tag is missing width attr`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10\" height=\"1000\">"
        val result = ReaderHtmlUtils.getWidthAttrValue(testTag)
        assertEquals(0, result)
    }

    @Test
    fun `getClassAttrValue extracts class attribute data correctly`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" class=\"wp-image-10 size-large example\" width=\"500\">"
        val result = ReaderHtmlUtils.getClassAttrValue(testTag)
        assertEquals("wp-image-10 size-large example", result)
    }

    @Test
    fun `getClassAttrValue returns null if tag is missing class attr`() {
        val testTag = "<img src=\"example.jpg\" alt=\"\" width=\"500\">"
        val result = ReaderHtmlUtils.getClassAttrValue(testTag)
        assertEquals(null, result)
    }

    @Test
    fun `SRCSET_ATTR_PATTERN matches srcset attribute data correctly`() {
        val test = "<img src=\"https://i0.wp.com/image.jpg?resize=525%2C700\" alt=\"\" class=\"wp-image-10\" " +
                "srcset=\"https://i1.wp.com/image-scaled.jpg?resize=768%2C1024 768w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=225%2C300 225w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1152%2C1536 1152w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1536%2C2048 1536w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1920 1920w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1050 1050w\" " +
                "sizes=\"(max-width: 767px) 89vw, (max-width: 1000px) 54vw, (max-width: 1071px) 543px, 580px\">"
        val matcher = ReaderHtmlUtils.SRCSET_ATTR_PATTERN.matcher(test)
        assertTrue(matcher.find())
    }

    @Test
    fun `SRCSET_INNER_PATTERN matches srcset entries correctly`() {
        val test = "https://i1.wp.com/image-scaled.jpg?resize=768%2C1024 768w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=225%2C300 225w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1152%2C1536 1152w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1536%2C2048 1536w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1920 1920w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1050 1050w"
        val matcher = ReaderHtmlUtils.SRCSET_INNER_PATTERN.matcher(test)
        var count = 0
        var lastMatchWidth = ""
        var lastMatchUrl = ""
        while (matcher.find()) {
            count++
            lastMatchUrl = matcher.group(1)
            lastMatchWidth = matcher.group(2)
        }
        assertEquals("1050", lastMatchWidth)
        assertEquals("https://i1.wp.com/image-scaled.jpg?w=1050", lastMatchUrl)
        assertEquals(6, count)
    }

    @Test
    fun `getSrcsetImageForTag returns correct URL when it's first in the list`() {
        val test = "<img src=\"https://i0.wp.com/image.jpg?resize=525%2C700\" alt=\"\" class=\"wp-image-10\" " +
                "srcset=\"https://i1.wp.com/image-scaled.jpg?resize=768%2C1024 768w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=225%2C300 225w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1152%2C1536 1152w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1536%2C2048 1536w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1920 1920w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1050 1050w\" " +
                "sizes=\"(max-width: 767px) 89vw, (max-width: 1000px) 54vw, (max-width: 1071px) 543px, 580px\">"
        val bestUrl = ReaderHtmlUtils.getSrcsetImageForTag(test, 640)
        assertEquals("https://i1.wp.com/image-scaled.jpg?resize=768%2C1024", bestUrl)
    }

    @Test
    fun `getSrcsetImageForTag returns correct URL when larger sizes appear first`() {
        val test = "<img src=\"https://i0.wp.com/image.jpg?resize=525%2C700\" alt=\"\" class=\"wp-image-10\" " +
                "srcset=\"https://i1.wp.com/image-scaled.jpg?resize=225%2C300 225w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1152%2C1536 1152w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=1536%2C2048 1536w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1920 1920w, " +
                "https://i1.wp.com/image-scaled.jpg?resize=768%2C1024 768w, " +
                "https://i1.wp.com/image-scaled.jpg?w=1050 1050w\" " +
                "sizes=\"(max-width: 767px) 89vw, (max-width: 1000px) 54vw, (max-width: 1071px) 543px, 580px\">"
        val bestUrl = ReaderHtmlUtils.getSrcsetImageForTag(test, 640)
        assertEquals("https://i1.wp.com/image-scaled.jpg?resize=768%2C1024", bestUrl)
    }

    @Test
    fun `getSrcsetImageForTag returns null if srcset contains no image large enough`() {
        val test = "<img src=\"https://i0.wp.com/image.jpg?resize=525%2C700\" alt=\"\" class=\"wp-image-10\" " +
                "srcset=\"https://i1.wp.com/image-scaled.jpg?resize=600%2C1024 600w\" " +
                "sizes=\"(max-width: 767px) 89vw, (max-width: 1000px) 54vw, (max-width: 1071px) 543px, 580px\">"
        val bestUrl = ReaderHtmlUtils.getSrcsetImageForTag(test, 640)
        assertEquals(null, bestUrl)
    }

    @Test
    fun `getSrcsetImageForTag returns null if tag is missing srcset attr`() {
        val test = "<img src=\"https://i0.wp.com/image.jpg?resize=525%2C700\" alt=\"\" class=\"wp-image-10\">"
        val bestUrl = ReaderHtmlUtils.getSrcsetImageForTag(test, 640)
        assertEquals(null, bestUrl)
    }
}
