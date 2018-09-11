package org.wordpress.android.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.fluxc.tools.FormattableMeta
import org.wordpress.android.fluxc.tools.FormattableRange

@RunWith(MockitoJUnitRunner::class)
class FormattableContentUtilsTest {
    private lateinit var content: FormattableContent
    private lateinit var range: FormattableRange
    private lateinit var media: FormattableMedia
    private lateinit var meta: FormattableMeta
    private lateinit var titles: FormattableMeta.Titles
    private lateinit var ids: FormattableMeta.Ids
    private lateinit var links: FormattableMeta.Links

    @Before
    fun setUp() {
        content = FormattableContent()
        range = FormattableRange()
        media = FormattableMedia()
        meta = FormattableMeta()
        titles = FormattableMeta.Titles()
        ids = FormattableMeta.Ids()
        links = FormattableMeta.Links()
    }

    @Test
    fun verifyTextOrEmptyReturnsText() {
        val content = content.copy(text = "ahoj")
        assertEquals("ahoj", content.getTextOrEmpty())
    }

    @Test
    fun verifyTextOrEmptyReturnsEmpty() {
        val content = content.copy(text = null)
        assertEquals("", content.getTextOrEmpty())
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsId() {
        val range = range.copy(siteId = 123L)
        val content = content.copy(ranges = listOf(range))
        assertEquals(123L, content.getRangeSiteIdOrZero(0))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsZeroWhenEmpty() {
        val content = content.copy(ranges = listOf())
        assertEquals(0L, content.getRangeSiteIdOrZero(0))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsZeroWhenNull() {
        val range = range.copy(siteId = null)
        val content = content.copy(ranges = listOf(range))
        assertEquals(0L, content.getRangeSiteIdOrZero(0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsValue() {
        val range = range.copy(value = "example value")
        val content = content.copy(ranges = listOf(range))
        assertEquals("example value", content.getRangeValueOrEmpty(0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsEmptyWhenEmpty() {
        val content = content.copy(ranges = listOf())
        assertEquals("", content.getRangeValueOrEmpty(0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsEmptyWhenNull() {
        val range = range.copy(value = null)
        val content = content.copy(ranges = listOf(range))
        assertEquals("", content.getRangeValueOrEmpty(0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsId() {
        val range = range.copy(id = 123L)
        val content = content.copy(ranges = listOf(range))
        assertEquals(123L, content.getRangeIdOrZero(0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsZeroWhenEmpty() {
        val content = content.copy(ranges = listOf())
        assertEquals(0L, content.getRangeIdOrZero(0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsZeroWhenNull() {
        val range = range.copy(id = null)
        val content = content.copy(ranges = listOf(range))
        assertEquals(0L, content.getRangeIdOrZero(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsUrl() {
        val range = range.copy(url = "http://example.com")
        val content = content.copy(ranges = listOf(range))
        assertEquals("http://example.com", content.getRangeUrlOrEmpty(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenEmpty() {
        val range = range.copy(url = null)
        val content = content.copy(ranges = listOf(range))
        assertEquals("", content.getRangeUrlOrEmpty(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenNull() {
        val content = content.copy(ranges = null)
        assertEquals("", content.getRangeUrlOrEmpty(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenInvalidIndex() {
        val content = content.copy(ranges = listOf(range))
        assertEquals("", content.getRangeUrlOrEmpty(999))
    }

    @Test
    fun verifyRangeOrNullReturnsRange() {
        val content = content.copy(ranges = listOf(range))
        assertEquals(range, content.getRangeOrNull(0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenEmpty() {
        val content = content.copy(ranges = listOf())
        assertEquals(null, content.getRangeOrNull(0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenNull() {
        val content = content.copy(ranges = null)
        assertEquals(null, content.getRangeOrNull(0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenInvalidIndex() {
        val content = content.copy(ranges = listOf(range))
        assertEquals(null, content.getRangeOrNull(999))
    }

    @Test
    fun verifyMediaUrlOrEmptyReturnsUrl() {
        val media = media.copy(url = "http://example.com")
        val content = content.copy(media = listOf(media))
        assertEquals("http://example.com", content.getMediaUrlOrEmpty(0))
    }

    @Test
    fun verifyMediaUrlOrEmptyReturnsEmpty() {
        val content = content.copy(media = listOf())
        assertEquals("", content.getMediaUrlOrEmpty(0))
    }

    @Test
    fun verifyMediaOrNullReturnsMedia() {
        val content = content.copy(media = listOf(media))
        assertEquals(media, content.getMediaOrNull(0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenEmpty() {
        val content = content.copy(media = listOf())
        assertEquals(null, content.getMediaOrNull(0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenNull() {
        val content = content.copy(media = null)
        assertEquals(null, content.getMediaOrNull(0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenInvalidIndex() {
        val content = content.copy(media = listOf(media))
        assertEquals(null, content.getMediaOrNull(999))
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsHome() {
        val titles = titles.copy(home = "example home")
        val meta = meta.copy(titles = titles)
        val content = content.copy(meta = meta)
        assertEquals("example home", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenTitlesNull() {
        val meta = meta.copy(titles = null)
        val content = content.copy(meta = meta)
        assertEquals("", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenMetaNull() {
        val content = content.copy(meta = null)
        assertEquals("", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenHomeNull() {
        val titles = titles.copy(home = null)
        val meta = meta.copy(titles = titles)
        val content = content.copy(meta = meta)
        assertEquals("", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsHome() {
        val links = links.copy(home = "example home")
        val meta = meta.copy(links = links)
        val content = content.copy(meta = meta)
        assertEquals("example home", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenLinksNull() {
        val meta = meta.copy(links = null)
        val content = content.copy(meta = meta)
        assertEquals("", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenMetaNull() {
        val content = content.copy(meta = null)
        assertEquals("", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenHomeNull() {
        val links = links.copy(home = null)
        val meta = meta.copy(links = links)
        val content = content.copy(meta = meta)
        assertEquals("", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsTagline() {
        val titles = titles.copy(tagline = "example tagline")
        val meta = meta.copy(titles = titles)
        val content = content.copy(meta = meta)
        assertEquals("example tagline", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenTitlesNull() {
        val meta = meta.copy(titles = null)
        val content = content.copy(meta = meta)
        assertEquals("", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenMetaNull() {
        val content = content.copy(meta = null)
        assertEquals("", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenTaglineNull() {
        val titles = titles.copy(tagline = null)
        val meta = meta.copy(titles = titles)
        val content = content.copy(meta = meta)
        assertEquals("", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsSiteId() {
        val ids = ids.copy(site = 123L)
        val meta = meta.copy(ids = ids)
        val content = content.copy(meta = meta)
        assertEquals(123L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenNull() {
        val ids = ids.copy(site = null)
        val meta = meta.copy(ids = ids)
        val content = content.copy(meta = meta)
        assertEquals(0L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenIdsNull() {
        val meta = meta.copy(ids = null)
        val content = content.copy(meta = meta)
        assertEquals(0L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenMetaNull() {
        val content = content.copy(meta = null)
        assertEquals(0L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsUserId() {
        val ids = ids.copy(user = 123L)
        val meta = meta.copy(ids = ids)
        val content = content.copy(meta = meta)
        assertEquals(123L, content.getMetaIdsUserIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenNull() {
        val ids = ids.copy(user = null)
        val meta = meta.copy(ids = ids)
        val content = content.copy(meta = meta)
        assertEquals(0L, content.getMetaIdsUserIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenIdsNull() {
        val meta = meta.copy(ids = null)
        val content = content.copy(meta = meta)
        assertEquals(0L, content.getMetaIdsUserIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenMetaNull() {
        val content = content.copy(meta = null)
        assertEquals(0L, content.getMetaIdsUserIdOrZero())
    }
}
