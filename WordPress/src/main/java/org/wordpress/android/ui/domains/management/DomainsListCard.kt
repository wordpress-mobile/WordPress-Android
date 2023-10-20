package org.wordpress.android.ui.domains.management

import android.content.res.Configuration
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainStatus
import org.wordpress.android.fluxc.network.rest.wpcom.site.StatusType
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@Composable
fun DomainListCard(uiState: DomainCardUiState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        uiState.domain?.let { domain ->
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.title?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
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
                    painter = painterResource(id = R.drawable.ic_chevron_right_white_24dp),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun PendingGhostStrip(width: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pending ghost strip transition")
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.ghost.copy(alpha = 0.06f),
        targetValue = MaterialTheme.colorScheme.ghost.copy(alpha = 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Pending ghost strip color"
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(lineHeightDp)
            .background(color)
    )
}

private val lineHeightDp
    @Composable
    get() = with(LocalDensity.current) {
        LocalTextStyle.current.lineHeight.toDp()
    }

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DomainListCardPreview() {
    val expiry = LocalDate.of(2024,8,15).asLegacyDate()

    M3Theme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            DomainListCard(uiState = DomainCardUiState.Initial)
            DomainListCard(
                uiState = DomainCardUiState.fromDomain(
                    domain = AllDomainsDomain(
                        domain = "domain.cool",
                        blogName = "A cool website",
                        domainStatus = DomainStatus(StatusType.SUCCESS.titleName, StatusType.SUCCESS),
                        expiry = expiry,
                    )
                )
            )
            DomainListCard(
                uiState = DomainCardUiState.fromDomain(
                    domain = AllDomainsDomain(
                        domain = "domain.cool",
                        blogName = "A cool website",
                        domainStatus = DomainStatus(StatusType.ERROR.titleName, StatusType.ERROR),
                        expiry = expiry,
                    )
                )
            )
            DomainListCard(
                uiState = DomainCardUiState.fromDomain(
                    domain = AllDomainsDomain(
                        domain = "domain.cool",
                        blogName = "A cool website",
                        domainStatus = null,
                        expiry = expiry,
                    )
                )
            )
        }
    }
}

private fun LocalDate.asLegacyDate(zoneId: ZoneId = ZoneId.systemDefault()) =
    Date.from(atStartOfDay(zoneId).toInstant())
