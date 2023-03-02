package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useDefaultMargins: Boolean = true,
    buttonSize: ButtonSize = ButtonSize.NORMAL,
) {
    var computedModifier: Modifier = modifier

    if (useDefaultMargins) {
        computedModifier = computedModifier
            .padding(bottom = 10.dp)
            .padding(horizontal = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal))
    }

    buttonSize.height?.let {
        computedModifier = computedModifier.defaultMinSize(minHeight = it)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.primary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colors.primary,
        ),
        modifier = computedModifier.fillMaxWidth()
    ) {
        Text(text = text)
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonPreview() {
    AppTheme {
        SecondaryButton(text = "Continue", onClick = {})
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonNoDefaultMarginsPreview() {
    AppTheme {
        SecondaryButton(text = "Continue", onClick = {}, useDefaultMargins = false)
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonLargePreview() {
    AppTheme {
        SecondaryButton(text = "Continue", onClick = {}, buttonSize = ButtonSize.LARGE)
    }
}
