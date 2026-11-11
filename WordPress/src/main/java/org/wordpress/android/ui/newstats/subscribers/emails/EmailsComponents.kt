package org.wordpress.android.ui.newstats.subscribers.emails

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.util.formatEmailStat

@Composable
internal fun EmailColumnHeaders() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.stats_emails_latest_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(
                R.string.stats_emails_opens_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(
                R.string.stats_emails_clicks_header
            ),
            style = MaterialTheme
                .typography.labelMedium,
            color = MaterialTheme
                .colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider(
        color = MaterialTheme
            .colorScheme.outlineVariant
    )
}

@Composable
internal fun EmailItemRow(item: EmailListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme
                .colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatEmailStat(item.opens),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (item.opens == 0L) {
                MaterialTheme
                    .colorScheme.onSurfaceVariant
            } else {
                MaterialTheme
                    .colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = formatEmailStat(item.clicks),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (item.clicks == 0L) {
                MaterialTheme
                    .colorScheme.onSurfaceVariant
            } else {
                MaterialTheme
                    .colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
    }
}
