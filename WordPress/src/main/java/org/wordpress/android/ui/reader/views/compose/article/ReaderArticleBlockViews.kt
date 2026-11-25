package org.wordpress.android.ui.reader.views.compose.article

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.wordpress.android.ui.reader.views.compose.readableWidth

/**
 * How the post's images are loaded and opened: [url] sizes an image's URL for a width in pixels (Photon, and the
 * private-post rules), and [onClick] opens it in the photo viewer
 */
class ReaderArticleImages(
    val url: (url: String, widthPx: Int) -> String = { url, _ -> url },
    val onClick: (url: String) -> Unit = {},
)

internal val LocalReaderArticleImages = staticCompositionLocalOf { ReaderArticleImages() }

private val TEXT_PADDING = 22.dp
private val MEDIA_PADDING = 14.dp
private val GALLERY_GAP = 6.dp
private val MAX_COLUMN_WIDTH = 240.dp
private val MIN_IMAGE_HEIGHT = 120.dp
private const val ACCENT_PLACEHOLDER_ALPHA = 0.25f
private const val BODY_LINE_HEIGHT = 1.62f
private const val MAJOR_HEADING_SCALE = 1.3f
private const val MINOR_HEADING_SCALE = 1.12f
private const val QUOTE_SCALE = 1.06f
private const val NOTE_SCALE = 0.94f

/**
 * One block of the post's body, as WPFolio lays it out. [accent] tints rules, quotes and placeholders; [isActive]
 * is false once the post isn't the one on screen, which stops any video playing in it.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
internal fun ReaderArticleBlockView(block: ReaderArticleBlock, accent: Color, isActive: Boolean) {
    val colors = articleColors
    val links = rememberLinkStyles()
    val padded = Modifier
        .fillMaxWidth()
        .padding(horizontal = TEXT_PADDING, vertical = 7.dp)
    when (block) {
        is ReaderArticleBlock.Paragraph -> Text(
            rememberHtml(block.html, links),
            style = body(BODY_SIZE, lineHeight = BODY_LINE_HEIGHT),
            color = colors.body,
            modifier = padded,
        )
        is ReaderArticleBlock.Heading -> Text(
            block.text,
            style = ui(
                bodySize() * if (block.level <= 2) MAJOR_HEADING_SCALE else MINOR_HEADING_SCALE,
                weight = 600,
                lineHeight = 1.25f,
                tracking = -0.01f,
            ),
            color = colors.text,
            modifier = padded.padding(top = 10.dp),
        )
        is ReaderArticleBlock.Image -> Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            ArticleImage(
                block,
                accent,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MEDIA_PADDING)
            )
            block.caption?.let { Caption(it) }
        }
        is ReaderArticleBlock.Gallery -> Gallery(block, accent)
        is ReaderArticleBlock.Quote -> Quote(block, accent, links, padded)
        is ReaderArticleBlock.Table -> Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            TableGrid(block, links, Modifier.padding(horizontal = MEDIA_PADDING))
            block.caption?.let { Caption(it) }
        }
        is ReaderArticleBlock.ListItem -> Row(padded.padding(start = (block.depth * 18).dp)) {
            val style = body(BODY_SIZE, lineHeight = BODY_LINE_HEIGHT)
            Text(block.marker, style = style, color = colors.textSecondary, modifier = Modifier.width(24.dp))
            Text(rememberHtml(block.html, links), style = style, color = colors.body)
        }
        // scrolls sideways rather than wrapping, as code does on the web
        is ReaderArticleBlock.Code -> Text(
            block.text,
            style = ui(14f, lineHeight = null).copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
            color = colors.body,
            softWrap = false,
            modifier = padded
                .clip(RoundedCornerShape(12.dp))
                .background(colors.divider)
                .horizontalScroll(rememberScrollState())
                .padding(14.dp),
        )
        is ReaderArticleBlock.Video -> ReaderArticleVideo(block, accent, isActive)
        is ReaderArticleBlock.Embed -> ReaderArticleEmbedCard(block)
        ReaderArticleBlock.Rule -> HorizontalDivider(padded.padding(vertical = 12.dp), color = colors.divider)
        is ReaderArticleBlock.Button -> {
            val uriHandler = LocalUriHandler.current
            Box(padded.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(
                    block.label,
                    style = ui(15f, 600),
                    color = colors.onInverse,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.inverse)
                        .clickable { uriHandler.openUri(block.url) }
                        .padding(horizontal = 24.dp, vertical = 13.dp),
                )
            }
        }
        is ReaderArticleBlock.Details -> DetailsBlock(block, accent, isActive)
    }
}

/**
 * An image at the width it's shown, with its height reserved up front when the HTML gives its size, so the text
 * below doesn't jump as it loads
 */
