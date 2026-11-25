package org.wordpress.android.ui.reader.views.compose.article

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A piece of a post's body that the native post detail lays out on its own. Ported from WPFolio's ArticleBlocks.
 */
sealed interface ReaderArticleBlock {
    data class Paragraph(val html: String) : ReaderArticleBlock
    data class Heading(val text: String, val level: Int) : ReaderArticleBlock

    /** [width] and [height] come from the HTML, so space can be reserved before the image loads */
    data class Image(
        val url: String,
        val caption: String?,
        val width: Int? = null,
        val height: Int? = null,
    ) : ReaderArticleBlock {
        val aspectRatio: Float? get() = if (width != null && height != null) width.toFloat() / height else null
    }

    /** A WordPress gallery, shown as a grid of [columns]. [caption] belongs to the whole gallery */
    data class Gallery(val images: List<Image>, val columns: Int, val caption: String?) : ReaderArticleBlock

    /** [citation] is the quote's attribution; [pull] marks a pullquote, which stands apart from the text */
    data class Quote(val html: String, val citation: String? = null, val pull: Boolean = false) : ReaderArticleBlock

    /** Rows of cells as inline HTML; the first [headerRows] rows are headings */
    data class Table(val rows: List<List<String>>, val headerRows: Int, val caption: String?) : ReaderArticleBlock
    data class ListItem(val html: String, val marker: String, val depth: Int) : ReaderArticleBlock
    data class Code(val text: String) : ReaderArticleBlock
    data object Rule : ReaderArticleBlock

    /** A WordPress button block: a call to action that opens [url] */
    data class Button(val label: String, val url: String) : ReaderArticleBlock

    /** A `<details>` section, collapsed to its [summary] until tapped */
    data class Details(val summary: String, val blocks: List<ReaderArticleBlock>) : ReaderArticleBlock

    /**
     * A video played in place on tap. [id] is the host's video ID, or the file URL for [VideoHost.FILE].
     */
    data class Video(
        val host: VideoHost,
        val id: String,
        val poster: String?,
        val width: Int? = null,
        val height: Int? = null,
    ) : ReaderArticleBlock {
        val aspectRatio: Float
            get() = if (width != null && height != null) width.toFloat() / height else DEFAULT_VIDEO_RATIO
    }

    /** Embedded content that can't be shown natively (maps, audio, social posts), as a link */
    data class Embed(val url: String, val title: String?) : ReaderArticleBlock
}

private const val DEFAULT_VIDEO_RATIO = 16f / 9f

enum class VideoHost(val label: String) {
    YOUTUBE("YouTube"),
    VIMEO("Vimeo"),
    VIDEOPRESS("VideoPress"),
    FILE("Video"),
}

/** Recognises videos from embed, watch and file URLs */
object ReaderVideoLinks {
    private val YOUTUBE = Regex(
        """(?:youtube(?:-nocookie)?\.com/(?:embed/|shorts/|v/|watch\?(?:[^#]*&)?v=)|youtu\.be/)([A-Za-z0-9_-]+)"""
    )
    private val VIMEO = Regex("""(?:player\.vimeo\.com/video/|//(?:www\.)?vimeo\.com/)(\d+)""")
    private val VIDEOPRESS = Regex("""(?:video\.wordpress\.com|videopress\.com)/(?:embed|v)/([A-Za-z0-9]+)""")
    private val FILE = Regex("""\.(?:mp4|m4v|webm|mov)(?:[?#]|$)""", RegexOption.IGNORE_CASE)

    @Suppress("ReturnCount")
    fun parse(
        url: String,
        poster: String? = null,
        width: Int? = null,
        height: Int? = null,
    ): ReaderArticleBlock.Video? {
        fun video(host: VideoHost, id: String, poster: String?) =
            ReaderArticleBlock.Video(host, id, poster, width, height)
        YOUTUBE.find(url)?.groupValues?.get(1)?.let { id ->
            val thumbnail = poster ?: "https://img.youtube.com/vi/$id/hqdefault.jpg"
            return video(VideoHost.YOUTUBE, id, thumbnail)
        }
        VIMEO.find(url)?.groupValues?.get(1)?.let { id ->
            return video(VideoHost.VIMEO, id, poster)
        }
        VIDEOPRESS.find(url)?.groupValues?.get(1)?.let { id ->
            return video(VideoHost.VIDEOPRESS, id, poster)
        }
        // only https files: the player can't load cleartext
        if (url.startsWith("https://") && FILE.containsMatchIn(url)) {
            return video(VideoHost.FILE, url, poster)
        }
        return null
    }
}

