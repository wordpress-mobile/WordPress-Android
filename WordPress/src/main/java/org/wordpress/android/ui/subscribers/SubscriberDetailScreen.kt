package org.wordpress.android.ui.subscribers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.models.wrappers.SimpleDateFormatWrapper
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage
import org.wordpress.android.ui.subscribers.SubscribersViewModel.Companion.displayNameOrEmail
import uniffi.wp_api.IndividualSubscriberStats
import uniffi.wp_api.Subscriber
import uniffi.wp_api.SubscriberCountry
import java.util.Date

@Composable
fun SubscriberDetailScreen(
    subscriber: Subscriber,
    onUrlClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    onPlanClick: (index: Int) -> Unit,
    onDeleteClick: (subscriber: Subscriber) -> Unit,
    modifier: Modifier = Modifier,
    subscriberStats: State<IndividualSubscriberStats?>? = null
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

        subscriberStats?.value?.let { stats ->
            EmailStatsCard(subscriberStats = stats)
        }

        Spacer(modifier = Modifier.height(16.dp))

        NewsletterSubscriptionCard(
            subscriber = subscriber,
            onPlanClick = onPlanClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        SubscriberDetailsCard(
            subscriber = subscriber,
            onUrlClick = onUrlClick,
            onEmailClick = onEmailClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        DeleteSubscriberButton(
            onClick = {
                onDeleteClick(subscriber)
            }
        )
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
    }
}

@Composable
fun EmailStatsCard(
    subscriberStats: IndividualSubscriberStats
) {
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
                value = subscriberStats.emailsSent.toString()
            )
            StatItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_email_open),
                label = stringResource(R.string.subscribers_opened_label),
                value = subscriberStats.uniqueOpens.toString()
            )
            StatItem(
                icon = ImageVector.vectorResource(id = R.drawable.ic_touch),
                label = stringResource(R.string.subscribers_clicked_label),
                value = subscriberStats.uniqueClicks.toString()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
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
fun NewsletterSubscriptionCard(
    subscriber: Subscriber,
    onPlanClick: (index: Int) -> Unit
) {
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
                value = SimpleDateFormatWrapper().getDateInstance().format(subscriber.dateSubscribed)
            )

            subscriber.subscriptionStatus?.let { status ->
                Spacer(modifier = Modifier.height(16.dp))
                DetailRow(
                    label = stringResource(R.string.subscribers_status_label),
                    value = status
                )
            }

            if (subscriber.plans?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))

                subscriber.plans!!.forEachIndexed { index, plan ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    DetailRow(
                        label = if (subscriber.plans!!.size == 1) {
                            stringResource(R.string.subscribers_plan_label)
                        } else {
                            stringResource(R.string.subscribers_plan_label) + " ${index + 1}"
                        },
                        value = plan.title,
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            onPlanClick(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriberDetailsCard(
    subscriber: Subscriber,
    onUrlClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
) {
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
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    onEmailClick(subscriber.emailAddress)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            subscriber.country?.name?.let { countryName ->
                DetailRow(
                    label = stringResource(R.string.subscribers_country_label),
                    value = countryName
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscriber.url?.let { url ->
                DetailRow(
                    label = stringResource(R.string.subscribers_site_label),
                    value = url,
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onUrlClick(url)
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
    ) {
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
private fun DeleteSubscriberButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = {
            onClick()
        },
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
        subscriptionId = 0u,
        displayName = "User Name",
        emailAddress = "email@example.com",
        isEmailSubscriber = true,
        url = "https://example.com",
        dateSubscribed = Date(),
        subscriptionStatus = "Subscribed",
        avatar = "",
        country = SubscriberCountry("US", "United States"),
        plans = emptyList(),
    )

    val subscriberStats = IndividualSubscriberStats(
        emailsSent = 10u,
        uniqueOpens = 5u,
        uniqueClicks = 3u,
        blogRegistrationDate = Date().toString(),
    )

    AppThemeM3 {
        SubscriberDetailScreen(
            subscriber = subscriber,
            subscriberStats = remember { mutableStateOf(subscriberStats) },
            onUrlClick = {},
            onEmailClick = {},
            onPlanClick = {},
            onDeleteClick = {}
        )
    }
}
