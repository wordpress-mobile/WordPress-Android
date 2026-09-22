package org.wordpress.android.ui.postsrs.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.ui.reader.utils.SiteAccessibilityInfo
import org.wordpress.android.ui.reader.utils.SiteVisibility

/** Robolectric because the photon rewrite goes through android.net.Uri. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MediaDisplayUrlTest {
    @Test
    fun `a photon site crops server-side to the shape of the slot`() {
        val url = PHOTO.toDisplayUrl(PUBLIC, isWpComRest = true, widthPx = 1080, heightPx = 390)

        assertThat(url).contains("resize=1080,390")
    }

    @Test
    fun `a photon site asks for width alone when the row is not cropping`() {
        val url = PHOTO.toDisplayUrl(PUBLIC, isWpComRest = true, widthPx = 216, heightPx = 0)

        assertThat(url).contains("w=216")
        assertThat(url).doesNotContain("resize=")
    }

    @Test
    fun `a private site is never sent a height, since w and h there bound rather than crop`() {
        val url = PHOTO.toDisplayUrl(PRIVATE, isWpComRest = true, widthPx = 1080, heightPx = 390)

        assertThat(url).contains("w=1080")
        assertThat(url).doesNotContain("h=")
    }

    @Test
    fun `the two shapes of one image resolve to different renders`() {
        val thumbnail = PHOTO.toDisplayUrl(SELF_HOSTED, false, widthPx = 216, heightPx = 216)
        val hero = PHOTO.toDisplayUrl(
            SELF_HOSTED, false, widthPx = 1080, heightPx = 390,
            maxRenderWidthPx = MAX_HERO_RENDER_WIDTH_PX
        )

        // 300x225 gives a square slot 225px, enough for 216. The hero asks wider than any render,
        // but the search caps at 1024, so it settles on `large` instead of the full-size upload.
        assertThat(thumbnail).isEqualTo(MEDIUM_URL)
        assertThat(hero).isEqualTo(LARGE_URL)
    }

    @Test
    fun `a hard-cropped render is rejected unless the slot is that same shape`() {
        val uncropped = PHOTO.toDisplayUrl(SELF_HOSTED, false, widthPx = 144, heightPx = 0)
        val square = PHOTO.toDisplayUrl(SELF_HOSTED, false, widthPx = 144, heightPx = 144)

        // 150x150 is wide enough for both, but it is a square crop of a 4:3 photo.
        assertThat(uncropped).isEqualTo(MEDIUM_URL)
        assertThat(square).isEqualTo(THUMBNAIL_URL)
    }

    companion object {
        private const val UPLOADS = "https://example.com/wp-content/uploads"
        private const val SOURCE_URL = "$UPLOADS/photo.jpg"
        private const val THUMBNAIL_URL = "$UPLOADS/photo-150x150.jpg"
        private const val MEDIUM_URL = "$UPLOADS/photo-300x225.jpg"
        private const val LARGE_URL = "$UPLOADS/photo-1024x768.jpg"

        /** A 4:3 upload with WordPress's default sizes. Only `thumbnail` is hard-cropped. */
        private val PHOTO = MediaImage(
            sourceUrl = SOURCE_URL,
            sourceWidth = 2000,
            sourceHeight = 1500,
            sizes = listOf(
                ScaledSize(150, 150, THUMBNAIL_URL),
                ScaledSize(300, 225, MEDIUM_URL),
                ScaledSize(1024, 768, LARGE_URL),
            )
        )

        private val PUBLIC = SiteAccessibilityInfo(SiteVisibility.PUBLIC, isPhotonCapable = true)
        private val PRIVATE = SiteAccessibilityInfo(SiteVisibility.PRIVATE, isPhotonCapable = false)
        private val SELF_HOSTED =
            SiteAccessibilityInfo(SiteVisibility.PUBLIC, isPhotonCapable = false)
    }
}
