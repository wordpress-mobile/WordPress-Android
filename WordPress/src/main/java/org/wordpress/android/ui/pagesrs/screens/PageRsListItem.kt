package org.wordpress.android.ui.pagesrs.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.pagesrs.PageRsDisplayState
import org.wordpress.android.ui.pagesrs.PageRsUiModel
import org.wordpress.android.ui.postsrs.screens.PlaceholderItem

@Composable
internal fun PageRsListItem(
    page: PageRsUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (page.displayState) {
        PageRsDisplayState.PLACEHOLDER -> PlaceholderItem(modifier)
        PageRsDisplayState.ERROR -> ErrorItem(modifier)
        PageRsDisplayState.NORMAL,
        PageRsDisplayState.FETCHING_WITH_DATA,
        PageRsDisplayState.FAILED_WITH_DATA -> PageContentItem(page, onClick, modifier)
    }
}

@Composable
private fun PageContentItem(
    page: PageRsUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (page.date.isNotBlank()) {
                Text(
                    text = page.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (page.badges.isNotEmpty()) {
                BadgeRow(page.badges)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = page.title.ifBlank { stringResource(R.string.untitled_in_parentheses) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (page.excerpt.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = page.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (page.displayState == PageRsDisplayState.FETCHING_WITH_DATA) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun BadgeRow(badges: List<Int>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        badges.forEach { labelResId ->
            Text(
                text = stringResource(labelResId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ErrorItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = stringResource(R.string.page_rs_failed_to_load),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPageItem() {
    MaterialTheme {
        PageRsListItem(
            page = PageRsUiModel(
                remotePageId = 1L,
                title = "About",
                excerpt = "Learn more about our journey and what we do.",
                date = "Dec 15, 2025"
            ),
            onClick = {}
        )
    }
}
