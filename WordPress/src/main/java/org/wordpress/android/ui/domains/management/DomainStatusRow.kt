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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StatusRow(
    status: DomainStatus?,
    expiry: LocalDate,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        status?.also {
            BulletPoint(color = status.statusUiState.indicatorColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = status.statusUiState.text),
                color = status.statusUiState.textColor,
                style = if (status.statusUiState.isBold) {
                    LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                } else {
                    LocalTextStyle.current
                },
            )
        } ?: run {
            PendingGhostStrip(80.dp)
        }
        Text(
            text = "Expires ${expiry.mediumFormat}",
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
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
    get() = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))


class PreviewStatusProvider: PreviewParameterProvider<DomainStatus> {
    override val values
        get() = DomainStatus.values().asSequence()
}

@Preview(showBackground = true, widthDp = 296)
@Preview(showBackground = true, widthDp = 296, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DomainStatusRowPreview(
    @PreviewParameter(PreviewStatusProvider::class) status: DomainStatus,
) {
    M3Theme {
        Column (Modifier.padding(8.dp)) {
            StatusRow(
                status = status,
                expiry = LocalDate.of(2024,8,15),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 296)
@Preview(showBackground = true, widthDp = 296, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DomainStatusGhostBulletsPreview() {
    M3Theme {
        Column (Modifier.padding(8.dp)) {
            StatusRow(
                status = null,
                expiry = LocalDate.of(2024,8,15),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
