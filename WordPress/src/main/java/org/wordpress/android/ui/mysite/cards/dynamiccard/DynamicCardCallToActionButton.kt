package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.domains.management.success

@Composable
fun DynamicCardCallToActionButton(
    text: String,
    onClicked: () -> Unit
) {
    TextButton(
        modifier = Modifier.padding(start = 4.dp),
        onClick = onClicked,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.success
            ),
        )
    }
}
