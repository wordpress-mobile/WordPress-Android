package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun SecondaryButtonM3(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.primary,
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
        elevation = ButtonDefaults.buttonElevation(
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
    AppThemeM3 {
        SecondaryButtonM3(text = "Continue", onClick = {})
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonLargePreview() {
    AppThemeM3 {
        SecondaryButtonM3(text = "Continue", onClick = {}, buttonSize = ButtonSize.LARGE)
    }
}
