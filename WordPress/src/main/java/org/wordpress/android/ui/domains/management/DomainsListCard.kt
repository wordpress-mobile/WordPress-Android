package org.wordpress.android.ui.domains.management

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.domains.management.composable.PendingGhostStrip
import org.wordpress.android.ui.compose.theme.AppThemeM3
import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.DomainListItemStatus
import uniffi.wp_api.DomainListItemStatusId
import uniffi.wp_api.DomainListItemStatusType
import uniffi.wp_api.DomainSubtype
import uniffi.wp_api.DomainSubtypeId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainListCard(
    uiState: DomainCardUiState,
    onDomainTapped: (domain: String, detailUrl: String) -> Unit =
        { _: String, _: String -> },
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = {
            if (uiState is DomainCardUiState.Loaded &&
                uiState.detailUrl != null
            ) {
                onDomainTapped(uiState.domain, uiState.detailUrl)
            }
        },
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                when (uiState) {
                    DomainCardUiState.Initial -> {
                        PendingGhostStrip(width = 100.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        PendingGhostStrip(100.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow(
                            uiState = StatusRowUiState.Initial,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is DomainCardUiState.Loaded -> {
                        Text(
                            text = uiState.domain,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.title,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow(
                            uiState = uiState.statusUiState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (uiState == DomainCardUiState.Initial) {
                Spacer(modifier = Modifier.width(24.dp))
            } else {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_chevron_right_white_24dp
                    ),
                    contentDescription = stringResource(
                        R.string.domain_management_open_domain_details
                    ),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DomainListCardPreview() {
    val expiry = "2024-08-15"

    AppThemeM3 {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            DomainListCard(uiState = DomainCardUiState.Initial)
            DomainListCard(
                uiState = DomainCardUiState.fromDomain(
                    domain = previewDomain(
                        status = DomainListItemStatusType.Success,
                        statusLabel = "Active",
                        expiry = expiry,
                    )
                )
            )
            DomainListCard(
                uiState = DomainCardUiState.fromDomain(
                    domain = previewDomain(
                        status = DomainListItemStatusType.Error,
                        statusLabel = "Expired",
                        expiry = expiry,
                    )
                )
            )
        }
    }
}

private fun previewDomain(
    status: DomainListItemStatusType,
    statusLabel: String,
    expiry: String?,
) = AllDomainItem(
    domain = "domain.cool",
    subtype = DomainSubtype(
        id = DomainSubtypeId.DomainRegistration,
        label = "Domain name registration"
    ),
    blogId = 0u,
    blogName = "A cool website",
    siteSlug = "domain.wordpress.com",
    autoRenewing = false,
    currentUserIsOwner = true,
    isDomainOnlySite = false,
    expiry = expiry,
    expired = false,
    primaryDomain = true,
    canSetAsPrimary = true,
    domainStatus = DomainListItemStatus(
        id = DomainListItemStatusId.Active,
        label = statusLabel,
        statusType = status,
        cta = null,
    ),
    subscriptionId = null,
    tags = emptyList(),
)
