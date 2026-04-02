package org.wordpress.android.ui.compose.styles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DashboardCardTypography {
    val title: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

    val smallTitle: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

    val subTitle: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

    val detailText: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
        )

    val largeText: TextStyle
        @Composable
        get() = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        )

    val footerCTA: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

    val standaloneText: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
}

@Preview
@Composable
fun DashboardCardTypographyPreview() {
    val padding = Modifier.padding(8.dp)

    Column {
        Text(
            text = "Title",
            style = DashboardCardTypography.title,
            modifier = padding
        )
        Text(
            text = "subTitle",
            style = DashboardCardTypography.subTitle,
            modifier = padding
        )
        Text(
            text = "detailText",
            style = DashboardCardTypography.detailText,
            modifier = padding
        )
        Text(
            text = "largeText",
            style = DashboardCardTypography.largeText,
            modifier = padding
        )
        Text(
            text = "footerCTA",
            style = DashboardCardTypography.footerCTA,
            modifier = padding
        )
        Text(
            text = "standaloneText",
            style = DashboardCardTypography.standaloneText,
            modifier = padding
        )
    }
}