/**
 * Splits post HTML into blocks that Compose can lay out natively. Inline markup inside a block stays as HTML for
 * AnnotatedString.fromHtml. Videos become play-in-place blocks, audio and other iframes become links, and scripts
 * and forms are dropped. Ported from WPFolio.
 */
@Suppress("TooManyFunctions")
object ReaderArticleBlocks {
    private val SKIP = setOf("script", "style", "noscript", "object", "embed", "form", "button", "svg", "input")
    private const val MEDIA = "iframe, video, audio"
    private val CELLS = setOf("td", "th")

    /** Images that are content, not emoji drawn inline in text */
    private const val CONTENT_IMAGE = "img:not(.wp-smiley):not(.emoji)"

    /** Block-editor, classic shortcode and Jetpack tiled galleries */
    private val GALLERIES = setOf("wp-block-gallery", "gallery", "tiled-gallery", "wp-block-jetpack-tiled-gallery")
    private const val GALLERY_ITEM = "figure, .gallery-item, .tiled-gallery-item"
    private val GALLERY_COLUMNS = Regex("""^(?:gallery-)?columns-(\d+)$""")
    private const val MAX_GALLERY_COLUMNS = 3

    /** Widgets WordPress sometimes leaves in content that aren't part of the post */
    private val WIDGET_HOSTS = listOf("widgets.wp.com", "jetpack.wordpress.com")

    /** Images that count views rather than show anything */
    private val TRACKERS = listOf("feeds.feedburner.com", "pixel.wp.com", "stats.wordpress.com", "gravatar.com/avatar")
    private val INLINE = setOf(
        "a", "abbr", "b", "br", "cite", "code", "del", "em", "i", "ins", "kbd", "mark", "q", "s", "small", "span",
        "strong", "sub", "sup", "time", "u",
    )
    private const val MAX_ALT_CAPTION_LENGTH = 200

    /** Footnote markers link here, followed by the note's HTML, URL-encoded */
    private const val FOOTNOTE_SCHEME = "footnote:"


    /** The note's HTML if [url] is a footnote marker's link, else null */
    fun footnote(url: String): String? =
        if (url.startsWith(FOOTNOTE_SCHEME)) URLDecoder.decode(url.removePrefix(FOOTNOTE_SCHEME), "UTF-8") else null

    fun parse(html: String, baseUrl: String): List<ReaderArticleBlock> {
        if (html.isBlank()) return emptyList()
        val body = Jsoup.parseBodyFragment(html, baseUrl).body()
        linkFootnotes(body)
        return mutableListOf<ReaderArticleBlock>().also { walk(body, it, depth = 0) }
    }


    /*
     * In-page links have nowhere to go in a native view. Footnote markers (links to a list item elsewhere in the
     * post) instead carry their note, so tapping one can show it in place; the notes' links back to their markers
     * are dropped, and any other in-page link becomes text.
     */
    private fun linkFootnotes(body: Element) {
        val items = body.select("li[id]").associateBy { it.id() }
        val links = body.select("a[href^=#]")
        val notes = links.mapNotNull { items[it.attr("href").drop(1)] }.toSet()
        notes.forEach { it.select("a[href^=#]").remove() }
        for (link in links) {
            if (link.parent() == null) continue // a back-link, removed above
            val note = items[link.attr("href").drop(1)]?.takeIf { it in notes }?.html()?.trim()
            if (note.isNullOrEmpty()) {
                link.unwrap()
            } else {
                link.attr("href", FOOTNOTE_SCHEME + URLEncoder.encode(note, "UTF-8"))
            }
        }
    }

