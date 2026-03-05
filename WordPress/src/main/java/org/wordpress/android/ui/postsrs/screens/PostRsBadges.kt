package org.wordpress.android.ui.postsrs.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
internal fun BadgeRow(
    badges: List<Int>,
    modifier: Modifier = Modifier
) {
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
                        color = MaterialTheme
                            .colorScheme.tertiary
                            .copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(
                        horizontal = 6.dp,
                        vertical = 2.dp
                    )
            )
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun PreviewStickyBadge() {
    MaterialTheme {
        BadgeRow(
            badges = listOf(R.string.post_status_sticky)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPendingReviewBadge() {
    MaterialTheme {
        BadgeRow(
            badges = listOf(
                R.string.post_status_pending_review
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPrivateBadge() {
    MaterialTheme {
        BadgeRow(
            badges = listOf(
                R.string.post_status_post_private
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMultipleBadges() {
    MaterialTheme {
        BadgeRow(
            badges = listOf(
                R.string.post_status_post_private,
                R.string.post_status_sticky
            )
        )
    }
}

// endregion
