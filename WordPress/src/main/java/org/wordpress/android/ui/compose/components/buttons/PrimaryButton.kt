package org.wordpress.android.ui.compose.components.buttons

import android.content.res.Configuration
<<<<<<< HEAD:WordPress/src/main/java/org/wordpress/android/ui/compose/components/PrimaryButton.kt
import androidx.compose.foundation.layout.PaddingValues
=======
import androidx.compose.foundation.layout.defaultMinSize
>>>>>>> origin/trunk:WordPress/src/main/java/org/wordpress/android/ui/compose/components/buttons/PrimaryButton.kt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
<<<<<<< HEAD:WordPress/src/main/java/org/wordpress/android/ui/compose/components/PrimaryButton.kt
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
=======
import androidx.compose.material.LocalContentColor
>>>>>>> origin/trunk:WordPress/src/main/java/org/wordpress/android/ui/compose/components/buttons/PrimaryButton.kt
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
<<<<<<< HEAD:WordPress/src/main/java/org/wordpress/android/ui/compose/components/PrimaryButton.kt
    colors: ButtonColors = ButtonDefaults.buttonColors(
        disabledBackgroundColor = colorResource(R.color.jetpack_green_70),
    ),
    padding: PaddingValues = PaddingValues(
        start = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        top = 20.dp,
        end = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal),
        bottom = 10.dp
    ),
    textStyle: TextStyle = LocalTextStyle.current,
=======
    useDefaultMargins: Boolean = true,
    buttonSize: ButtonSize = ButtonSize.NORMAL,
>>>>>>> origin/trunk:WordPress/src/main/java/org/wordpress/android/ui/compose/components/buttons/PrimaryButton.kt
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
<<<<<<< HEAD:WordPress/src/main/java/org/wordpress/android/ui/compose/components/PrimaryButton.kt
        colors = colors,
        modifier = modifier
            .padding(padding)
=======
        colors = ButtonDefaults.buttonColors(
            contentColor = AppColor.White,
            disabledBackgroundColor = colorResource(R.color.jetpack_green_70),
        ),
        modifier = computedModifier
>>>>>>> origin/trunk:WordPress/src/main/java/org/wordpress/android/ui/compose/components/buttons/PrimaryButton.kt
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
