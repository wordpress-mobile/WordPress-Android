package org.wordpress.android.ui.compose.styles

import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DashboardCardTypography {
    val title: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface.copy(alpha = ContentAlpha.high)
        )

    val smallTitle: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = colors.onSurface.copy(alpha = ContentAlpha.high)
        )

    val subTitle: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Normal,
            color = colors.onSurface.copy(alpha = ContentAlpha.high)
        )

    val detailText: TextStyle
        @Composable
        get() = MaterialTheme.typography.bodyMedium.copy(
            color = colors.onSurface.copy(alpha = ContentAlpha.medium)
        )

    val largeText: TextStyle
        @Composable
        get() = MaterialTheme.typography.headlineMedium.copy(
            color = colors.onSurface.copy(alpha = ContentAlpha.high)
        )

    val footerCTA: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            color = colors.primary
        )

    val standaloneText: TextStyle
        @Composable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            color = colors.onSurface.copy(alpha = ContentAlpha.high)
        )
}