@Composable
private fun ArticleImage(block: ReaderArticleBlock.Image, accent: Color, modifier: Modifier) {
    val images = LocalReaderArticleImages.current
    BoxWithConstraints(modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val url = remember(block.url, widthPx) { images.url(block.url, widthPx) }
        val ratio = block.aspectRatio
        AsyncImage(
            model = url,
            contentDescription = block.caption,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.heightIn(min = MIN_IMAGE_HEIGHT))
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = ACCENT_PLACEHOLDER_ALPHA))
                .clickable { images.onClick(block.url) },
        )
    }
}

@Composable
private fun Quote(block: ReaderArticleBlock.Quote, accent: Color, links: TextLinkStyles, padded: Modifier) {
    val colors = articleColors
    if (block.pull) {
        // a pullquote stands apart: centred and larger, under a short accent rule
        Column(padded.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                rememberHtml(block.html, links),
                style = ui(bodySize() * MAJOR_HEADING_SCALE, 600, lineHeight = 1.3f, tracking = -0.01f),
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            block.citation?.let { Citation(it, Modifier.padding(top = 12.dp)) }
        }
    } else {
        Row(
            padded
                .padding(vertical = 4.dp)
                .height(IntrinsicSize.Min)
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent)
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    rememberHtml(block.html, links),
                    style = body(BODY_SIZE * QUOTE_SCALE, lineHeight = 1.55f).copy(fontStyle = FontStyle.Italic),
                    color = colors.body,
                )
                block.citation?.let { Citation(it, Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

/** A grid of square crops; a short last row stretches to fill the width, as it does on the web */
@Composable
private fun Gallery(block: ReaderArticleBlock.Gallery, accent: Color) {
    val images = LocalReaderArticleImages.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MEDIA_PADDING)
        ) {
            val cell = (maxWidth - GALLERY_GAP * (block.columns - 1)) / block.columns
            val cellPx = with(LocalDensity.current) { maxWidth.roundToPx() / block.columns }
            Column(verticalArrangement = Arrangement.spacedBy(GALLERY_GAP)) {
                block.images.chunked(block.columns).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(cell),
                        horizontalArrangement = Arrangement.spacedBy(GALLERY_GAP)
                    ) {
                        row.forEach { image ->
                            // a short last row's cells are wider, so they ask for the full width
                            val widthPx = if (row.size < block.columns) cellPx * block.columns else cellPx
                            AsyncImage(
                                model = remember(image.url, widthPx) { images.url(image.url, widthPx) },
                                contentDescription = image.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = ACCENT_PLACEHOLDER_ALPHA))
                                    .clickable { images.onClick(image.url) },
                            )
                        }
                    }
                }
            }
        }
        block.caption?.let { Caption(it) }
    }
}

/** A collapsed `<details>` section: its summary, and its blocks once tapped open */
@Composable
private fun DetailsBlock(block: ReaderArticleBlock.Details, accent: Color, isActive: Boolean) {
    val colors = articleColors
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        HorizontalDivider(Modifier.padding(horizontal = TEXT_PADDING), color = colors.divider)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { isOpen = !isOpen }
                .padding(horizontal = TEXT_PADDING, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                block.summary,
                style = ui(bodySize() * NOTE_SCALE, 600),
                color = colors.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        }
        if (isOpen) block.blocks.forEach { ReaderArticleBlockView(it, accent, isActive) }
        HorizontalDivider(Modifier.padding(horizontal = TEXT_PADDING), color = colors.divider)
    }
}

