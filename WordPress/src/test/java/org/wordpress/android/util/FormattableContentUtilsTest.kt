package org.wordpress.android.util

import com.nhaarman.mockito_kotlin.whenever
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
    private lateinit var utils: FormattableContentUtils

    @Before
    fun setUp() {
        utils = FormattableContentUtils()
    }

    @Test
    fun verifyTextOrEmptyReturnsText() {
        whenever(content.text).thenReturn("ahoj")
        assertEquals("ahoj", utils.getTextOrEmpty(content))
    }

    @Test
    fun verifyTextOrEmptyReturnsEmpty() {
        whenever(content.text).thenReturn(null)
        assertEquals("", utils.getTextOrEmpty(content))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsId() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.siteId).thenReturn(123L)
        assertEquals(123L, utils.getRangeSiteIdOrZero(content, 0))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsZeroWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals(0L, utils.getRangeSiteIdOrZero(content, 0))
    }

    @Test
    fun verifyRangeSiteIdOrZeroReturnsZeroWhenNull() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.siteId).thenReturn(null)
        assertEquals(0L, utils.getRangeSiteIdOrZero(content, 0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsValue() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.value).thenReturn("example value")
        assertEquals("example value", utils.getRangeValueOrEmpty(content, 0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsEmptyWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals("", utils.getRangeValueOrEmpty(content, 0))
    }

    @Test
    fun verifyRangeValueOrEmptyReturnsEmptyWhenNull() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.value).thenReturn(null)
        assertEquals("", utils.getRangeValueOrEmpty(content, 0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsId() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.id).thenReturn(123L)
        assertEquals(123L, utils.getRangeIdOrZero(content, 0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsZeroWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals(0L, utils.getRangeIdOrZero(content, 0))
    }

    @Test
    fun verifyRangeIdOrZeroReturnsZeroWhenNull() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.id).thenReturn(null)
        assertEquals(0L, utils.getRangeIdOrZero(content, 0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsUrl() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.url).thenReturn("http://example.com")
        assertEquals("http://example.com", utils.getRangeUrlOrEmpty(content, 0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf(range))
        whenever(range.url).thenReturn(null)
        assertEquals("", utils.getRangeUrlOrEmpty(content, 0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenNull() {
        whenever(content.ranges).thenReturn(null)
        assertEquals("", utils.getRangeUrlOrEmpty(content, 0))
    }

    @Test
    fun verifyRangeUrlOrEmptyReturnsEmptyWhenInvalidIndex() {
        whenever(content.ranges).thenReturn(listOf(range))
        assertEquals("", utils.getRangeUrlOrEmpty(content, 999))
    }

    @Test
    fun verifyRangeOrNullReturnsRange() {
        whenever(content.ranges).thenReturn(listOf(range))
        assertEquals(range, utils.getRangeOrNull(content, 0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenEmpty() {
        whenever(content.ranges).thenReturn(listOf())
        assertEquals(null, utils.getRangeOrNull(content, 0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenNull() {
        whenever(content.ranges).thenReturn(null)
        assertEquals(null, utils.getRangeOrNull(content, 0))
    }

    @Test
    fun verifyRangeOrNullReturnsNullWhenInvalidIndex() {
        whenever(content.ranges).thenReturn(listOf(range))
        assertEquals(null, utils.getRangeOrNull(content, 999))
    }

    @Test
    fun verifyMediaUrlOrEmptyReturnsUrl() {
        whenever(content.media).thenReturn(listOf(media))
        whenever(media.url).thenReturn("http://example.com")
        assertEquals("http://example.com", utils.getMediaUrlOrEmpty(content, 0))
    }

    @Test
    fun verifyMediaUrlOrEmptyReturnsEmpty() {
        whenever(content.media).thenReturn(listOf())
        assertEquals("", utils.getMediaUrlOrEmpty(content, 0))
    }

    @Test
    fun verifyMediaOrNullReturnsMedia() {
        whenever(content.media).thenReturn(listOf(media))
        assertEquals(media, utils.getMediaOrNull(content, 0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenEmpty() {
        whenever(content.media).thenReturn(listOf())
        assertEquals(null, utils.getMediaOrNull(content, 0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenNull() {
        whenever(content.media).thenReturn(null)
        assertEquals(null, utils.getMediaOrNull(content, 0))
    }

    @Test
    fun verifyMediaOrNullReturnsNullWhenInvalidIndex() {
        whenever(content.media).thenReturn(listOf(media))
        assertEquals(null, utils.getMediaOrNull(content, 999))
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsHome() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.home).thenReturn("example home")
        assertEquals("example home", utils.getMetaTitlesHomeOrEmpty(content))
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenTitlesNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(null)
        assertEquals("", utils.getMetaTitlesHomeOrEmpty(content))
    }

    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals("", utils.getMetaTitlesHomeOrEmpty(content))
    }
    @Test
    fun verifyMetaTitlesHomeOrEmptyReturnsEmptyWhenHomeNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.home).thenReturn(null)
        assertEquals("", utils.getMetaTitlesHomeOrEmpty(content))
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsHome() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.links).thenReturn(links)
        whenever(links.home).thenReturn("example home")
        assertEquals("example home", utils.getMetaLinksHomeOrEmpty(content))
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenLinksNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.links).thenReturn(null)
        assertEquals("", utils.getMetaLinksHomeOrEmpty(content))
    }

    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals("", utils.getMetaLinksHomeOrEmpty(content))
    }
    @Test
    fun verifyMetaLinksHomeOrEmptyReturnsEmptyWhenHomeNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.links).thenReturn(links)
        whenever(links.home).thenReturn(null)
        assertEquals("", utils.getMetaLinksHomeOrEmpty(content))
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsTagline() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.tagline).thenReturn("example tagline")
        assertEquals("example tagline", utils.getMetaTitlesTaglineOrEmpty(content))
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenTitlesNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(null)
        assertEquals("", utils.getMetaTitlesTaglineOrEmpty(content))
    }

    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals("", utils.getMetaTitlesTaglineOrEmpty(content))
    }
    @Test
    fun verifyMetaTitlesTaglineOrEmptyReturnsEmptyWhenTaglineNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.titles).thenReturn(titles)
        whenever(titles.tagline).thenReturn(null)
        assertEquals("", utils.getMetaTitlesTaglineOrEmpty(content))
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsSiteId() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.site).thenReturn(123L)
        assertEquals(123L, utils.getMetaIdsSiteIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.site).thenReturn(null)
        assertEquals(0L, utils.getMetaIdsSiteIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenIdsNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(null)
        assertEquals(0L, utils.getMetaIdsSiteIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsSiteIdOrZeroReturnsZeroWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals(0L, utils.getMetaIdsSiteIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsUserId() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.user).thenReturn(123L)
        assertEquals(123L, utils.getMetaIdsUserIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(ids)
        whenever(ids.user).thenReturn(null)
        assertEquals(0L, utils.getMetaIdsUserIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenIdsNull() {
        whenever(content.meta).thenReturn(meta)
        whenever(meta.ids).thenReturn(null)
        assertEquals(0L, utils.getMetaIdsUserIdOrZero(content))
    }

    @Test
    fun verifyMetaIdsUserIdOrZeroReturnsZeroWhenMetaNull() {
        whenever(content.meta).thenReturn(null)
        assertEquals(0L, utils.getMetaIdsUserIdOrZero(content))
    }
}
