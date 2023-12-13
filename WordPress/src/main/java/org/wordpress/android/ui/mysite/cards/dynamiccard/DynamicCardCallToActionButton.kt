package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.styles.DashboardCardTypography

@Composable
fun DynamicCardCallToActionButton(
    text: String,
    onClicked: () -> Unit
) {
    TextButton(
        modifier = Modifier.padding(start = 8.dp),
        onClick = onClicked,
    ) {
        Text(
            text = text,
            style = DashboardCardTypography.footerCTA,
        )
    }
}
