package org.wordpress.android.ui.compose.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private const val TRIPLE_DELIMITER_LENGTH = 3
private const val DOUBLE_DELIMITER_LENGTH = 2
private const val SINGLE_DELIMITER_LENGTH = 1
private const val CODE_BACKGROUND_ALPHA = 0.2f

/**
 * Convert markdown text to Compose AnnotatedString with basic formatting support.
 *
 * ## Supported Syntax
 * - **Bold**: `**text**` or `__text__`
 * - *Italic*: `*text*` or `_text_`
 * - ***Bold + Italic***: `***text***` or `___text___`
 * - `Inline Code`: `` `text` ``
 *
 * ## Limitations
 * - Nested formatting is not supported (e.g., `**bold *and italic***` will only apply bold to the outer content)
 * - Mixed delimiters are not supported (e.g., `**bold__` won't work, use matching delimiters)
 * - Multiline formatting is supported but not optimized for very long texts (>10,000 characters)
 * - Links, images, lists, headers, and block quotes are not supported
 *
 * ## Escape Characters
 * Use backslash `\` to escape markdown characters:
 * - `\*not italic\*` → *not italic* (literal asterisks)
 * - `\`not code\`` → `not code` (literal backticks)
 *
 * ## Security
 * This parser only applies text styling and does not interpret URLs, HTML, or scripts.
 * Safe to use with untrusted user input from support conversations.
 *
 * @param markdownText The input text with optional markdown syntax
 * @return AnnotatedString with applied formatting styles
 */
fun markdownToAnnotatedString(markdownText: String): AnnotatedString = buildAnnotatedString {
    var currentIndex = 0
    val text = markdownText

    while (currentIndex < text.length) {
        when {
            // Escape character: \* → *
            text[currentIndex] == '\\' && currentIndex + SINGLE_DELIMITER_LENGTH < text.length -> {
                val nextChar = text[currentIndex + SINGLE_DELIMITER_LENGTH]
                if (nextChar in setOf('*', '_', '`', '\\')) {
                    append(nextChar)
                    currentIndex += DOUBLE_DELIMITER_LENGTH
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Bold + Italic: ***text*** or ___text___
            text.startsWith("***", currentIndex) || text.startsWith("___", currentIndex) -> {
                currentIndex = processBoldItalic(text, currentIndex)
            }
            // Bold: **text** or __text__
            text.startsWith("**", currentIndex) || text.startsWith("__", currentIndex) -> {
                currentIndex = processBold(text, currentIndex)
            }
            // Italic: *text* or _text_
            text[currentIndex] == '*' || text[currentIndex] == '_' -> {
                currentIndex = processItalic(text, currentIndex)
            }
            // Inline code: `text`
            text[currentIndex] == '`' -> {
                currentIndex = processInlineCode(text, currentIndex)
            }
            else -> {
                append(text[currentIndex])
                currentIndex++
            }
        }
    }
}

private fun AnnotatedString.Builder.processBoldItalic(text: String, startIndex: Int): Int {
    val delimiter = text.substring(startIndex, startIndex + TRIPLE_DELIMITER_LENGTH)
    val endIndex = text.indexOf(delimiter, startIndex + TRIPLE_DELIMITER_LENGTH)
    return if (endIndex != -1) {
        val start = length
        append(text.substring(startIndex + TRIPLE_DELIMITER_LENGTH, endIndex))
        addStyle(
            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
            start,
            length
        )
        endIndex + TRIPLE_DELIMITER_LENGTH
    } else {
        append(text[startIndex])
        startIndex + SINGLE_DELIMITER_LENGTH
    }
}

private fun AnnotatedString.Builder.processBold(text: String, startIndex: Int): Int {
    val delimiter = text.substring(startIndex, startIndex + DOUBLE_DELIMITER_LENGTH)
    val endIndex = text.indexOf(delimiter, startIndex + DOUBLE_DELIMITER_LENGTH)
    return if (endIndex != -1 && endIndex > startIndex + DOUBLE_DELIMITER_LENGTH) {
        val start = length
        append(text.substring(startIndex + DOUBLE_DELIMITER_LENGTH, endIndex))
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
        endIndex + DOUBLE_DELIMITER_LENGTH
    } else {
        append(text[startIndex])
        startIndex + SINGLE_DELIMITER_LENGTH
    }
}

private fun AnnotatedString.Builder.processItalic(text: String, startIndex: Int): Int {
    val delimiter = text[startIndex]
    val endIndex = text.indexOf(delimiter, startIndex + SINGLE_DELIMITER_LENGTH)
    return if (endIndex != -1 && endIndex != startIndex + SINGLE_DELIMITER_LENGTH) {
        val start = length
        append(text.substring(startIndex + SINGLE_DELIMITER_LENGTH, endIndex))
        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
        endIndex + SINGLE_DELIMITER_LENGTH
    } else {
        append(text[startIndex])
        startIndex + SINGLE_DELIMITER_LENGTH
    }
}

private fun AnnotatedString.Builder.processInlineCode(text: String, startIndex: Int): Int {
    val endIndex = text.indexOf('`', startIndex + SINGLE_DELIMITER_LENGTH)
    return if (endIndex != -1) {
        val start = length
        append(text.substring(startIndex + SINGLE_DELIMITER_LENGTH, endIndex))
        addStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color.Gray.copy(alpha = CODE_BACKGROUND_ALPHA)
            ),
            start,
            length
        )
        endIndex + SINGLE_DELIMITER_LENGTH
    } else {
        append(text[startIndex])
        startIndex + SINGLE_DELIMITER_LENGTH
    }
}
