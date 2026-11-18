package org.wordpress.android.ui.newstats.authors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.newstats.components.StatsDetailListItem
import org.wordpress.android.ui.newstats.components.StatsExpandChevron
import org.wordpress.android.ui.newstats.components.StatsListItem
import org.wordpress.android.ui.newstats.components.StatsListRowContainer
import org.wordpress.android.ui.newstats.util.formatStatValue

private val PostsStartPadding = 24.dp

/**
 * An author row that expands to list that author's posts with their views, each opening the post's
 * detail stats -- what old Stats' Authors block did (`AuthorsUseCase`). An author the API reports
 * no posts for gets no chevron and does not react to a tap, as in old Stats.
 *
 * Shared by [AuthorsCard] and [AuthorsDetailActivity]; they differ only by the leading [position]
 * number (detail only) and the [postsStartPadding] indent of the posts.
 *
 * The caller is expected to wrap this row in a `key(...)` so the [rememberSaveable] expanded state
 * is scoped to the author: it then survives a configuration change and isn't handed to whatever
 * author later takes this position.
 */
@Composable
internal fun ExpandableAuthorRow(
    author: AuthorUiItem,
    percentage: Float,
    onPostClick: (AuthorPostUiItem) -> Unit,
    position: Int? = null,
    postsStartPadding: Dp = PostsStartPadding
) {
    val isExpandable = author.posts.isNotEmpty()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rowModifier = if (isExpandable) {
        Modifier.clickable { expanded = !expanded }
    } else {
        Modifier
    }
    val chevron: (@Composable () -> Unit)? = if (isExpandable) {
        { StatsExpandChevron(expanded = expanded) }
    } else {
        null
    }
    val avatar: @Composable () -> Unit = {
        AuthorAvatar(avatarUrl = author.avatarUrl, name = author.name)
    }

    Column {
        if (position != null) {
            StatsDetailListItem(
                position = position,
                percentage = percentage,
                name = author.name,
                views = author.views,
                change = author.change,
                icon = avatar,
                modifier = rowModifier,
                viewsLeading = chevron
            )
        } else {
            StatsListItem(
                percentage = percentage,
                name = author.name,
                views = author.views,
                change = author.change,
                icon = avatar,
                modifier = rowModifier,
                viewsLeading = chevron
            )
        }

        AuthorPostsList(
            posts = author.posts,
            expanded = expanded,
            startPadding = postsStartPadding,
            onPostClick = onPostClick
        )
    }
}

/**
 * The indented, animated list of posts revealed when an author row is expanded. Each post's bar is
 * filled relative to the most viewed post of that author, so one author's spread reads on its own
 * rather than against the whole site's.
 */
@Composable
private fun AuthorPostsList(
    posts: List<AuthorPostUiItem>,
    expanded: Boolean,
    startPadding: Dp,
    onPostClick: (AuthorPostUiItem) -> Unit
) {
    if (posts.isEmpty()) return
    AnimatedVisibility(visible = expanded) {
        val maxPostViews = posts.maxOfOrNull { it.views } ?: 0L
        Column(
            modifier = Modifier.padding(start = startPadding, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            posts.forEach { post ->
                val percentage = if (maxPostViews > 0) {
                    post.views.toFloat() / maxPostViews.toFloat()
                } else 0f
                AuthorPostRow(
                    post = post,
                    percentage = percentage,
                    onClick = { onPostClick(post) }
                )
            }
        }
    }
}

@Composable
private fun AuthorPostRow(
    post: AuthorPostUiItem,
    percentage: Float,
    onClick: () -> Unit
) {
    StatsListRowContainer(
        percentage = percentage,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatStatValue(post.views),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
