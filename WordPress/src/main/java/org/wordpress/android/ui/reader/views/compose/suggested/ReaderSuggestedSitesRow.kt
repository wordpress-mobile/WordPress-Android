package org.wordpress.android.ui.reader.views.compose.suggested

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.R
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.repository.ReaderSuggestedSiteItem
import org.wordpress.android.ui.reader.repository.ReaderSuggestedSitesStore
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.ReaderFollowToggleButton
import org.wordpress.android.util.UrlUtils

private val CARD_WIDTH = 156.dp
private val ICON_SIZE = 48.dp
private const val NAME_MAX_LINES = 2

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReaderSuggestedSitesEntryPoint {
    fun suggestedSitesStore(): ReaderSuggestedSitesStore
    fun readerTracker(): ReaderTracker
}

/**
 * A titled row of sites to subscribe to, from WordPress.com's suggestions. Tapping a site previews it; its button
 * subscribes. Shows nothing until there are suggestions, and nothing at all when signed out.
 *
 * [source] is the analytics source for follows and previews from here. [contentPadding] lines the cards up with the
 * screen's own content while still scrolling to its edges.
 */
@Composable
fun ReaderSuggestedSitesRow(
    source: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.reader_suggested_sites),
    contentPadding: Dp = 16.dp,
) {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, ReaderSuggestedSitesEntryPoint::class.java)
    }
    val store = entryPoint.suggestedSitesStore()
    LaunchedEffect(store) { store.refreshIfStale() }
    val sites by store.sites.collectAsState()
    if (sites.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = contentPadding, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = contentPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sites, key = { it.key }) { item ->
                SuggestedSiteCard(
                    item = item,
                    onClick = {
                        val site = item.site
                        if (site.blogId != 0L || site.feedId != 0L) {
                            ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                                context,
                                site.blogId,
                                site.feedId,
                                item.isFollowing,
                                source,
                                entryPoint.readerTracker()
                            )
                        }
                    },
                    onFollowClick = { store.toggleFollow(item, source) }
                )
            }
        }
    }
}

@Composable
private fun SuggestedSiteCard(item: ReaderSuggestedSiteItem, onClick: () -> Unit, onFollowClick: () -> Unit) {
    val site = item.site
    val host = remember(site.url) { UrlUtils.getHost(site.url) }
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(CARD_WIDTH)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        AsyncImage(
            model = site.iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(ICON_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = site.name ?: host,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = NAME_MAX_LINES,
            minLines = NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = host,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .height(18.dp)
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            ReaderFollowToggleButton(item.isFollowing, item.isFollowEnabled, onFollowClick)
        }
    }
}
