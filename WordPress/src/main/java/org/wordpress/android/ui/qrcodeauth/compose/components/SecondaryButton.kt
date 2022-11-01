package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
            onClick = onClick,
            enabled = enabled,
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.background,
                    disabledBackgroundColor = MaterialTheme.colors.background,
            ),
            modifier = modifier.padding(
                    vertical = Margin.Small.value,
                    horizontal = Margin.ExtraExtraMediumLarge.value,
            )
    ) {
        Text(text = text)
    }
}