/**
 * Columns are as wide as their widest cell, up to [MAX_COLUMN_WIDTH], and stretch to fill the width when there's
 * room; wider tables scroll sideways, like code blocks
 */
@Suppress("LongMethod")
@Composable
private fun TableGrid(table: ReaderArticleBlock.Table, links: TextLinkStyles, modifier: Modifier = Modifier) {
    val colors = articleColors
    val shape = RoundedCornerShape(12.dp)
    val rows = table.rows.size
    val columns = table.rows.maxOf { it.size }
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.divider, shape)
            .horizontalScroll(rememberScrollState()),
        propagateMinConstraints = true,
    ) {
        Layout(
            content = {
                repeat(table.headerRows) { Box(Modifier.background(colors.divider)) }
                table.rows.forEachIndexed { r, row ->
                    repeat(columns) { c ->
                        Text(
                            rememberHtml(row.getOrNull(c).orEmpty(), links),
                            style = ui(14f, if (r < table.headerRows) 600 else 400, lineHeight = 1.4f),
                            color = colors.body,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        )
                    }
                }
                repeat(rows - 1) { Box(Modifier.background(colors.divider)) }
            },
        ) { measurables, constraints ->
            val headers = measurables.take(table.headerRows)
            val cells = measurables.subList(table.headerRows, table.headerRows + rows * columns)
            val lines = measurables.takeLast(rows - 1)
            val widths = IntArray(columns) { c ->
                (0 until rows).maxOf { r -> cells[r * columns + c].maxIntrinsicWidth(Constraints.Infinity) }
                    .coerceAtMost(MAX_COLUMN_WIDTH.roundToPx())
            }
            val spare = constraints.minWidth - widths.sum()
            if (spare > 0) {
                for (c in widths.indices) widths[c] += spare / columns + if (c < spare % columns) 1 else 0
            }
            val placed = cells.mapIndexed { i, cell -> cell.measure(Constraints.fixedWidth(widths[i % columns])) }
            val heights = IntArray(rows) { r -> (0 until columns).maxOf { placed[r * columns + it].height } }
            val width = widths.sum()
            val line = 1.dp.roundToPx()
            val headerBackgrounds = headers.mapIndexed { r, header ->
                header.measure(Constraints.fixed(width, heights[r]))
            }
            val dividers = lines.map { it.measure(Constraints.fixed(width, line)) }
            layout(width, heights.sum()) {
                var y = 0
                for (r in 0 until rows) {
                    headerBackgrounds.getOrNull(r)?.place(0, y)
                    var x = 0
                    for (c in 0 until columns) {
                        placed[r * columns + c].place(x, y)
                        x += widths[c]
                    }
                    y += heights[r]
                    dividers.getOrNull(r)?.place(0, y - line)
                }
            }
        }
    }
}

/** A footnote's text, shown in a sheet when its marker is tapped */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderArticleFootnoteSheet(html: String, onDismiss: () -> Unit) {
    val colors = articleColors
    val links = rememberLinkStyles()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.sheet) {
        Text(
            rememberHtml(html, links),
            style = body(BODY_SIZE * NOTE_SCALE, lineHeight = 1.55f),
            color = colors.body,
            modifier = Modifier
                .readableWidth()
                .padding(start = TEXT_PADDING, end = TEXT_PADDING, bottom = 36.dp),
        )
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = ui(13f, 500),
        color = articleColors.textSecondary,
        modifier = Modifier.padding(start = TEXT_PADDING, end = TEXT_PADDING, top = 8.dp),
    )
}

@Composable
private fun Citation(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ui(13.5f, 500), color = articleColors.textSecondary, modifier = modifier)
}

/** Converted once per block rather than on every recomposition or scroll back into view */
@Composable
internal fun rememberHtml(html: String, links: TextLinkStyles): AnnotatedString =
    remember(html, links) { AnnotatedString.fromHtml(html, linkStyles = links) }

@Composable
internal fun rememberLinkStyles(): TextLinkStyles {
    val color = articleColors.text
    return remember(color) { TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline)) }
}

/** The horizontal padding text blocks use, for the screen's own rows to line up with */
internal val ReaderArticleTextPadding: Dp = TEXT_PADDING
