package org.wordpress.android.support.he.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.he.model.ConversationStatus

@Composable
fun ConversationStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val conversationStatus = ConversationStatus.fromStatus(status)
    val (statusText, backgroundColor, textColor) = when (conversationStatus) {
        ConversationStatus.WAITING_FOR_SUPPORT -> Triple(
            stringResource(R.string.he_support_status_waiting_for_support),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        ConversationStatus.WAITING_FOR_USER -> Triple(
            stringResource(R.string.he_support_status_waiting_for_user),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ConversationStatus.SOLVED -> Triple(
            stringResource(R.string.he_support_status_solved),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        ConversationStatus.CLOSED -> Triple(
            stringResource(R.string.he_support_status_closed),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        ConversationStatus.UNKNOWN -> Triple(
            stringResource(R.string.he_support_status_unknown),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
