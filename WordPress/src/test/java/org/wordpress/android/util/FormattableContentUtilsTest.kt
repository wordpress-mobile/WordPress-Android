package org.wordpress.android.util

import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.fluxc.tools.FormattableMeta
import org.wordpress.android.fluxc.tools.FormattableRange

@RunWith(MockitoJUnitRunner::class)
class FormattableContentUtilsTest {
    @Mock
    lateinit var content: FormattableContent
    @Mock
    lateinit var range: FormattableRange
    @Mock
    lateinit var media: FormattableMedia
    @Mock
    lateinit var titles: FormattableMeta.Titles
    @Mock
    lateinit var links: FormattableMeta.Links
    @Mock
    lateinit var ids: FormattableMeta.Ids
    @Mock
    lateinit var meta: FormattableMeta

    @Before
    fun setUp() {
    }

    @Test
    fun verifyTextOrEmptyReturnsText() {
        whenever(content.text).thenReturn("ahoj")
        assertEquals("ahoj", content.getTextOrEmpty())
    }

    @Test
    fun verifyTextOrEmptyReturnsEmpty() {
        whenever(content.text).thenReturn(null)
        assertEquals("", content.getTextOrEmpty())
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsId() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.siteId).thenReturn(123L)
        assertEquals(123L, content.getRangeSiteIdOrZero(0))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsZeroWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals(0L, content.getRangeSiteIdOrZero(0))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsZeroWhenNull() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.siteId).thenReturn(null)
        assertEquals(0L, content.getRangeSiteIdOrZero(0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsValue() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.value).thenReturn("example value")
        assertEquals("example value", content.getRangeValueOrEmpty(0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsEmptyWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals("", content.getRangeValueOrEmpty(0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsEmptyWhenNull() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.value).thenReturn(null)
        assertEquals("", content.getRangeValueOrEmpty(0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsId() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.id).thenReturn(123L)
        assertEquals(123L, content.getRangeIdOrZero(0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsZeroWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals(0L, content.getRangeIdOrZero(0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsZeroWhenNull() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.id).thenReturn(null)
        assertEquals(0L, content.getRangeIdOrZero(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsUrl() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.url).thenReturn("http://example.com")
        assertEquals("http://example.com", content.getRangeUrlOrEmpty(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.url).thenReturn(null)
        assertEquals("", content.getRangeUrlOrEmpty(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenNull() {
        whenever(content.ranges).thenReturn(null)
        assertEquals("", content.getRangeUrlOrEmpty(0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenInvalidIndex() {
        whenever(content.ranges).thenReturn(listOf(range))
        assertEquals("", content.getRangeUrlOrEmpty(999))
    }

    @Test
    fun verifyRangeOrNullReturnsRange() {
        whenever(content.ranges).thenReturn(listOf(range))
        assertEquals(range, content.getRangeOrNull(0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals(null, content.getRangeOrNull(0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenNull() {
        whenever(content.ranges).thenReturn(null)
        assertEquals(null, content.getRangeOrNull(0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenInvalidIndex() {
        whenever(content.ranges).thenReturn(listOf(range))
        assertEquals(null, content.getRangeOrNull(999))
    }

    @Test
    fun verifyMediaUrlOrEmptyReturnsUrl() {
        whenever(content.media).thenReturn(listOf(media))
        whenever(media.url).thenReturn("http://example.com")
        assertEquals("http://example.com", content.getMediaUrlOrEmpty(0))
    }

    @Test
    fun verifyMediaUrlOrEmptyReturnsEmpty() {
        whenever(content.media).thenReturn(listOf())
        assertEquals("", content.getMediaUrlOrEmpty(0))
    }

    @Test
    fun verifyMediaOrNullReturnsMedia() {
        whenever(content.media).thenReturn(listOf(media))
        assertEquals(media, content.getMediaOrNull(0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenEmpty() {
        whenever(content.media).thenReturn(listOf())
        assertEquals(null, content.getMediaOrNull(0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenNull() {
        whenever(content.media).thenReturn(null)
        assertEquals(null, content.getMediaOrNull(0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenInvalidIndex() {
        whenever(content.media).thenReturn(listOf(media))
        assertEquals(null, content.getMediaOrNull(999))
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsHome() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.home).thenReturn("example home")
        assertEquals("example home", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenTitlesNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(null)
        assertEquals("", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals("", content.getMetaTitlesHomeOrEmpty())
    }
    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenHomeNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.home).thenReturn(null)
        assertEquals("", content.getMetaTitlesHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsHome() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.links).thenReturn(links)
        whenever(links.home).thenReturn("example home")
        assertEquals("example home", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenLinksNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.links).thenReturn(null)
        assertEquals("", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals("", content.getMetaLinksHomeOrEmpty())
    }
    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenHomeNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.links).thenReturn(links)
        whenever(links.home).thenReturn(null)
        assertEquals("", content.getMetaLinksHomeOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsTagline() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.tagline).thenReturn("example tagline")
        assertEquals("example tagline", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenTitlesNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(null)
        assertEquals("", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals("", content.getMetaTitlesTaglineOrEmpty())
    }
    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenTaglineNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.tagline).thenReturn(null)
        assertEquals("", content.getMetaTitlesTaglineOrEmpty())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsSiteId() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.site).thenReturn(123L)
        assertEquals(123L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.site).thenReturn(null)
        assertEquals(0L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenIdsNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(null)
        assertEquals(0L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals(0L, content.getMetaIdsSiteIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsUserId() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.user).thenReturn(123L)
        assertEquals(123L, content.getMetaIdsUserIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.user).thenReturn(null)
        assertEquals(0L, content.getMetaIdsUserIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenIdsNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(null)
        assertEquals(0L, content.getMetaIdsUserIdOrZero())
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals(0L, content.getMetaIdsUserIdOrZero())
    }
}
