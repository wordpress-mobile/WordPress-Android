package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInProgress: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        contentColor = AppColor.White,
        disabledContentColor = AppColor.White.copy(alpha = ContentAlpha.disabled),
        disabledBackgroundColor = colorResource(R.color.jetpack_green_70),
    ),
    padding: PaddingValues = PaddingValues(
        start = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        top = 20.dp,
        end = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        bottom = 10.dp
    ),
    textStyle: TextStyle = LocalTextStyle.current,
    buttonSize: ButtonSize = ButtonSize.NORMAL,
) {
    Button(
        onClick = onClick,
        enabled = !isInProgress,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = colors,
        modifier = modifier
            .padding(padding)
            .defaultMinSize(minHeight = buttonSize.height)
            .fillMaxWidth(),
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(text, style = textStyle)
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonPreview() {
    AppTheme {
        PrimaryButton(text = "Continue", onClick = {})
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonInProgressPreview() {
    AppTheme {
        PrimaryButton(text = "Continue", onClick = {}, isInProgress = true)
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonLargePreview() {
    AppTheme {
        PrimaryButton(text = "Continue", onClick = {}, buttonSize = ButtonSize.LARGE)
    }
}
