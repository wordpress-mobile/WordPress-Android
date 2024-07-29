package org.wordpress.android.ui.domains.management.composable

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.themes.M3Theme

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isInProgress: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = AppColor.White,
        ),
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonPreview() {
    M3Theme {
        PrimaryButton(text = "Continue", onClick = {})
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonInProgressPreview() {
    M3Theme {
        PrimaryButton(text = "Continue", onClick = {}, isInProgress = true)
    }
}
