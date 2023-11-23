package org.wordpress.android.ui.compose.components.menu

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun DropdownMenuButton() {
}


@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonPreview() {
    AppTheme {
        PrimaryButton(text = "Continue", onClick = {})
    }
}
