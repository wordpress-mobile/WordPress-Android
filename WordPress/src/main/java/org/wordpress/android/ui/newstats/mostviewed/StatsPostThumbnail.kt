package org.wordpress.android.ui.newstats.mostviewed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import org.wordpress.android.R

// Sized well under the featured image on the Latest Post card, which is a hero: here the glyph is
// a hint at what kind of thing the row is, not something to look at, so it sits quietly to the left
// of the title rather than competing with it.
private val ThumbnailSize = 32.dp
private val ThumbnailCorner = 6.dp
private val ThumbnailIconSize = 16.dp

/** Keeps the empty-state fill a wash behind the glyph rather than a solid block. */
private const val PLACEHOLDER_ALPHA = 0.4f

/** The glyph reads as secondary to the title, so it doesn't take the full on-surface tint. */
private const val GLYPH_ALPHA = 0.6f

/** The one item type the stats API reports that gets the post glyph; see [postTypeIconRes]. */
private const val TYPE_POST = "post"

/**
 * The glyph drawn for a post or page that has no featured image.
 *
 * Only a genuine post gets the post glyph; pages, the homepage, attachments and anything
 * unrecognised get the page one. That is the same grouping old Stats used, so a site's list looks
 * the way it always has.
 */
@DrawableRes
internal fun postTypeIconRes(postType: String?): Int =
    if (postType?.lowercase() == TYPE_POST) {
        R.drawable.ic_posts_white_24dp
    } else {
        R.drawable.ic_pages_white_24dp
    }

/**
 * The leading square on a Posts & Pages row: the post's featured image, or its type glyph when it
 * has none.
 *
 * The glyph is drawn underneath rather than in an `else` branch, so it also stands in while the
 * image is still loading and stays put if the image can't be loaded at all -- a row is never left
 * with an empty box. It is dropped once the image has actually loaded, because a featured image
 * with transparency would otherwise leave the glyph showing through for good.
 *
 * No content description: the post's title sits immediately beside it, so announcing the thumbnail
 * as well would only repeat it.
 */
@Composable
internal fun StatsPostThumbnail(
    imageUrl: String?,
    postType: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(ThumbnailCorner)
    var imageLoaded by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PLACEHOLDER_ALPHA)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageLoaded) {
            Icon(
                painter = painterResource(postTypeIconRes(postType)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GLYPH_ALPHA),
                modifier = Modifier.size(ThumbnailIconSize)
            )
        }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { imageLoaded = it is AsyncImagePainter.State.Success },
                modifier = Modifier.size(ThumbnailSize)
            )
        }
    }
}
