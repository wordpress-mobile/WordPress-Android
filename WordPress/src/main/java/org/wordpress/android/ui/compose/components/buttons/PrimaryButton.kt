package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
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
    useDefaultMargins: Boolean = true,
    buttonSize: ButtonSize = ButtonSize.NORMAL,
) {
    var computedModifier: Modifier = modifier

    if (useDefaultMargins) {
        computedModifier = computedModifier
            .padding(top = 20.dp, bottom = 10.dp)
            .padding(horizontal = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal))
    }

    computedModifier = computedModifier.defaultMinSize(minHeight = buttonSize.height)

    Button(
        onClick = onClick,
        enabled = !isInProgress,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            contentColor = AppColor.White,
            disabledBackgroundColor = colorResource(R.color.jetpack_green_70),
        ),
        modifier = computedModifier
            .fillMaxWidth(),
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(text = text)
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
private fun PrimaryButtonNoDefaultMarginsPreview() {
    AppTheme {
        PrimaryButton(text = "Continue", onClick = {}, useDefaultMargins = false)
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
