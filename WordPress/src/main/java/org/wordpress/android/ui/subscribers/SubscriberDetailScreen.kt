package org.wordpress.android.ui.subscribers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage
import org.wordpress.android.ui.subscribers.SubscribersViewModel.Companion.displayNameOrEmail
import uniffi.wp_api.Subscriber
import java.util.Date

@Composable
fun SubscriberDetailScreen(
    subscriber: Subscriber,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(subscriber)

        Spacer(modifier = Modifier.height(24.dp))

        EmailStatsCard()

        Spacer(modifier = Modifier.height(16.dp))

        NewsletterSubscriptionCard(subscriber)

        Spacer(modifier = Modifier.height(16.dp))

        SubscriberDetailsCard(subscriber)

        Spacer(modifier = Modifier.height(32.dp))

        DeleteSubscriberButton()
    }
}

@Composable
fun ProfileHeader(
    subscriber: Subscriber
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            RemoteImage(
                imageUrl = subscriber.avatar,
                fallbackImageRes = R.drawable.ic_user_placeholder_primary_24,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Text(
                text = subscriber.displayNameOrEmail(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
        Row {
            Text(
                text = subscriber.emailAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        // TODO remove this once we have actual data
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Text(
                text = "Note: Displaying dummy data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmailStatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Email,
                label = stringResource(R.string.subscribers_emails_sent_label),
                value = "100"
            )
            StatItem(
                icon = Icons.Default.MailOutline,
                label = stringResource(R.string.subscribers_opened_label),
                value = "10"
            )
            StatItem(
                icon = Icons.Default.Check,
                label = stringResource(R.string.subscribers_clicked_label),
                value = "10%"
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NewsletterSubscriptionCard(subscriber: Subscriber) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.subscribers_newsletter_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(
                label = stringResource(R.string.subscribers_date_label),
                value = subscriber.dateSubscribed.toString()
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                label = stringResource(R.string.subscribers_plan_label),
                value = "???",
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SubscriberDetailsCard(subscriber: Subscriber) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.subscribers_detail_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(
                label = stringResource(R.string.subscribers_email_label),
                value = subscriber.emailAddress,
                valueColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                label = stringResource(R.string.subscribers_country_label),
                value = "???"
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(
                label = stringResource(R.string.subscribers_site_label),
                value = "???",
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

@Composable
fun DeleteSubscriberButton() {
    Button(
        onClick = { /* Handle delete action */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.error
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.subscribers_delete_button),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.subscribers_delete_button),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SubscriberDetailScreenPreview() {
    val subscriber = Subscriber(
        userId = 0L,
        displayName = "User Name",
        emailAddress = "email@example.com",
        emailSubscriptionId = 0u,
        dateSubscribed = Date(),
        subscriptionStatus = "Subscribed",
        avatar = "",
    )

    AppThemeM3 {
        SubscriberDetailScreen(subscriber)
    }
}
