package org.wordpress.android.ui.newstats.mostviewed

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

private val FaviconSize = 20.dp
private val FaviconCorner = 4.dp

/** The gap between a row's leading content and its title. */
internal val LeadingContentGap = 12.dp

/** The glyph tint matches the one the post thumbnails use, so the two read as the same family. */
private const val GLYPH_ALPHA = 0.6f

/**
 * The icon WP.com hands out for the "Search Engines" referrer group. It is a bitmap sized for the
 * web, so the app substitutes its own vector -- the same swap old Stats made.
 */
private const val SEARCH_ENGINE_ICON_URL = "https://wordpress.com/i/stats/search-engine.png"

/**
 * The favicon at the start of a referrer or click row.
 *
 * Nothing is drawn when the API supplied no icon, which is most of the time for referrers it only
 * knows by hostname. That matches the web, which leaves those rows' titles flush against the start
 * of the row rather than reserving an empty square -- unlike the Posts & Pages rows, where a type
 * glyph is always worth showing.
 *
 * When the API does give a URL that then fails to load, the slot keeps a neutral globe rather than
 * going blank: the row has already reserved the space, and an empty square beside a sibling that
 * resolved its icon reads as a load that never finished.
 *
 * No content description: the referrer's name is right beside it.
 */
@Composable
internal fun StatsFavicon(
    iconUrl: String,
    modifier: Modifier = Modifier
) {
    if (iconUrl == SEARCH_ENGINE_ICON_URL) {
        Icon(
            painter = painterResource(R.drawable.ic_search_white_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GLYPH_ALPHA),
            modifier = modifier.size(FaviconSize)
        )
        return
    }
    var loadFailed by remember(iconUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(FaviconSize)
            .clip(RoundedCornerShape(FaviconCorner)),
        contentAlignment = Alignment.Center
    ) {
        if (loadFailed) {
            Icon(
                painter = painterResource(R.drawable.ic_globe_white_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GLYPH_ALPHA),
                modifier = Modifier.size(FaviconSize)
            )
        } else {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { loadFailed = it is AsyncImagePainter.State.Error },
                modifier = Modifier.size(FaviconSize)
            )
        }
    }
}

/**
 * The leading slot for a row that may carry a favicon, or null when it has none so the row draws
 * no leading content at all.
 */
internal fun faviconSlot(iconUrl: String?): (@Composable () -> Unit)? =
    iconUrl?.takeIf { it.isNotBlank() }?.let { url -> { StatsFavicon(url) } }
