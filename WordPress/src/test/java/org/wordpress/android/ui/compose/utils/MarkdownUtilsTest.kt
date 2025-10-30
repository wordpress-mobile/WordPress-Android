package org.wordpress.android.ui.compose.utils

import androidx.compose.ui.text.LinkAnnotation
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
        // CommonMark applies bold and italic as separate, nested spans
        assertThat(result.spanStyles).hasSize(2)
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
    }

    @Test
    fun `bold and italic text with triple underscores is formatted`() {
        val input = "This is ___bold and italic___ text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("This is bold and italic text")
        // CommonMark applies bold and italic as separate, nested spans
        assertThat(result.spanStyles).hasSize(2)
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
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
    fun `nested markdown formats are properly supported`() {
        val input = "**bold *and italic* combined**"
        val result = markdownToAnnotatedString(input)

        // CommonMark properly handles nested formatting
        assertThat(result.text).isEqualTo("bold and italic combined")
        assertThat(result.spanStyles.size).isGreaterThan(1)
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
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

        // CommonMark adds paragraph separators, so we just verify formatting is applied
        assertThat(result.spanStyles.size).isGreaterThanOrEqualTo(2)
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
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

    // Link Tests

    @Test
    fun `simple link is formatted with underline and color`() {
        val input = "Check out [this link](https://example.com)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Check out this link")
        assertThat(result.spanStyles).hasSize(1)

        // Should have color and underline styles combined
        val linkStyle = result.spanStyles[0]
        assertThat(linkStyle.item.textDecoration).isNotNull()
        assertThat(linkStyle.item.color).isNotNull()
    }

    @Test
    fun `link URL is stored as link annotation`() {
        val input = "Visit [example](https://example.com) for more"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Visit example for more")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        val linkAnnotation = annotations[0].item as LinkAnnotation.Url
        assertThat(linkAnnotation.url).isEqualTo("https://example.com")
        assertThat(annotations[0].start).isEqualTo(6)
        assertThat(annotations[0].end).isEqualTo(13)
    }

    @Test
    fun `multiple links are all formatted`() {
        val input = "See [link1](http://one.com) and [link2](http://two.com)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("See link1 and link2")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(2)
        val link1 = annotations[0].item as LinkAnnotation.Url
        val link2 = annotations[1].item as LinkAnnotation.Url
        assertThat(link1.url).isEqualTo("http://one.com")
        assertThat(link2.url).isEqualTo("http://two.com")
    }

    @Test
    fun `link with formatted text inside works`() {
        val input = "Click [**bold link**](https://example.com)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Click bold link")

        // Should have bold, color, and underline
        assertThat(result.spanStyles.size).isGreaterThanOrEqualTo(2)
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        assertThat(hasBold).isTrue()

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        val linkAnnotation = annotations[0].item as LinkAnnotation.Url
        assertThat(linkAnnotation.url).isEqualTo("https://example.com")
    }

    @Test
    fun `link at start of string is formatted`() {
        val input = "[Start link](https://example.com) here"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Start link here")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].start).isEqualTo(0)
    }

    @Test
    fun `link at end of string is formatted`() {
        val input = "End with [this link](https://example.com)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("End with this link")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].end).isEqualTo(result.text.length)
    }

    @Test
    fun `entire string as a link is formatted`() {
        val input = "[Everything is a link](https://example.com)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Everything is a link")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].start).isEqualTo(0)
        assertThat(annotations[0].end).isEqualTo(result.text.length)
    }

    @Test
    fun `link with special characters in URL is preserved`() {
        val input = "Go to [search](https://example.com/search?q=test&lang=en)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Go to search")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        val linkAnnotation = annotations[0].item as LinkAnnotation.Url
        assertThat(linkAnnotation.url).isEqualTo("https://example.com/search?q=test&lang=en")
    }

    // Heading Tests

    @Test
    fun `heading level 1 is formatted as bold`() {
        val input = "# Heading 1"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Heading 1")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `heading level 2 is formatted as bold`() {
        val input = "## Heading 2"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Heading 2")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `heading level 6 is formatted as bold`() {
        val input = "###### Heading 6"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Heading 6")
        assertThat(result.spanStyles).hasSize(1)
        assertThat(result.spanStyles[0].item.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun `heading with inline formatting preserves both styles`() {
        val input = "# Heading with *italic* text"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("Heading with italic text")
        // Should have bold for heading and italic for the word
        assertThat(result.spanStyles.size).isGreaterThanOrEqualTo(2)
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
    }

    @Test
    fun `multiple headings are all formatted`() {
        val input = "# First\n## Second"
        val result = markdownToAnnotatedString(input)

        // Both headings should be bold
        val boldStyles = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertThat(boldStyles.size).isGreaterThanOrEqualTo(2)
    }

    @Test
    fun `heading followed by paragraph maintains separation`() {
        val input = "# Heading\nRegular paragraph"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("Heading")
        assertThat(result.text).contains("Regular paragraph")
        // Should have bold for heading
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        assertThat(hasBold).isTrue()
    }

    @Test
    fun `heading with link inside works`() {
        val input = "# Heading with [link](https://example.com)"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("Heading with link")
        // Should have bold for heading
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        assertThat(hasBold).isTrue()
        // Should have link annotation
        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
    }

    @Test
    fun `heading with code inside works`() {
        val input = "# Heading with `code`"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("Heading with code")
        // Should have bold for heading
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        assertThat(hasBold).isTrue()
        // Should have monospace for code
        val hasCode = result.spanStyles.any { it.item.fontFamily == FontFamily.Monospace }
        assertThat(hasCode).isTrue()
    }

    // List Tests

    @Test
    fun `simple unordered list is formatted with bullets`() {
        val input = "- First item\n- Second item\n- Third item"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("- First item")
        assertThat(result.text).contains("- Second item")
        assertThat(result.text).contains("- Third item")
    }

    @Test
    fun `list with asterisk delimiter is formatted`() {
        val input = "* Item one\n* Item two"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("- Item one")
        assertThat(result.text).contains("- Item two")
    }

    @Test
    fun `list items with inline formatting preserve styles`() {
        val input = "- **Bold item**\n- *Italic item*\n- Item with `code`"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("- Bold item")
        assertThat(result.text).contains("- Italic item")
        assertThat(result.text).contains("- Item with code")

        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        val hasCode = result.spanStyles.any { it.item.fontFamily == FontFamily.Monospace }

        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
        assertThat(hasCode).isTrue()
    }

    @Test
    fun `list item with link works`() {
        val input = "- Check [this link](https://example.com)\n- Another item"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("- Check this link")
        assertThat(result.text).contains("- Another item")

        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
        val linkAnnotation = annotations[0].item as LinkAnnotation.Url
        assertThat(linkAnnotation.url).isEqualTo("https://example.com")
    }

    @Test
    fun `list followed by paragraph maintains separation`() {
        val input = "- List item\n\nRegular paragraph"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("- List item")
        assertThat(result.text).contains("Regular paragraph")
    }

    @Test
    fun `single list item is formatted`() {
        val input = "- Only one item"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).isEqualTo("- Only one item")
    }

    // Horizontal Rule Tests

    @Test
    fun `horizontal rule with dashes is rendered`() {
        val input = "Before\n\n---\n\nAfter"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("Before")
        assertThat(result.text).contains("──────────")
        assertThat(result.text).contains("After")
    }

    @Test
    fun `horizontal rule with asterisks is rendered`() {
        val input = "Text above\n\n***\n\nText below"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("Text above")
        assertThat(result.text).contains("──────────")
        assertThat(result.text).contains("Text below")
    }

    @Test
    fun `horizontal rule with underscores is rendered`() {
        val input = "Start\n\n___\n\nEnd"
        val result = markdownToAnnotatedString(input)

        assertThat(result.text).contains("Start")
        assertThat(result.text).contains("──────────")
        assertThat(result.text).contains("End")
    }

    @Test
    fun `multiple horizontal rules are all rendered`() {
        val input = "Section 1\n\n---\n\nSection 2\n\n---\n\nSection 3"
        val result = markdownToAnnotatedString(input)

        val hrCount = result.text.count { it == '─' } / 10
        assertThat(hrCount).isEqualTo(2)
    }

    // Complex Integration Test

    @Test
    fun `complex message with all features renders correctly`() {
        val input = """
            # Welcome

            Here's a **bold** statement and *italic* text.

            ## Features

            - First **feature**
            - Second with [link](https://example.com)
            - Third with `code`

            ---

            Visit our site!
        """.trimIndent()

        val result = markdownToAnnotatedString(input)

        // Check all elements are present
        assertThat(result.text).contains("Welcome")
        assertThat(result.text).contains("bold")
        assertThat(result.text).contains("italic")
        assertThat(result.text).contains("Features")
        assertThat(result.text).contains("- First feature")
        assertThat(result.text).contains("- Second with link")
        assertThat(result.text).contains("- Third with code")
        assertThat(result.text).contains("──────────")
        assertThat(result.text).contains("Visit our site!")

        // Check styles are applied
        val hasBold = result.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        val hasItalic = result.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        val hasCode = result.spanStyles.any { it.item.fontFamily == FontFamily.Monospace }

        assertThat(hasBold).isTrue()
        assertThat(hasItalic).isTrue()
        assertThat(hasCode).isTrue()

        // Check link annotation
        val annotations = result.getLinkAnnotations(0, result.text.length)
        assertThat(annotations).hasSize(1)
    }
}
