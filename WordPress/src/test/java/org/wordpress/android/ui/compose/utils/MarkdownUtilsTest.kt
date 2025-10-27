package org.wordpress.android.ui.compose.utils

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MarkdownUtilsTest {
    @Test
    fun `plain text without markdown is unchanged`() {
        val input = "This is plain text without any formatting"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo(input)
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `bold text with double asterisks is formatted`() {
        val input = "This is **bold** text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is bold text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(12)
    }

    @Test
    fun `bold text with double underscores is formatted`() {
        val input = "This is __bold__ text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is bold text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(12)
    }

    @Test
    fun `italic text with single asterisk is formatted`() {
        val input = "This is *italic* text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is italic text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(14)
    }

    @Test
    fun `italic text with single underscore is formatted`() {
        val input = "This is _italic_ text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is italic text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(14)
    }

    @Test
    fun `bold and italic text with triple asterisks is formatted`() {
        val input = "This is ***bold and italic*** text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is bold and italic text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].item.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(23)
    }

    @Test
    fun `bold and italic text with triple underscores is formatted`() {
        val input = "This is ___bold and italic___ text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is bold and italic text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].item.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(23)
    }

    @Test
    fun `inline code with backticks is formatted`() {
        val input = "Use the `code` function"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Use the code function")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontFamily).isEqualTo(FontFamily.Monospace)
        assertThat(result.spanStyles[0].item.background).isNotNull()
        assertThat(result.spanStyles[0].start).isEqualTo(8)
        assertThat(result.spanStyles[0].end).isEqualTo(12)
    }

    @Test
    fun `multiple markdown formats in same text are all formatted`() {
        val input = "This has **bold**, *italic*, and `code` formatting"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This has bold, italic, and code formatting")
        assertThat(result.spanStyles).hasSize(3)

        // Bold
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].start).isEqualTo(9)
        assertThat(result.spanStyles[0].end).isEqualTo(13)

        // Italic
        assertThat(result.spanStyles[1].item.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(result.spanStyles[1].start).isEqualTo(15)
        assertThat(result.spanStyles[1].end).isEqualTo(21)

        // Code
        assertThat(result.spanStyles[2].item.fontFamily).isEqualTo(FontFamily.Monospace)
        assertThat(result.spanStyles[2].start).isEqualTo(27)
        assertThat(result.spanStyles[2].end).isEqualTo(31)
    }

    @Test
    fun `unclosed markdown delimiters are treated as plain text`() {
        val input = "This has **unclosed bold text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This has **unclosed bold text")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `empty markdown delimiters are treated as plain text`() {
        val input = "This has **** and ____ empty bold"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This has **** and ____ empty bold")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `nested markdown formats are not supported and treated literally`() {
        val input = "**bold *and italic* combined**"
        val result = markdownToAnnotatedString(input)

        // The outer bold will be applied to "bold *and italic* combined"
        assertThat(result.text).isEqualTo("bold *and italic* combined")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `multiple bold sections in text are all formatted`() {
        val input = "**First** word and **second** word"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("First word and second word")
        assertThat(result.spanStyles).hasSize(2)

        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(5)

        assertThat(result.spanStyles[1].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[1].start).isEqualTo(15)
        assertThat(result.spanStyles[1].end).isEqualTo(21)
    }

    @Test
    fun `empty string returns empty annotated string`() {
        val input = ""
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEmpty()
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `markdown at start of string is formatted`() {
        val input = "**Bold** at start"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Bold at start")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(4)
    }

    @Test
    fun `markdown at end of string is formatted`() {
        val input = "At end **bold**"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("At end bold")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(7)
        assertThat(result.spanStyles[0].end).isEqualTo(11)
    }

    @Test
    fun `entire string is markdown formatted`() {
        val input = "**Everything is bold**"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Everything is bold")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].start).isEqualTo(0)
        assertThat(result.spanStyles[0].end).isEqualTo(18)
    }

    @Test
    fun `single character markdown formatting works`() {
        val input = "Single **a** character"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Single a character")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].start).isEqualTo(7)
        assertThat(result.spanStyles[0].end).isEqualTo(8)
    }

    // Edge Cases and Escape Characters

    @Test
    fun `escaped asterisk is treated as literal`() {
        val input = "This is \\*not italic\\* text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is *not italic* text")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `escaped underscore is treated as literal`() {
        val input = "This is \\_not italic\\_ text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is _not italic_ text")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `escaped backtick is treated as literal`() {
        val input = "This is \\`not code\\` text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is `not code` text")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `escaped backslash is treated as literal`() {
        val input = "This is \\\\ a backslash"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is \\ a backslash")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `backslash before non-special character is kept`() {
        val input = "This is \\a normal text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is \\a normal text")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `mixed escaped and formatted characters work together`() {
        val input = "\\*literal\\* and **bold** text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("*literal* and bold text")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[0].start).isEqualTo(14)
        assertThat(result.spanStyles[0].end).isEqualTo(18)
    }

    @Test
    fun `unicode characters are preserved correctly`() {
        val input = "**Hello 世界** and *emoji 😀*"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Hello 世界 and emoji 😀")
        assertThat(result.spanStyles).hasSize(2)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[1].item.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `mixed delimiters are not formatted`() {
        val input = "This is **not bold__"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is **not bold__")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `multiline text with formatting works`() {
        val input = "Line 1 **bold**\nLine 2 *italic*\nLine 3 normal"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Line 1 bold\nLine 2 italic\nLine 3 normal")
        assertThat(result.spanStyles).hasSize(2)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(result.spanStyles[1].item.fontStyle).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `long text with multiple formats performs correctly`() {
        val input = buildString {
            repeat(100) {
                append("**bold** *italic* `code` ")
            }
        }
        val result = markdownToAnnotatedString(input)

        // Should have 300 spans (100 bold + 100 italic + 100 code)
        assertThat(result.spanStyles).hasSize(300)
    }

    @Test
    fun `escaped characters at end of string are handled`() {
        val input = "Text ending with \\*"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Text ending with *")
        assertThat(result.spanStyles).isEmpty()
    }

    @Test
    fun `backslash at end of string is preserved`() {
        val input = "Text ending with \\"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Text ending with \\")
        assertThat(result.spanStyles).isEmpty()
    }
}
