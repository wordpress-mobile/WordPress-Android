package org.wordpress.android.fluxc.network.rest.wpapi.site

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteOptionsRsFetcher.Companion.commentStatusSlug
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteOptionsRsFetcher.Companion.postThumbnailsSupport
import uniffi.wp_api.SiteSettingsCommentStatus
import uniffi.wp_api.ThemeSupports
import uniffi.wp_api.ThemeSupportsData

class SiteOptionsRsFetcherTest {
    @Test
    fun `post-thumbnails reported as a boolean is read as that boolean`() {
        assertThat(postThumbnailsSupport(mapOf(ThemeSupports.PostThumbnails to ThemeSupportsData.Bool(true))))
            .isTrue()
        assertThat(postThumbnailsSupport(mapOf(ThemeSupports.PostThumbnails to ThemeSupportsData.Bool(false))))
            .isFalse()
    }

    @Test
    fun `post-thumbnails reported as a post type list counts as supported`() {
        val support = ThemeSupportsData.VecString(listOf("post", "page"))

        assertThat(postThumbnailsSupport(mapOf(ThemeSupports.PostThumbnails to support))).isTrue()
    }

    @Test
    fun `post-thumbnails reported as an empty post type list counts as unsupported`() {
        val support = ThemeSupportsData.VecString(emptyList())

        assertThat(postThumbnailsSupport(mapOf(ThemeSupports.PostThumbnails to support))).isFalse()
    }

    @Test
    fun `an absent post-thumbnails key is unknown rather than unsupported`() {
        // Guessing "unsupported" would switch the editor's featured-image controls off, so the
        // stored value has to be kept instead.
        assertThat(postThumbnailsSupport(emptyMap())).isNull()
        assertThat(postThumbnailsSupport(mapOf(ThemeSupports.Formats to ThemeSupportsData.Bool(true)))).isNull()
    }

    @Test
    fun `no theme supports at all is unknown`() {
        assertThat(postThumbnailsSupport(null)).isNull()
    }

    @Test
    fun `comment status maps to the slug SiteModel stores`() {
        assertThat(commentStatusSlug(SiteSettingsCommentStatus.Open)).isEqualTo("open")
        assertThat(commentStatusSlug(SiteSettingsCommentStatus.Closed)).isEqualTo("closed")
    }

    @Test
    fun `a custom comment status keeps its own slug`() {
        assertThat(commentStatusSlug(SiteSettingsCommentStatus.Custom("moderated"))).isEqualTo("moderated")
    }

    @Test
    fun `an empty custom comment status is unknown rather than empty`() {
        assertThat(commentStatusSlug(SiteSettingsCommentStatus.Custom(""))).isNull()
    }
}
