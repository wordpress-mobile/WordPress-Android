package org.wordpress.android.ui.comments.unified.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for the [CommentHtmlBody] parsing helpers. Robolectric is required because
 * both helpers ultimately reach [android.text.Html]/HtmlCompat, which aren't stubbed on the plain
 * JVM. These pin the segment-splitting boundaries and the link/whitespace handling that the
 * composable relies on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class CommentHtmlBodyTest {
    @Test
    fun `plain html with no image is a single html segment`() {
        val segments = splitCommentHtml("Hello <b>world</b>")

        assertThat(segments).containsExactly(CommentBodySegment.Html("Hello <b>world</b>"))
    }

    @Test
    fun `an image at the start yields no leading html segment`() {
        val segments = splitCommentHtml("""<img src="https://example.com/a.png">tail""")

        assertThat(segments).containsExactly(
            CommentBodySegment.Image("https://example.com/a.png"),
            CommentBodySegment.Html("tail")
        )
    }

    @Test
    fun `text around an image splits into html, image, html in order`() {
        val segments = splitCommentHtml("before<img src='https://example.com/b.jpg'>after")

        assertThat(segments).containsExactly(
            CommentBodySegment.Html("before"),
            CommentBodySegment.Image("https://example.com/b.jpg"),
            CommentBodySegment.Html("after")
        )
    }

    @Test
    fun `multiple images are split with the text between them`() {
        val segments = splitCommentHtml("a<img src='1.png'>b<img src='2.png'>c")

        assertThat(segments).containsExactly(
            CommentBodySegment.Html("a"),
            CommentBodySegment.Image("1.png"),
            CommentBodySegment.Html("b"),
            CommentBodySegment.Image("2.png"),
            CommentBodySegment.Html("c")
        )
    }

    @Test
    fun `blank text around an image is dropped`() {
        val segments = splitCommentHtml("  <img src='1.png'>  ")

        assertThat(segments).containsExactly(CommentBodySegment.Image("1.png"))
    }

    @Test
    fun `an img tag without a src is left inline in the html`() {
        val segments = splitCommentHtml("x<img alt='y'>z")

        assertThat(segments).containsExactly(CommentBodySegment.Html("x<img alt='y'>z"))
    }

    @Test
    fun `plain html renders its text content`() {
        val result = commentHtmlToAnnotatedString("Nice <b>post</b>", LINK_COLOR)

        assertThat(result.text).isEqualTo("Nice post")
    }

    @Test
    fun `trailing block whitespace is trimmed`() {
        val result = commentHtmlToAnnotatedString("<p>hello</p>", LINK_COLOR)

        assertThat(result.text).isEqualTo("hello")
    }

    @Test
    fun `a link becomes a tappable url annotation`() {
        val result = commentHtmlToAnnotatedString(
            """see <a href="https://example.com">this</a>""",
            LINK_COLOR
        )

        assertThat(result.text).isEqualTo("see this")
        val links = result.getLinkAnnotations(0, result.length)
        assertThat(links).hasSize(1)
        val link = links.first().item
        assertThat(link).isInstanceOf(LinkAnnotation.Url::class.java)
        assertThat((link as LinkAnnotation.Url).url).isEqualTo("https://example.com")
    }

    @Test
    fun `whitespace-only html yields an empty annotated string`() {
        val result = commentHtmlToAnnotatedString("<p>   </p>", LINK_COLOR)

        assertThat(result.text).isEmpty()
    }

    companion object {
        private val LINK_COLOR = Color.Blue
    }
}
