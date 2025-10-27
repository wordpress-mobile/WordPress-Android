package org.wordpress.android.ui.compose.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Convert markdown text to Compose AnnotatedString.
 * Supports basic markdown formatting: bold, italic, bold+italic, and inline code.
 */
fun markdownToAnnotatedString(markdownText: String): AnnotatedString = buildAnnotatedString {
    var currentIndex = 0
    val text = markdownText

    while (currentIndex < text.length) {
        when {
            // Bold + Italic: ***text*** or ___text___
            text.startsWith("***", currentIndex) || text.startsWith("___", currentIndex) -> {
                val delimiter = text.substring(currentIndex, currentIndex + 3)
                val endIndex = text.indexOf(delimiter, currentIndex + 3)
                if (endIndex != -1) {
                    val start = length
                    append(text.substring(currentIndex + 3, endIndex))
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        start,
                        length
                    )
                    currentIndex = endIndex + 3
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Bold: **text** or __text__
            text.startsWith("**", currentIndex) || text.startsWith("__", currentIndex) -> {
                val delimiter = text.substring(currentIndex, currentIndex + 2)
                val endIndex = text.indexOf(delimiter, currentIndex + 2)
                if (endIndex != -1 && endIndex > currentIndex + 2) {
                    val start = length
                    append(text.substring(currentIndex + 2, endIndex))
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                    currentIndex = endIndex + 2
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Italic: *text* or _text_
            text[currentIndex] == '*' || text[currentIndex] == '_' -> {
                val delimiter = text[currentIndex]
                val endIndex = text.indexOf(delimiter, currentIndex + 1)
                if (endIndex != -1 && endIndex != currentIndex + 1) {
                    val start = length
                    append(text.substring(currentIndex + 1, endIndex))
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                    currentIndex = endIndex + 1
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            // Inline code: `text`
            text[currentIndex] == '`' -> {
                val endIndex = text.indexOf('`', currentIndex + 1)
                if (endIndex != -1) {
                    val start = length
                    append(text.substring(currentIndex + 1, endIndex))
                    addStyle(
                        SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f)
                        ),
                        start,
                        length
                    )
                    currentIndex = endIndex + 1
                } else {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
            else -> {
                append(text[currentIndex])
                currentIndex++
            }
        }
    }
}
