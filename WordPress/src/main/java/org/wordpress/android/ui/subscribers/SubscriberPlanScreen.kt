package org.wordpress.android.ui.subscribers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import uniffi.wp_api.SubscriptionPlan
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@Composable
fun SubscriberPlanScreen(
    plan: SubscriptionPlan,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PlanHeader(plan)

        Spacer(modifier = Modifier.height(24.dp))

        PlanDetailsCard(plan)

        Spacer(modifier = Modifier.height(16.dp))

        BillingInformationCard(plan)

        Spacer(modifier = Modifier.height(16.dp))

        RenewalInformationCard(plan)

        if (plan.isGift) {
            Spacer(modifier = Modifier.height(16.dp))
            GiftPlanCard()
        }
    }
}

@Composable
private fun PlanHeader(plan: SubscriptionPlan) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (plan.isGift) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_gift),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = plan.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = if (plan.isActive()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = plan.status.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (plan.isActive()) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PlanDetailsCard(plan: SubscriptionPlan) {
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
                text = stringResource(id = R.string.subscribers_plan_details_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlanDetailRow(
                icon = Icons.Default.DateRange,
                label = stringResource(id = R.string.subscribers_start_date),
                value = SimpleDateFormatWrapper().getDateInstance().format(plan.startDate)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlanDetailRow(
                icon = Icons.Default.DateRange,
                label = stringResource(id = R.string.subscribers_end_date),
                value = SimpleDateFormatWrapper().getDateInstance().format(plan.endDate)
            )
        }
    }
}

@Composable
private fun BillingInformationCard(plan: SubscriptionPlan) {
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
                text = stringResource(id = R.string.subscribers_billing_info),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlanDetailRow(
                icon = Icons.Default.Info,
                label = stringResource(id = R.string.subscribers_price),
                value = formatCurrency(plan.renewalPrice, plan.currency)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlanDetailRow(
                icon = Icons.Default.Refresh,
                label = stringResource(id = R.string.subscribers_billing_interval),
                value = plan.renewInterval
            )

            plan.inactiveRenewInterval?.let { inactiveInterval ->
                Spacer(modifier = Modifier.height(12.dp))
                PlanDetailRow(
                    icon = Icons.Default.Refresh,
                    label = stringResource(id = R.string.subscribers_inactive_interval),
                    value = inactiveInterval
                )
            }
        }
    }
}

@Composable
private fun RenewalInformationCard(plan: SubscriptionPlan) {
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
                text = stringResource(id = R.string.subscribers_renewal_info),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (plan.isActive()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.subscribers_renewal_interval),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = plan.renewInterval,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GiftPlanCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_gift),
                contentDescription = stringResource(id = R.string.subscribers_gift_content_description),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.subscribers_gift_plan_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlanDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Suppress("SwallowedException")
private fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val currency = Currency.getInstance(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = currency
        formatter.format(amount)
    } catch (e: IllegalArgumentException) {
        "$currencyCode $amount"
    }
}

private fun SubscriptionPlan.isActive() = status == "active"

@Preview(showBackground = true)
@Composable
fun SubscriberPlansScreenPreview() {
    val plan = SubscriptionPlan(
        isGift = true,
        giftId = 1u,
        paidSubscriptionId = "id",
        status = "Subscribed",
        title = "Title",
        currency = "USD",
        renewInterval = "Monthly",
        inactiveRenewInterval = null,
        renewalPrice = 12.0,
        startDate = Date(),
        endDate = Date(),
    )

    AppThemeM3 {
        SubscriberPlanScreen(
            plan = plan,
        )
    }
}
