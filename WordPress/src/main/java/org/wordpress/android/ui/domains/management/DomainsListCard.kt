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
import org.wordpress.android.ui.domains.management.DomainStatus.Active
import org.wordpress.android.ui.domains.management.DomainStatus.Expired
import java.time.LocalDate

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
                Text(
                    text = uiState.domain,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                uiState.title?.also {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } ?: run {
                    PendingGhostStrip(100.dp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    status = uiState.status,
                    expiry = uiState.expiry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_white_24dp),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun PendingGhostStrip(width: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pending ghost strip transition")
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.outline,
        targetValue = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
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
    M3Theme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            DomainListCard(uiState = DomainCardUiState(
                domain = "domain.cool",
                title = "A cool website",
                status = Active,
                expiry = LocalDate.of(2024,8,15),
            ))
            DomainListCard(uiState = DomainCardUiState(
                domain = "domain.cool",
                title = "A cool website",
                status = Expired,
                expiry = LocalDate.of(2024,8,15),
            ))
            DomainListCard(uiState = DomainCardUiState(
                domain = "domain.cool",
                title = null,
                status = null,
                expiry = LocalDate.of(2024,8,15),
            ))
            DomainListCard(uiState = DomainCardUiState(
                domain = "domain.cool",
                title = "A cool website",
                status = null,
                expiry = LocalDate.of(2024,8,15),
            ))
        }
    }
}
