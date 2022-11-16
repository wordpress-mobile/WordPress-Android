package org.wordpress.android.ui.main.jetpack.migration.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    isInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
            onClick = onClick,
            enabled = !isInProgress,
            elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                    disabledBackgroundColor = colorResource(R.color.jetpack_green_70),
            ),
            modifier = modifier
                    .padding(top = 20.dp, bottom = 10.dp)
                    .padding(horizontal = dimensionResource(R.dimen.jp_migration_buttons_padding_horizontal))
                    .fillMaxWidth(),
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                    color = MaterialTheme.colors.onPrimary,
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
