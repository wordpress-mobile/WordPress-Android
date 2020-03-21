@file:JvmName("ReblogUtils")

package org.wordpress.android.util

/**
 * Returns the string embedded in a quote
 */
val String.embeddedQuote
    get() = "<blockquote>$this</blockquote>"

/**
 * Returns the string embedded in a WordPress quote
 */
val String.embeddedWpQuote
    get() = """<!-- wp:quote --><blockquote class="wp-block-quote">$this</blockquote><!-- /wp:quote -->"""

/**
 * Returns the string embedded in a citation
 */
val String.embeddedCitation
    get() = "<cite>$this</cite>"

/**
 * Returns an html image from an image url string
 */
val String.htmlImage
    get() = """<img src="$this">"""

/**
 * Returns an html WordPress image
 */
val String.htmlWpImage
    get() = "<!-- wp:image --><figure class=\"wp-block-image\">${this.htmlImage}</figure><!-- /wp:image -->"

/**
 * Returns an html paragraph
 */
val String.htmlParagraph
    get() = "<p>$this</p>"

/**
 * Returns an html WordPress paragraph
 */
val String.htmlWpParagraph
    get() = "<!-- wp:paragraph -->${this.htmlParagraph}<!-- /wp:paragraph -->"

/**
 * Creates a hyperlink from a URL
 * @param url the url
 * @param text the text to display. If not provided the [url] will be used
 * @return the html of the hyperlink
 */
fun hyperLink(url: String, text: String? = null) = """<a href="$url">${text ?: url}</a>"""

/**
 * Provides an html containing the post [quote] followed by a link citation
 * @param quote the post quote
 * @param citationTitle the citation text
 * @param citationUrl the citation link
 * @return the html containing the post [quote] followed by a link citation
 */
fun quoteWithCitation(quote: String, citationTitle: String, citationUrl: String): String {
    return quote.htmlParagraph + hyperLink(citationUrl, citationTitle).embeddedCitation
}

/**
 * Provides the reblog post containing a featured image (if exists) followed by the quote and citation
 * @param imageUrl the featured image url (might be null)
 * @param quote the post quote
 * @param citationTitle the citation text
 * @param citationUrl the citation link
 * @param isGutenberg if true
 * @return the html containing the featured image (if exists) followed by the quote and citation
 */
fun reblogContent(
    imageUrl: String?,
    quote: String,
    citationTitle: String,
    citationUrl: String,
    isGutenberg: Boolean = true
): String {
    val quoteWithCitation = quoteWithCitation(quote, citationTitle, citationUrl)
    val html = if (isGutenberg) quoteWithCitation.embeddedWpQuote else quoteWithCitation.embeddedQuote
    if (imageUrl != null && UrlUtils.isImageUrl(imageUrl)) {
        val imageHtml = if (isGutenberg) imageUrl.htmlWpImage else imageUrl.htmlImage.htmlParagraph
        return imageHtml + html
    }
    return html
}
