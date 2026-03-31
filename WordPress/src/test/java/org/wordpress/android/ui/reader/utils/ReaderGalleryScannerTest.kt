package org.wordpress.android.ui.reader.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ReaderGalleryScannerTest {
    @Test
    fun `parseGalleries finds wp-block-gallery`() {
        val html = """
            <figure class="wp-block-gallery columns-3">
                <ul class="blocks-gallery-grid">
                    <li><figure><img src="https://example.com/img1.jpg"></figure></li>
                    <li><figure><img src="https://example.com/img2.jpg"></figure></li>
                    <li><figure><img src="https://example.com/img3.jpg"></figure></li>
                </ul>
            </figure>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(1)
        assertThat(galleries[0]).containsExactly(
            "https://example.com/img1.jpg",
            "https://example.com/img2.jpg",
            "https://example.com/img3.jpg"
        )
    }

    @Test
    fun `parseGalleries finds jetpack tiled gallery`() {
        val html = """
            <div class="wp-block-jetpack-tiled-gallery">
                <img src="https://example.com/tile1.jpg">
                <img src="https://example.com/tile2.jpg">
            </div>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(1)
        assertThat(galleries[0]).containsExactly(
            "https://example.com/tile1.jpg",
            "https://example.com/tile2.jpg"
        )
    }

    @Test
    fun `parseGalleries finds classic tiled-gallery`() {
        val html = """
            <div class="tiled-gallery">
                <div class="gallery-row">
                    <img src="https://example.com/classic1.jpg">
                    <img src="https://example.com/classic2.jpg">
                </div>
            </div>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(1)
        assertThat(galleries[0]).containsExactly(
            "https://example.com/classic1.jpg",
            "https://example.com/classic2.jpg"
        )
    }

    @Test
    fun `parseGalleries finds classic gallery shortcode`() {
        val html = """
            <div class="gallery gallery-columns-3">
                <figure><img src="https://example.com/sc1.jpg"></figure>
                <figure><img src="https://example.com/sc2.jpg"></figure>
            </div>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(1)
        assertThat(galleries[0]).containsExactly(
            "https://example.com/sc1.jpg",
            "https://example.com/sc2.jpg"
        )
    }

    @Test
    fun `parseGalleries finds multiple galleries in one post`() {
        val html = """
            <figure class="wp-block-gallery">
                <img src="https://example.com/g1-img1.jpg">
                <img src="https://example.com/g1-img2.jpg">
            </figure>
            <p>Some text between galleries</p>
            <div class="wp-block-gallery">
                <img src="https://example.com/g2-img1.jpg">
                <img src="https://example.com/g2-img2.jpg">
                <img src="https://example.com/g2-img3.jpg">
            </div>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(2)
        assertThat(galleries[0]).hasSize(2)
        assertThat(galleries[1]).hasSize(3)
    }

    @Test
    fun `parseGalleries returns empty for no galleries`() {
        val html = """
            <p>Just a paragraph</p>
            <img src="https://example.com/standalone.jpg">
        """.trimIndent()

        assertThat(ReaderGalleryScanner.parseGalleries(html)).isEmpty()
    }

    @Test
    fun `parseGalleries returns empty for null or blank html`() {
        assertThat(ReaderGalleryScanner.parseGalleries(null)).isEmpty()
        assertThat(ReaderGalleryScanner.parseGalleries("  ")).isEmpty()
    }

    @Test
    fun `parseGalleries skips gallery with no images`() {
        val html = """
            <figure class="wp-block-gallery">
                <figcaption>Empty gallery</figcaption>
            </figure>
        """.trimIndent()

        assertThat(ReaderGalleryScanner.parseGalleries(html)).isEmpty()
    }

    @Test
    fun `parseGalleries skips images without valid src`() {
        val html = """
            <figure class="wp-block-gallery">
                <img>
                <img src="">
                <img src="data:image/png;base64,abc">
                <img src="file:///local/image.jpg">
                <img src="https://example.com/valid.jpg">
            </figure>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(1)
        assertThat(galleries[0]).containsExactly(
            "https://example.com/valid.jpg"
        )
    }

    @Test
    fun `findGalleryContaining matches image in correct gallery`() {
        val galleries = listOf(
            listOf(
                "https://example.com/g1-img1.jpg",
                "https://example.com/g1-img2.jpg"
            ),
            listOf(
                "https://example.com/g2-img1.jpg",
                "https://example.com/g2-img2.jpg",
                "https://example.com/g2-img3.jpg"
            )
        )

        val match = ReaderGalleryScanner.findGalleryContaining(
            galleries, "https://example.com/g2-img2.jpg"
        )

        assertThat(match).isNotNull
        assertThat(match).hasSize(3)
    }

    @Test
    fun `findGalleryContaining returns null when image not in any gallery`() {
        val galleries = listOf(
            listOf("https://example.com/g1-img1.jpg")
        )

        val match = ReaderGalleryScanner.findGalleryContaining(
            galleries, "https://example.com/standalone.jpg"
        )

        assertThat(match).isNull()
    }

    @Test
    fun `findGalleryContaining normalizes URLs with query params`() {
        val galleries = listOf(
            listOf(
                "https://example.com/img1.jpg?w=300&h=200",
                "https://example.com/img2.jpg"
            )
        )

        val match = ReaderGalleryScanner.findGalleryContaining(
            galleries, "https://example.com/img1.jpg?w=600&h=400"
        )

        assertThat(match).isNotNull
        assertThat(match).hasSize(2)
    }

    @Test
    fun `nested galleries are not double-counted`() {
        val html = """
            <figure class="wp-block-gallery">
                <div class="wp-block-gallery">
                    <img src="https://example.com/nested.jpg">
                </div>
            </figure>
        """.trimIndent()

        val galleries = ReaderGalleryScanner.parseGalleries(html)

        assertThat(galleries).hasSize(1)
        assertThat(galleries[0]).containsExactly(
            "https://example.com/nested.jpg"
        )
    }
}
