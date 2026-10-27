package org.wordpress.android.ui.reader.views.compose.postlist

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListItem
import org.wordpress.android.ui.reader.viewmodels.ReaderSiteHeaderUiState
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.BlavatarShape
import org.wordpress.android.util.image.ImagePlaceholderManager
import org.wordpress.android.util.image.ImageType
import java.util.Locale

private const val MINIMUM_SUBSCRIBERS_COMPACT_FORMAT = 10_000
private const val DESCRIPTION_MAX_LINES = 3
private const val HEADER_TITLE_MAX_LINES = 5
private const val MEDIUM_EMPHASIS_ALPHA = 0.6f
private const val HIGH_EMPHASIS_ALPHA = 0.87f
private const val X_POST_SUBTITLE_MAX_LINES = 2
private const val X_POST_TITLE_PREFIX = "X-post: "

// Stateless, so one instance serves every avatar
private val imagePlaceholders = ImagePlaceholderManager()

@Composable
private fun ReaderPostListRowSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, content = content)
}

/**
 * The blog or feed being previewed, shown above its posts
 */
@Composable
internal fun ReaderSiteHeader(
    info: ReaderSiteHeaderUiState,
    onFollowClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val blavatarSize = dimensionResource(R.dimen.blavatar_sz_extra_large)
    ReaderPostListRowSurface {
        Column(modifier = Modifier.padding(Margin.ExtraLarge.value)) {
            info.imageUrl?.let { imageUrl ->
                val sizePx = with(LocalDensity.current) { blavatarSize.roundToPx() }
                AsyncImage(
                    model = imageRequest(
                        url = PhotonUtils.getPhotonImageUrl(imageUrl, sizePx, sizePx, PhotonUtils.Quality.HIGH),
                        imageType = SiteUtils.getSiteImageType(info.isP2orA8C, BlavatarShape.CIRCULAR)
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = Margin.ExtraLarge.value)
                        .size(blavatarSize)
                        .clip(CircleShape)
                )
            }
            val host = info.host
            Text(
                text = info.name ?: host ?: stringResource(R.string.reader_untitled_post),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = HEADER_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            if (host != null) {
                Text(
                    text = host,
                    modifier = Modifier.padding(top = Margin.Medium.value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            info.description?.let { description ->
                Text(
                    text = description,
                    modifier = Modifier.padding(top = Margin.Medium.value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                    maxLines = DESCRIPTION_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subscriberCountText(info.subscriberCount),
                modifier = Modifier.padding(top = Margin.Medium.value, bottom = Margin.ExtraLarge.value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FollowButton(
                    isFollowed = info.isFollowed,
                    isInProgress = info.isFollowInProgress,
                    onClick = onFollowClick
                )
                if (info.showSettings) {
                    IconButton(
                        onClick = onSettingsClick,
                        enabled = !info.isFollowInProgress,
                        modifier = Modifier.padding(start = Margin.Medium.value)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cog_white_24dp),
                            contentDescription = stringResource(R.string.reader_subscription_settings_title),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun subscriberCountText(count: Int): String {
    // Reference: pcdRpT-3BI-p2#comment-5978
    val formatted = remember(count) {
        if (count >= MINIMUM_SUBSCRIBERS_COMPACT_FORMAT) {
            CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
                .format(count)
        } else {
            NumberFormat.getInstance().format(count)
        }
    }
    val res = if (count == 1) {
        R.string.reader_label_subscribers_count_single
    } else {
        R.string.reader_label_subscribers_count
    }
    return stringResource(res, formatted)
}

/**
 * The tag being previewed, shown above its posts
 */
@Composable
internal fun ReaderTagHeader(item: ReaderPostListItem.TagHeader, onFollowClick: () -> Unit) {
    // creative-writing -> Creative Writing, in the app's language rather than the device's
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(item.label, locale) {
        item.label.split("-").joinToString(separator = " ") { word ->
            word.replaceFirstChar { it.titlecase(locale) }
        }
    }
    ReaderPostListRowSurface {
        Column(modifier = Modifier.padding(Margin.ExtraLarge.value)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = HEADER_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Margin.Medium.value))
            FollowButton(
                isFollowed = item.isFollowed,
                isInProgress = item.isFollowActionRunning,
                onClick = onFollowClick
            )
        }
    }
}

@Composable
private fun FollowButton(isFollowed: Boolean, isInProgress: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        if (isFollowed) {
            OutlinedButton(onClick = onClick, enabled = !isInProgress) {
                Text(stringResource(R.string.reader_btn_subscribed))
            }
        } else {
            Button(onClick = onClick, enabled = !isInProgress) {
                Text(stringResource(R.string.reader_btn_subscribe))
            }
        }
        if (isInProgress) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

/**
 * Marks a gap in time between two posts, which can be filled in by loading the posts in between
 */
@Composable
internal fun ReaderGapMarker(isFilling: Boolean, onClick: () -> Unit) {
    val tear = ImageBitmap.imageResource(R.drawable.reader_tear)
    val tearBrush = remember(tear) { ShaderBrush(ImageShader(tear, TileMode.Repeated, TileMode.Clamp)) }
    val tearHeight = with(LocalDensity.current) { tear.height.toDp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Margin.Medium.value)
    ) {
        // a torn-paper strip across the row, behind the button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tearHeight)
                .background(tearBrush)
        )
        if (isFilling) {
            CircularProgressIndicator()
        } else {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(4.dp),
                color = colorResource(R.color.neutral_50),
                contentColor = Color.White
            ) {
                Text(
                    text = stringResource(R.string.reader_label_gap_marker),
                    modifier = Modifier.padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.Medium.value),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * A cross-post (a post or comment cross-posted from one P2 to another)
 */
@Composable
internal fun ReaderXPostRow(post: ReaderPost, onClick: () -> Unit) {
    val subtitle = remember(post) { xPostSubtitle(post) }
    ReaderPostListRowSurface(modifier = Modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.reader_card_content_padding))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    XPostAvatar(
                        avatarUrl = post.postAvatar,
                        imageType = ImageType.AVATAR_WITH_BACKGROUND
                    )
                    XPostAvatar(
                        avatarUrl = post.blogImageUrl,
                        imageType = SiteUtils.getSiteImageType(post.isP2orA8C, BlavatarShape.CIRCULAR),
                        modifier = Modifier.padding(
                            start = dimensionResource(R.dimen.reader_a8c_avatar_margin_start_new)
                        )
                    )
                }
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(start = Margin.Medium.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = HIGH_EMPHASIS_ALPHA),
                    maxLines = X_POST_SUBTITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = post.title.removePrefix(X_POST_TITLE_PREFIX),
                modifier = Modifier.padding(top = Margin.Medium.value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = DESCRIPTION_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun XPostAvatar(avatarUrl: String, imageType: ImageType, modifier: Modifier = Modifier) {
    val size = dimensionResource(R.dimen.avatar_sz_extra_small)
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val url = remember(avatarUrl, sizePx) { WPAvatarUtils.rewriteAvatarUrl(avatarUrl, sizePx) }
    AsyncImage(
        model = imageRequest(url, imageType),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .clip(CircleShape)
    )
}

@Composable
private fun imageRequest(url: String?, imageType: ImageType) = ImageRequest.Builder(LocalContext.current)
    .data(url)
    .apply {
        imagePlaceholders.getPlaceholderResource(imageType)?.let { placeholder(it) }
        imagePlaceholders.getErrorResource(imageType)?.let { error(it) }
    }
    .build()

// note that these strings don't need to be localized due to the intended audience
private const val UNKNOWN_SITE = "(unknown)"
private const val FMT_SITE_XPOST = "%1\$s cross-posted from %2\$s to %3\$s"
private const val FMT_COMMENT_XPOST = "%1\$s commented on %2\$s, cross-posted to %3\$s"

/*
 * ex: "Nick cross-posted from +blog1 to +blog2"
 * ex: "Nick commented on +blog1, cross-posted to +blog2"
 */
private fun xPostSubtitle(post: ReaderPost): AnnotatedString {
    val isCommentXPost = post.excerpt.startsWith("X-comment")
    val name = if (post.hasAuthorFirstName()) post.authorFirstName else post.authorName
    val html = String.format(
        if (isCommentXPost) FMT_COMMENT_XPOST else FMT_SITE_XPOST,
        "<strong>$name</strong>",
        "<strong>${xPostFromSiteName(post)}</strong>",
        "<strong>${xPostToSiteName(post)}</strong>"
    )
    return AnnotatedString.fromHtml(html)
}

// the origin site is in the excerpt, ex: "<p>X-post from +blog2: I have a request..."
private fun xPostFromSiteName(post: ReaderPost): String {
    val excerpt = post.excerpt
    val plusPos = excerpt.indexOf("+")
    val colonPos = excerpt.indexOf(":", plusPos)
    return if (plusPos > 0 && colonPos > 0) excerpt.substring(plusPos, colonPos) else UNKNOWN_SITE
}

// the destination site is the subdomain of the blog url
private fun xPostToSiteName(post: ReaderPost): String {
    val domain = Uri.parse(post.blogUrl).host
    return if (domain == null || !domain.contains(".")) {
        "+$UNKNOWN_SITE"
    } else {
        "+" + domain.substring(0, domain.indexOf("."))
    }
}

/**
 * A post that was removed from Saved - tapping it saves it again
 */
@Composable
internal fun ReaderRemovedPostRow(post: ReaderPost, onClick: () -> Unit) {
    val removed = stringResource(R.string.removed)
    // "Removed [post title]" with the title in bold
    val text = remember(removed, post.title) {
        buildAnnotatedString {
            append(removed)
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(post.title) }
        }
    }
    val padding = dimensionResource(R.dimen.reader_card_content_padding)
    ReaderPostListRowSurface(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = padding)
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Margin.Large.value),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = HIGH_EMPHASIS_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(R.drawable.ic_undo_white_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Margin.Small.value))
            Text(
                text = stringResource(R.string.undo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
