package org.wordpress.android.ui.comments.unified.compose

import android.text.style.URLSpan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.toAnnotatedString
import org.wordpress.android.ui.dataview.compose.RemoteImage
import org.wordpress.android.util.EmoticonsUtils

/**
 * Renders a comment's HTML body, following the legacy CommentUtils.displayHtmlComment pipeline:
 * emoticon smilies are first replaced with unicode emoji so they stay inline as text, then any
 * remaining `<img>` tags render as width-capped block images (the legacy renderer sized inline
 * images to the view width, so real images effectively rendered as blocks there too), with the
 * HTML between them rendered as selectable text with tappable links.
 */
@Composable
fun CommentHtmlBody(html: String, modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val segments = remember(html) { splitCommentHtml(html) }
    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            segments.forEach { segment ->
                when (segment) {
                    is CommentBodySegment.Html -> {
                        val annotated = remember(segment.html, linkColor) {
                            commentHtmlToAnnotatedString(segment.html, linkColor)
                        }
                        if (annotated.isNotEmpty()) {
                            Text(
                                text = annotated,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }
                    }
                    // Route through the shared RemoteImage wrapper for consistency with the
                    // avatars in this screen. No fallback: a failed inline image renders nothing
                    // (a person/broken-image placeholder would be wrong for body content).
                    is CommentBodySegment.Image -> RemoteImage(
                        imageUrl = segment.url,
                        contentScale = ContentScale.Inside,
                        alignment = Alignment.TopStart,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

internal sealed class CommentBodySegment {
    data class Html(val html: String) : CommentBodySegment()
    data class Image(val url: String) : CommentBodySegment()
}

private val IMG_TAG_PATTERN = Regex(
    """<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>""",
    RegexOption.IGNORE_CASE
)

/**
 * Splits comment HTML into text and image segments. Emoticon smilies are converted to unicode
 * emoji first — same order as the legacy displayHtmlComment, which prevented smilies from being
 * downloaded as images — so only real images remain as `<img>` tags.
 */
internal fun splitCommentHtml(html: String): List<CommentBodySegment> {
    val withEmoji = EmoticonsUtils.replaceEmoticonsWithEmoji(html)
    val segments = mutableListOf<CommentBodySegment>()
    var consumedUpTo = 0
    IMG_TAG_PATTERN.findAll(withEmoji).forEach { match ->
        val precedingHtml = withEmoji.substring(consumedUpTo, match.range.first)
        if (precedingHtml.isNotBlank()) {
            segments.add(CommentBodySegment.Html(precedingHtml))
        }
        segments.add(CommentBodySegment.Image(match.groupValues[1]))
        consumedUpTo = match.range.last + 1
    }
    val remainingHtml = withEmoji.substring(consumedUpTo)
    if (remainingHtml.isNotBlank()) {
        segments.add(CommentBodySegment.Html(remainingHtml))
    }
    return segments
}

/**
 * Renders an HTML fragment as an [AnnotatedString] with tappable, link-styled URLs and trimmed
 * surrounding whitespace.
 */
internal fun commentHtmlToAnnotatedString(html: String, linkColor: Color): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val withLinks = buildAnnotatedString {
        append(spanned.toAnnotatedString())
        spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, end)
            addLink(LinkAnnotation.Url(span.url), start, end)
        }
    }
    // HtmlCompat pads block elements with trailing newlines; trim without breaking span offsets
    val text = withLinks.text
    val start = text.indexOfFirst { !it.isWhitespace() }
    if (start == -1) return AnnotatedString("")
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return if (start == 0 && end == text.length) withLinks else withLinks.subSequence(start, end)
}

@Preview(showBackground = true)
@Composable
private fun CommentHtmlBodyPreview() {
    AppThemeM3 {
        CommentHtmlBody(
            html = "Nice <b>post</b>, see <a href='https://example.com'>this</a>!" +
                "<img src=\"https://example.com/photo.jpg\" />And a closing thought."
        )
    }
}
