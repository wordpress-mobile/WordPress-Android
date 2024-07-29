package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.domains.management.M3ThemeWithoutBackground

@Composable
fun FlatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        contentColor = AppColor.White,
    ),
    enabled: Boolean = true,
): Unit = Button(
    modifier = modifier,
    onClick = onClick,
    colors = colors,
    elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
    ),
    enabled = enabled,
) {
    Text(text)
}

@Composable
fun FlatOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    isPending: Boolean = false,
): Unit = OutlinedButton(
    modifier = modifier,
    onClick = onClick,
    colors = colors,
    elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
    ),
    enabled = enabled && !isPending,
) {
    if (isPending) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    } else {
        Text(text)
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFlatButton() {
    M3ThemeWithoutBackground {
        FlatButton(
            text = "Flat",
            onClick = {},
        )
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewOutlinedButton() {
    M3ThemeWithoutBackground {
        FlatOutlinedButton(
            text = "Outlined",
            onClick = {},
        )
    }
}
