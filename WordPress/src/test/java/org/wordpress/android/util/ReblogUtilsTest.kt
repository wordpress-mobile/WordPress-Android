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

    @Before
    fun setup() {
        whenever(urlUtils.isValidUrlAndHostNotNull(VALID_URL)).thenReturn(true)
        whenever(urlUtils.isValidUrlAndHostNotNull(INVALID_URL)).thenReturn(false)
        whenever(urlUtils.isImageUrl(VALID_IMAGE_URL)).thenReturn(true)
        whenever(urlUtils.isImageUrl(INVALID_IMAGE_URL)).thenReturn(false)
    }

    @Test
    fun `embedded quote`() {
        val expected = "<blockquote>some quote</blockquote>"
        val actual = "some quote".embeddedQuote
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty quote`() {
        val expected = "<blockquote></blockquote>"
        val actual = "".embeddedQuote
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded WP quote`() {
        val expected = """<!-- wp:quote --><blockquote class="wp-block-quote">quote</blockquote><!-- /wp:quote -->"""
        val actual = "quote".embeddedWpQuote
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty WP quote`() {
        val expected = """<!-- wp:quote --><blockquote class="wp-block-quote"></blockquote><!-- /wp:quote -->"""
        val actual = "".embeddedWpQuote
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded citation`() {
        val expected = "<cite>some citation</cite>"
        val actual = "some citation".embeddedCitation
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty citation`() {
        val expected = "<cite></cite>"
        val actual = "".embeddedCitation
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded image`() {
        val expected = """<img src="$VALID_IMAGE_URL">"""
        val actual = htmlImage(VALID_IMAGE_URL, urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded image with invalid image url`() {
        assertNull(htmlImage(INVALID_IMAGE_URL, urlUtils))
    }

    @Test
    fun `embedded WP image`() {
        val expected = """<!-- wp:image --><figure class="wp-block-image"><img src="$VALID_IMAGE_URL">""" +
                "</figure><!-- /wp:image -->"
        val actual = htmlWpImage(VALID_IMAGE_URL, urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded WP image with invalid image url`() {
        assertNull(htmlWpImage(INVALID_IMAGE_URL, urlUtils))
    }

    @Test
    fun `embedded paragraph`() {
        val expected = "<p>some paragraph</p>"
        val actual = "some paragraph".htmlParagraph
        assertEquals(expected, actual)
    }

    @Test
    fun `embedded empty paragraph`() {
        val expected = "<p></p>"
        val actual = "".htmlParagraph
        assertEquals(expected, actual)
    }

    @Test
    fun `create hyperlink with valid url and text`() {
        val expected = """<a href="$VALID_URL">some text</a>"""
        val actual = hyperLink(VALID_URL, "some text", urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `create hyperlink with valid url without a text`() {
        val expected = """<a href="$VALID_URL">$VALID_URL</a>"""
        val actual = hyperLink(VALID_URL, urlUtils = urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `create hyperlink with invalid url`() {
        assertNull(hyperLink(INVALID_URL, "not important", urlUtils))
    }

    @Test
    fun `create quote without citation`() {
        val expected = "<p>some quote</p>"
        val actual = quoteWithCitation("some quote", urlUtils = urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `create quote with citation having a valid url but no text`() {
        val expected = """<p>some quote</p><cite><a href="$VALID_URL">$VALID_URL</a></cite>"""
        val actual = quoteWithCitation("some quote", VALID_URL, urlUtils = urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `create quote with citation having a valid url and text`() {
        val expected = """<p>some quote</p><cite><a href="$VALID_URL">some text</a></cite>"""
        val actual = quoteWithCitation("some quote", VALID_URL, "some text", urlUtils)
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and a valid image for Gutenberg editor`() {
        val expected = """<!-- wp:image --><figure class="wp-block-image">""" +
                """<img src="$VALID_IMAGE_URL"></figure><!-- /wp:image -->""" +
                """<!-- wp:quote --><blockquote class="wp-block-quote"><p>some quote</p>""" +
                """<cite><a href="$VALID_URL">some text</a></cite></blockquote><!-- /wp:quote -->"""
        val actual = reblogContent(
                VALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                true,
                urlUtils
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and an invalid image for Gutenberg editor`() {
        val expected = """<!-- wp:quote --><blockquote class="wp-block-quote"><p>some quote</p>""" +
                """<cite><a href="$VALID_URL">some text</a></cite></blockquote><!-- /wp:quote -->"""
        val actual = reblogContent(
                INVALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                true,
                urlUtils
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and a valid image for Aztec editor`() {
        val expected = """<p><img src="$VALID_IMAGE_URL"></p>""" +
                """<blockquote><p>some quote</p><cite><a href="$VALID_URL">some text</a></cite></blockquote>"""
        val actual = reblogContent(
                VALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                false,
                urlUtils
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `create reblog content with quote, citation and an invalid image for Aztec editor`() {
        val expected = """<blockquote><p>some quote</p><cite><a href="$VALID_URL">some text</a></cite></blockquote>"""
        val actual = reblogContent(
                INVALID_IMAGE_URL,
                "some quote",
                "some text",
                VALID_URL,
                false,
                urlUtils
        )
        assertEquals(expected, actual)
    }
}
