package org.wordpress.android.util

import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

private const val VALID_URL = "VALID_URL"
private const val INVALID_URL = "INVALID_URL"
private const val VALID_IMAGE_URL = "VALID_IMAGE_URL"
private const val INVALID_IMAGE_URL = "INVALID_IMAGE_URL"

/**
 * Implements tests for ReblogUtils
 */
@RunWith(MockitoJUnitRunner::class)
class ReblogUtilsTest {
    @Mock private lateinit var urlUtils: UrlUtilsWrapper
    private lateinit var reblogUtils: ReblogUtils

    @Before
    fun setup() {
        reblogUtils = ReblogUtils((urlUtils))
        whenever(urlUtils.isValidUrlAndHostNotNull(VALID_URL)).thenReturn(true)
        whenever(urlUtils.isValidUrlAndHostNotNull(INVALID_URL)).thenReturn(false)
        whenever(urlUtils.isImageUrl(VALID_IMAGE_URL)).thenReturn(true)
        whenever(urlUtils.isImageUrl(INVALID_IMAGE_URL)).thenReturn(false)
    }

    @Test
    fun `embedded quote`() {
        val expected = "<blockquote>some quote</blockquote>"
        val actual = reblogUtils.embeddedQuote("some quote")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty quote`() {
        val expected = "<blockquote></blockquote>"
        val actual = reblogUtils.embeddedQuote("")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded WP quote`() {
        val expected = """<!-- wp:quote --><blockquote class="wp-block-quote">quote</blockquote><!-- /wp:quote -->"""
        val actual = reblogUtils.embeddedWpQuote("quote")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty WP quote`() {
        val expected = """<!-- wp:quote --><blockquote class="wp-block-quote"></blockquote><!-- /wp:quote -->"""
        val actual = reblogUtils.embeddedWpQuote("")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded citation`() {
        val expected = "<cite>some citation</cite>"
        val actual = reblogUtils.embeddedCitation("some citation")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty citation`() {
        val expected = "<cite></cite>"
        val actual = reblogUtils.embeddedCitation("")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded image`() {
        val expected = """<img src="$VALID_IMAGE_URL">"""
        val actual = reblogUtils.htmlImage(VALID_IMAGE_URL)
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded image with invalid image url`() {
        assertNull(reblogUtils.htmlImage(INVALID_IMAGE_URL))
    }

    @Test
    fun `embedded WP image`() {
        val expected = """<!-- wp:image --><figure class="wp-block-image"><img src="$VALID_IMAGE_URL">""" +
                "</figure><!-- /wp:image -->"
        val actual = reblogUtils.htmlWpImage(VALID_IMAGE_URL)
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded WP image with invalid image url`() {
        assertNull(reblogUtils.htmlWpImage(INVALID_IMAGE_URL))
    }

    @Test
    fun `embedded paragraph`() {
        val expected = "<p>some paragraph</p>"
        val actual = reblogUtils.htmlParagraph("some paragraph")
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty paragraph`() {
        val expected = "<p></p>"
        val actual = reblogUtils.htmlParagraph("")
        assertEquals(expected, actual)
    }

    @Test
    fun `create hyperlink with valid url and text`() {
        val expected = """<a href="$VALID_URL">some text</a>"""
        val actual = reblogUtils.hyperLink(VALID_URL, "some text")
        assertEquals(expected, actual)
    }

    @Test
    fun `create hyperlink with valid url without a text`() {
        val expected = """<a href="$VALID_URL">$VALID_URL</a>"""
        val actual = reblogUtils.hyperLink(VALID_URL)
        assertEquals(expected, actual)
    }

    @Test
    fun `create hyperlink with invalid url`() {
        assertNull(reblogUtils.hyperLink(INVALID_URL, "not important"))
    }

    @Test
    fun `create quote without citation`() {
        val expected = "<p>some quote</p>"
        val actual = reblogUtils.quoteWithCitation("some quote")
        assertEquals(expected, actual)
    }

    @Test
    fun `create quote with citation having a valid url but no text`() {
        val expected = """<p>some quote</p><cite><a href="$VALID_URL">$VALID_URL</a></cite>"""
        val actual = reblogUtils.quoteWithCitation("some quote", VALID_URL)
        assertEquals(expected, actual)
    }

    @Test
    fun `create quote with citation having a valid url and text`() {
        val expected = """<p>some quote</p><cite><a href="$VALID_URL">some text</a></cite>"""
        val actual = reblogUtils.quoteWithCitation("some quote", VALID_URL, "some text")
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and a valid image for Gutenberg editor`() {
        val expected = """<!-- wp:image --><figure class="wp-block-image">""" +
                """<img src="$VALID_IMAGE_URL"></figure><!-- /wp:image -->""" +
                """<!-- wp:quote --><blockquote class="wp-block-quote"><p>some quote</p>""" +
                """<cite><a href="$VALID_URL">some text</a></cite></blockquote><!-- /wp:quote -->"""
        val actual = reblogUtils.reblogContent(
                VALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                true
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and an invalid image for Gutenberg editor`() {
        val expected = """<!-- wp:quote --><blockquote class="wp-block-quote"><p>some quote</p>""" +
                """<cite><a href="$VALID_URL">some text</a></cite></blockquote><!-- /wp:quote -->"""
        val actual = reblogUtils.reblogContent(
                INVALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                true
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and a valid image for Aztec editor`() {
        val expected = """<p><img src="$VALID_IMAGE_URL"></p>""" +
                """<blockquote><p>some quote</p><cite><a href="$VALID_URL">some text</a></cite></blockquote>"""
        val actual = reblogUtils.reblogContent(
                VALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                false
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and an invalid image for Aztec editor`() {
        val expected = """<blockquote><p>some quote</p><cite><a href="$VALID_URL">some text</a></cite></blockquote>"""
        val actual = reblogUtils.reblogContent(
                INVALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                false
        )
        assertEquals(expected, actual)
    }
}
