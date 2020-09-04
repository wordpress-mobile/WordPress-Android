package org.wordpress.android.util

import org.junit.Assert.assertEquals
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
}
