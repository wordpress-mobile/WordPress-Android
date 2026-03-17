package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun NoConnectionContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(
                    R.drawable.ic_wifi_off_24px
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme
                            .colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(12.dp),
                tint = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(
                    R.string.no_connection_error_title
                ),
                style = MaterialTheme
                    .typography.titleMedium,
                color = MaterialTheme
                    .colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string
                        .no_connection_error_description
                ),
                style = MaterialTheme
                    .typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
