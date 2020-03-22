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
 * Creates an html image from an image url string or null if the url is not valid
 * @param imageUrlString the image url string
 * @param urlUtils optional UrlUtilsWrapper
 * @return html image or null if the url is not valid
 */
@JvmOverloads
fun htmlImage(imageUrlString: String?, urlUtils: UrlUtilsWrapper = UrlUtilsWrapper()): String? {
    return if (urlUtils.isImageUrl(imageUrlString)) """<img src="$imageUrlString">""" else null
}

/**
 * Returns an html WordPress image from an image url string or null if the url is not valid
 * @param imageUrlString the image url string
 * @param urlUtils optional UrlUtilsWrapper
 * @return html image or null if the url is not valid
 */
@JvmOverloads
fun htmlWpImage(imageUrlString: String?, urlUtils: UrlUtilsWrapper = UrlUtilsWrapper()): String? {
    if (!urlUtils.isImageUrl(imageUrlString)) return null
    return """<!-- wp:image --><figure class="wp-block-image">""" +
            htmlImage(imageUrlString, urlUtils) + "</figure><!-- /wp:image -->"
}

/**
 * Returns an html paragraph
 */
val String.htmlParagraph
    get() = "<p>$this</p>"

/**
 * Creates a hyperlink from a url after validating the link
 * @param url the url
 * @param text the text to display. If not provided the [url] will be used
 * @param urlUtils optional UrlUtilsWrapper
 * @return the html of the hyperlink or null if the url is not valid
 */
@JvmOverloads
fun hyperLink(url: String, text: String = url, urlUtils: UrlUtilsWrapper = UrlUtilsWrapper()): String? {
    if (!urlUtils.isValidUrlAndHostNotNull(url)) return null
    return """<a href="$url">$text</a>"""
}

/**
 * Provides an html containing the post [quote] followed by a link citation if the later is valid
 * @param quote the post quot
 * @param citationUrl the citation link (optional)
 * @param citationTitle the citation text (optional)
 * @param urlUtils optional UrlUtilsWrapper
 * @return the html containing the post [quote] followed by a link citation if the later is valid
 */
fun quoteWithCitation(
    quote: String,
    citationUrl: String? = null,
    citationTitle: String? = null,
    urlUtils: UrlUtilsWrapper = UrlUtilsWrapper()
): String = when {
    citationUrl == null -> quote.htmlParagraph
    citationTitle == null -> quote.htmlParagraph + (hyperLink(citationUrl, urlUtils = urlUtils)?.embeddedCitation ?: "")
    else -> quote.htmlParagraph + (hyperLink(citationUrl, citationTitle, urlUtils)?.embeddedCitation ?: "")
}

/**
 * Provides the reblog post containing a featured image (if exists) followed by the quote and citation
 * @param imageUrl the featured image url (might be null)
 * @param quote the post quote
 * @param citationTitle the citation text
 * @param citationUrl the citation link
 * @param isGutenberg if true
 * @param urlUtils optional UrlUtilsWrapper instance
 * @return the html containing the featured image (if exists) followed by the quote and citation
 */
@JvmOverloads
fun reblogContent(
    imageUrl: String?,
    quote: String,
    citationTitle: String?,
    citationUrl: String?,
    isGutenberg: Boolean = true,
    urlUtils: UrlUtilsWrapper = UrlUtilsWrapper()
): String {
    val quoteWithCitation = quoteWithCitation(quote, citationUrl, citationTitle, urlUtils)
    val html = if (isGutenberg) quoteWithCitation.embeddedWpQuote else quoteWithCitation.embeddedQuote
    val imageHtml = if (isGutenberg) {
        htmlWpImage(imageUrl, urlUtils)
    } else {
        htmlImage(imageUrl, urlUtils)?.htmlParagraph
    }
    return (imageHtml ?: "") + html
}
