package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.components.StatsCardContainer
import org.wordpress.android.ui.newstats.components.StatsCardHeader

private val CardPadding = 16.dp
private val PlaceholderHeight = 120.dp

@Suppress("LongParameterList")
@Composable
fun SubscribersGraphCard(
    onRemoveCard: () -> Unit,
    modifier: Modifier = Modifier,
    cardPosition: CardPosition? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    StatsCardContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding)
        ) {
            StatsCardHeader(
                titleResId =
                    R.string.stats_subscribers_graph,
                onRemoveCard = onRemoveCard,
                cardPosition = cardPosition,
                onMoveUp = onMoveUp,
                onMoveToTop = onMoveToTop,
                onMoveDown = onMoveDown,
                onMoveToBottom = onMoveToBottom
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlaceholderHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        R.string.stats_no_data_yet
                    ),
                    style = MaterialTheme
                        .typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
