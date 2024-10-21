package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = Color.Transparent,
        contentColor = MaterialTheme.colors.primary,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colors.primary,
    ),
    padding: PaddingValues = PaddingValues(
        start = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        end = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        bottom = 10.dp,
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    textStyle: TextStyle = LocalTextStyle.current,
    buttonSize: ButtonSize = ButtonSize.NORMAL,
    fillMaxWidth: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick,
        enabled = enabled,
        colors = colors,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        modifier = modifier
            .padding(padding)
            .defaultMinSize(minHeight = buttonSize.height)
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier),
        contentPadding = contentPadding,
    ) {
        Text(text, style = textStyle)
        trailingContent?.invoke()
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonPreview() {
    AppThemeM2 {
        SecondaryButton(text = "Continue", onClick = {})
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonLargePreview() {
    AppThemeM2 {
        SecondaryButton(text = "Continue", onClick = {}, buttonSize = ButtonSize.LARGE)
    }
}
