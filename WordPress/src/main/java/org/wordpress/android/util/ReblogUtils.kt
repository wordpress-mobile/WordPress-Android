package org.wordpress.android.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReblogUtils @Inject constructor(private val urlUtils: UrlUtilsWrapper) {
    /**
     * Returns the [string] embedded in a quote
     */
    fun embeddedQuote(string: String) = "<blockquote>$string</blockquote>"

    /**
     * Returns the [string] embedded in a WordPress quote
     */
    fun embeddedWpQuote(string: String): String {
        return """<!-- wp:quote --><blockquote class="wp-block-quote">$string</blockquote><!-- /wp:quote -->"""
    }

    /**
     * Returns the [string] embedded in a citation
     */
    fun embeddedCitation(string: String) = "<cite>$string</cite>"

    /**
     * Creates an html image from an image url string or null if the url is not valid
     * @param imageUrlString the image url string
     * @return html image or null if the url is not valid
     */
    fun htmlImage(imageUrlString: String?): String? {
        return if (urlUtils.isImageUrl(imageUrlString)) """<img src="$imageUrlString">""" else null
    }

    /**
     * Returns an html WordPress image from an image url string or null if the url is not valid
     * @param imageUrlString the image url string
     * @return html image or null if the url is not valid
     */
    fun htmlWpImage(imageUrlString: String?): String? {
        if (!urlUtils.isImageUrl(imageUrlString)) return null
        return """<!-- wp:image --><figure class="wp-block-image">""" +
                htmlImage(imageUrlString) + "</figure><!-- /wp:image -->"
    }

    /**
     * Returns the [string] in an html paragraph
     */
    fun htmlParagraph(string: String) = "<p>$string</p>"

    /**
     * Creates a hyperlink from a url after validating the link
     * @param url the url
     * @param text the text to display. If not provided the [url] will be used
     * @return the html of the hyperlink or null if the url is not valid
     */
    @JvmOverloads
    fun hyperLink(url: String, text: String = url): String? {
        if (!urlUtils.isValidUrlAndHostNotNull(url)) return null
        return """<a href="$url">$text</a>"""
    }

    /**
     * Provides an html containing the post [quote] followed by a link citation if the later is valid
     * @param quote the post quot
     * @param citationUrl the citation link (optional)
     * @param citationTitle the citation text (optional)
     * @return the html containing the post [quote] followed by a link citation if the later is valid
     */
    fun quoteWithCitation(
        quote: String,
        citationUrl: String? = null,
        citationTitle: String? = null
    ): String = when {
        citationUrl == null -> htmlParagraph(quote)
        citationTitle == null -> htmlParagraph(quote) + embeddedCitation(
                hyperLink(citationUrl)
                        ?: ""
        )
        else -> htmlParagraph(quote) + (hyperLink(citationUrl, citationTitle)?.let { embeddedCitation(it) } ?: "")
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
    @JvmOverloads
    fun reblogContent(
        imageUrl: String?,
        quote: String,
        citationTitle: String?,
        citationUrl: String?,
        isGutenberg: Boolean = true
    ): String {
        val quoteWithCitation = quoteWithCitation(quote, citationUrl, citationTitle)
        val html = if (isGutenberg) embeddedWpQuote(quoteWithCitation) else embeddedQuote(quoteWithCitation)
        val imageHtml = if (isGutenberg) {
            htmlWpImage(imageUrl)
        } else {
            htmlImage(imageUrl)?.let { htmlParagraph(it) }
        }
        return (imageHtml ?: "") + html
    }
}