    private fun walk(parent: Element, out: MutableList<ReaderArticleBlock>, depth: Int) {
        val inline = StringBuilder()
        fun flush() {
            if (inline.isBlank()) {
                inline.clear()
                return
            }
            val text = inline.toString()
            if (Jsoup.parse(text).text().isNotBlank()) out += ReaderArticleBlock.Paragraph(text.trim())
            inline.clear()
        }
        for (node in parent.childNodes()) {
            when {
                node is TextNode -> inline.append(node.outerHtml())
                node !is Element -> Unit
                node.tagName() in SKIP -> Unit
                // inline wrappers can hold a video (WordPress's <span class="embed-youtube">) or an image
                // (Blogger's <a href="full.jpg"><img></a>), which need blocks of their own
                node.tagName() in INLINE && node.selectFirst("$MEDIA, $CONTENT_IMAGE") == null ->
                    inline.append(node.outerHtml())
                else -> {
                    flush()
                    block(node, out, depth)
                }
            }
        }
        flush()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "ReturnCount")
    private fun block(el: Element, out: MutableList<ReaderArticleBlock>, depth: Int) {
        if (el.classNames().any { it in GALLERIES }) return gallery(el, out)
        if (el.hasClass("wp-block-buttons") || el.hasClass("wp-block-button")) return buttons(el, out)
        when (el.tagName()) {
            "details" -> {
                val summary = el.children().firstOrNull { it.tagName() == "summary" }
                val title = summary?.text()?.ifEmpty { null } ?: "Details"
                summary?.remove()
                val inner = mutableListOf<ReaderArticleBlock>().also { walk(el, it, depth) }
                if (inner.isNotEmpty()) out += ReaderArticleBlock.Details(title, inner)
            }
            "p" -> {
                val images = el.select("img").mapNotNull { image(it, caption = null) }
                val media = el.select(MEDIA).mapNotNull(::media)
                el.select("img, $MEDIA").remove()
                if (el.text().isNotBlank()) out += ReaderArticleBlock.Paragraph(el.html().trim())
                out += images
                out += media
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> el.text().takeIf { it.isNotBlank() }?.let {
                out += ReaderArticleBlock.Heading(it, el.tagName()[1].digitToInt())
            }
            "img" -> image(el, el.attr("alt").takeIf { it.isNotBlank() && it.length < MAX_ALT_CAPTION_LENGTH })
                ?.let { out += it }
            "iframe", "video", "audio" -> media(el)?.let { out += it }
            "table" -> table(el, caption = null)?.let { out += it }
            "figure" -> {
                val caption = el.selectFirst("figcaption")?.text()?.takeIf { it.isNotBlank() }
                el.selectFirst("table")?.let { table ->
                    table(table, caption)?.let { out += it }
                    return
                }
                val img = el.selectFirst("img")?.let { image(it, caption) }
                // an embed WordPress couldn't render leaves just its URL in the wrapper
                val embedUrl = el.takeIf { it.hasClass("wp-block-embed") && it.selectFirst(MEDIA) == null }
                    ?.selectFirst(".wp-block-embed__wrapper")?.text()?.trim()?.takeIf { it.startsWith("http") }
                when {
                    img != null -> out += img
                    embedUrl != null -> out += ReaderVideoLinks.parse(embedUrl)
                        ?: ReaderArticleBlock.Embed(embedUrl, caption)
                    else -> walk(el, out, depth)
                }
            }
            "blockquote" -> {
                val cites = el.children().filter { it.tagName() == "cite" }
                val citation = cites.joinToString(" ") { it.text() }.takeIf { it.isNotBlank() }
                cites.forEach { it.remove() }
                val paragraphs = el.select("p")
                val html = if (paragraphs.isEmpty()) el.html() else paragraphs.joinToString("<br><br>") { it.html() }
                val pull = el.hasClass("wp-block-pullquote") || el.parent()?.hasClass("wp-block-pullquote") == true
                if (Jsoup.parse(html).text().isNotBlank()) out += ReaderArticleBlock.Quote(html.trim(), citation, pull)
            }
            "ul", "ol" -> el.children().filter { it.tagName() == "li" }.forEachIndexed { i, li ->
                val nested = li.children().filter { it.tagName() == "ul" || it.tagName() == "ol" }
                nested.forEach { it.remove() }
                if (li.text().isNotBlank()) {
                    val marker = if (el.tagName() == "ol") "${i + 1}." else "•"
                    out += ReaderArticleBlock.ListItem(li.html().trim(), marker, depth)
                }
                nested.forEach { block(it, out, depth + 1) }
            }
            "pre" -> out += ReaderArticleBlock.Code(el.wholeText().trimEnd())
            "hr" -> out += ReaderArticleBlock.Rule
            else -> walk(el, out, depth)
        }
    }

    private fun gallery(el: Element, out: MutableList<ReaderArticleBlock>) {
        val images = el.select("img").mapNotNull { img ->
            val item = img.parents().takeWhile { it !== el }.firstOrNull { it.`is`(GALLERY_ITEM) }
            val caption = item?.selectFirst("figcaption, .gallery-caption, .tiled-gallery-caption")?.text()
            image(img, caption?.takeIf { it.isNotBlank() })
        }.distinctBy { it.url }
        val caption = el.selectFirst(".blocks-gallery-caption")?.text()?.takeIf { it.isNotBlank() }
        if (images.size < 2) {
            images.singleOrNull()?.let { out += it.copy(caption = it.caption ?: caption) }
            return
        }
        // phones are too narrow for the wider layouts a site may ask for
        val declared = el.classNames().firstNotNullOfOrNull {
            GALLERY_COLUMNS.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull()
        }
        val columns = minOf(declared ?: MAX_GALLERY_COLUMNS, MAX_GALLERY_COLUMNS, images.size).coerceAtLeast(1)
        out += ReaderArticleBlock.Gallery(images, columns, caption)
    }

    private fun buttons(el: Element, out: MutableList<ReaderArticleBlock>) {
        for (link in el.select("a[href]")) {
            val url = link.absUrl("href")
            val label = link.text()
            if (url.startsWith("http") && label.isNotEmpty()) out += ReaderArticleBlock.Button(label, url)
        }
    }

    private fun table(table: Element, caption: String?): ReaderArticleBlock.Table? {
        val rows = table.select("tr").map { row ->
            row.children().filter { it.tagName() in CELLS }.map { it.html().trim() }
        }.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return null
        // header rows are those in <thead>, or a first row made only of <th>
        val headerRows = (
            table.select("thead tr").count { row -> row.children().any { it.tagName() in CELLS } }
                .takeIf { it > 0 }
                ?: if (table.selectFirst("tr")?.children()?.all { it.tagName() == "th" } == true) 1 else 0
            ).coerceAtMost(rows.size)
        return ReaderArticleBlock.Table(rows, headerRows, caption)
    }

    @Suppress("ReturnCount")
    private fun media(el: Element): ReaderArticleBlock? {
        // zero- and one-pixel frames are trackers, not content
        if (listOf(el.attr("width"), el.attr("height")).any { it == "0" || it == "1" }) return null
        val width = el.attr("width").trim().toIntOrNull()?.takeIf { it > 0 }
        val height = el.attr("height").trim().toIntOrNull()?.takeIf { it > 0 }
        val src = el.absUrl("src").ifEmpty { el.selectFirst("source[src]")?.absUrl("src").orEmpty() }
        return when (el.tagName()) {
            "audio" -> src.takeIf { it.startsWith("http") }?.let { ReaderArticleBlock.Embed(it, null) }
            "video" -> ReaderVideoLinks.parse(src, el.absUrl("poster").ifEmpty { null }, width, height)
                ?: src.takeIf { it.startsWith("http") }?.let { ReaderArticleBlock.Embed(it, null) }
            else -> {
                val frameSrc = el.absUrl("src").ifEmpty { el.absUrl("data-src") }
                if (!frameSrc.startsWith("http") || WIDGET_HOSTS.any { it in frameSrc }) return null
                ReaderVideoLinks.parse(frameSrc, width = width, height = height)
                    ?: ReaderArticleBlock.Embed(frameSrc, el.attr("title").takeIf { it.isNotBlank() })
            }
        }
    }

    @Suppress("ReturnCount")
    private fun image(img: Element, caption: String?): ReaderArticleBlock.Image? {
        val (attr, src) = listOf("data-orig-file", "data-src", "data-lazy-src", "src")
            .map { it to img.absUrl(it) }
            .firstOrNull { (_, url) -> url.isNotEmpty() && !url.startsWith("data:") }
            ?: return null
        if (img.attr("width") == "1" || img.attr("height") == "1" || TRACKERS.any { it in src }) return null
        // width/height describe the displayed (possibly cropped) size; when loading the original, WordPress's
        // data-orig-size ("w,h") gives that image's real shape
        val size = img.attr("data-orig-size").split(',').mapNotNull { it.trim().toIntOrNull() }
            .takeIf { attr == "data-orig-file" && it.size == 2 }
            ?: listOf(img.attr("width"), img.attr("height")).map { it.trim().toIntOrNull() }
        val (width, height) = size.map { it?.takeIf { d -> d > 0 } }
        return ReaderArticleBlock.Image(src, caption, width, height)
    }
}
