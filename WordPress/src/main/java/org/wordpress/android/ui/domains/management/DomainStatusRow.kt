package org.wordpress.android.ui.domains.management

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.domains.management.composable.PendingGhostStrip
import org.wordpress.android.ui.compose.theme.AppThemeM3
import uniffi.wp_api.DomainListItemStatus
import uniffi.wp_api.DomainListItemStatusId
import uniffi.wp_api.DomainListItemStatusType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StatusRow(
    uiState: StatusRowUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (uiState) {
            StatusRowUiState.Initial -> {
                PendingGhostStrip(80.dp)
                Spacer(modifier = Modifier.weight(1f))
                PendingGhostStrip(width = 120.dp)
            }
            is StatusRowUiState.Loaded -> {
                BulletPoint(color = uiState.indicatorColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.statusText,
                    color = uiState.textColor,
                    style = if (uiState.isBold) {
                        LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        LocalTextStyle.current
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                uiState.expiry?.let { localDate ->
                    Text(
                        text = stringResource(
                            R.string.domain_management_expires,
                            localDate.mediumFormat
                        ),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
fun BulletPoint(
    color: Color = MaterialTheme.colorScheme.outline,
    alpha: Float = 1f,
) = Box(
    modifier = Modifier
        .graphicsLayer(alpha = alpha)
        .size(8.dp)
        .clip(CircleShape)
        .background(color),
)

private val LocalDate.mediumFormat
    get() = format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    )

private val previewStatusTypes = listOf(
    DomainListItemStatusType.Success to "Success",
    DomainListItemStatusType.Warning to "Warning",
    DomainListItemStatusType.Error to "Error",
    DomainListItemStatusType.Alert to "Alert",
    DomainListItemStatusType.Neutral to "Neutral",
    DomainListItemStatusType.Premium to "Premium",
)

class PreviewStatusProvider :
    PreviewParameterProvider<Pair<DomainListItemStatus, LocalDate?>?> {
    override val values
        get() = previewStatusTypes.asSequence()
            .map { (statusType, label) ->
                Pair(
                    previewStatus(label = label, statusType = statusType),
                    LocalDate.of(2024, 8, 15),
                )
            } +
                // A blank label falls back to the generic error string.
                sequenceOf(Pair(previewStatus(label = ""), null)) +
                sequenceOf(null)
}

private fun previewStatus(
    label: String,
    statusType: DomainListItemStatusType = DomainListItemStatusType.Success,
) = DomainListItemStatus(
    id = DomainListItemStatusId.Active,
    label = label,
    statusType = statusType,
    cta = null,
)

@Preview(showBackground = true, widthDp = 296)
@Preview(
    showBackground = true,
    widthDp = 296,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun DomainStatusRowPreview(
    @PreviewParameter(PreviewStatusProvider::class)
    status: Pair<DomainListItemStatus, LocalDate?>?,
) {
    AppThemeM3 {
        Column(Modifier.padding(8.dp)) {
            StatusRow(
                uiState = status?.let { (domainStatus, expiry) ->
                    StatusRowUiState.Loaded(
                        indicatorColor = domainStatus.indicatorColor,
                        statusText = domainStatus.statusText,
                        textColor = domainStatus.textColor,
                        isBold = domainStatus.isBold,
                        expiry = expiry,
                    )
                } ?: run {
                    StatusRowUiState.Initial
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
