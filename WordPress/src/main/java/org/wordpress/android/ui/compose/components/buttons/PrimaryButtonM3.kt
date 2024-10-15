package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.modifiers.conditionalThen
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.M3Theme

@Composable
fun PrimaryButtonM3(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInProgress: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        contentColor = AppColor.White,
        disabledContentColor = AppColor.White.copy(alpha = 0.38f),
        disabledContainerColor = colorResource(R.color.jetpack_green_70),
    ),
    padding: PaddingValues = PaddingValues(
        start = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        top = 20.dp,
        end = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        bottom = 10.dp
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    textStyle: TextStyle = LocalTextStyle.current,
    buttonSize: ButtonSize = ButtonSize.NORMAL,
    fillMaxWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = !isInProgress,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = colors,
        modifier = modifier
            .padding(paddingValues = padding)
            .defaultMinSize(minHeight = buttonSize.height)
            .conditionalThen(
                predicate = fillMaxWidth,
                other = Modifier.fillMaxWidth()
            ),
        contentPadding = contentPadding,
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
    M3Theme {
        PrimaryButtonM3(text = "Continue", onClick = {})
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonInProgressPreview() {
    M3Theme {
        PrimaryButtonM3(text = "Continue", onClick = {}, isInProgress = true)
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonLargePreview() {
    M3Theme {
        PrimaryButtonM3(text = "Continue", onClick = {}, buttonSize = ButtonSize.LARGE)
    }
}
