package org.wordpress.android.ui.compose.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

private const val CODE_BACKGROUND_ALPHA = 0.2f

/**
 * Convert markdown text to Compose AnnotatedString using the CommonMark library.
 *
 * This provides robust, standards-compliant markdown parsing with support for:
 * - **Bold**: `**text**` or `__text__`
 * - *Italic*: `*text*` or `_text_`
 * - ***Bold + Italic***: `***text***` or `___text___`
 * - `Inline Code`: `` `text` ``
 * - Links: `[text](url)`
 * - Headings: `# Heading` (rendered as bold text)
 * - Unordered Lists: `- item` or `* item`
 * - Horizontal Rules: `---` or `***`
 * - Nested formatting (e.g., `**bold *and italic***`)
 * - Proper escape handling
 *
 * ## Heading Handling
 * Headings (# through ######) are rendered as bold text without size differentiation.
 * This provides visual emphasis while maintaining a consistent text flow for chat-like UIs.
 *
 * ## List Handling
 * Unordered list items are prefixed with "- " (dash). List formatting is preserved
 * with proper indentation and spacing.
 *
 * ## Link Handling
 * Links are styled with underline and color, and include LinkAnnotation.Url annotations
 * that automatically handle clicks. When used with Compose Text, links will open in
 * the default browser automatically.
 *
 * ## Security
 * This parser applies text styling and link annotations. Links use LinkAnnotation.Url
 * which will automatically open URLs in the system browser.
 * Safe to use with untrusted user input from support conversations.
 *
 * @param markdownText The input text with optional markdown syntax
 * @return AnnotatedString with applied formatting styles and link annotations
 */
fun markdownToAnnotatedString(markdownText: String): AnnotatedString {
    val parser = Parser.builder().build()
    val document = parser.parse(markdownText)

    return buildAnnotatedString {
        processNode(document)
    }
}

private const val SECTION_DIVIDER_SIZE = 10

@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun AnnotatedString.Builder.processNode(node: Node) {
    var child = node.firstChild
    while (child != null) {
        when (child) {
            is Text -> append(child.literal)
            is Code -> {
                val start = length
                append(child.literal)
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Gray.copy(alpha = CODE_BACKGROUND_ALPHA)
                    ),
                    start,
                    length
                )
            }
            is Link -> {
                withLink(LinkAnnotation.Url(child.destination)) {
                    val start = length
                    processNode(child)
                    addStyle(
                        SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline
                        ),
                        start,
                        length
                    )
                }
            }
            is Emphasis -> {
                val start = length
                processNode(child)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
            }
            is StrongEmphasis -> {
                val start = length
                processNode(child)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
            }
            is Heading -> {
                val start = length
                processNode(child)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                // Add newline after heading if it's not the last one
                if (child.next != null) {
                    append("\n\n")
                }
            }
            is BulletList -> {
                processNode(child)
                // Add newline after list if it's not the last one
                if (child.next != null) {
                    append("\n")
                }
            }
            is ListItem -> {
                append("- ")
                processNode(child)
                // Add newline after list item if it's not the last one
                if (child.next != null) {
                    append("\n")
                }
            }
            is ThematicBreak -> {
                append("─".repeat(SECTION_DIVIDER_SIZE))
                // Add newline after horizontal rule if it's not the last one
                if (child.next != null) {
                    append("\n\n")
                }
            }
            is Paragraph -> {
                processNode(child)
                // Add newline after paragraph if it's not the last one
                if (child.next != null) {
                    append("\n\n")
                }
            }
            is SoftLineBreak -> append("\n")
            else -> processNode(child)
        }
        child = child.next
    }
}
