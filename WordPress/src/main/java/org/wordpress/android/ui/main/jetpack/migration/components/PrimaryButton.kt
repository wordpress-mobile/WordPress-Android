package org.wordpress.android.ui.main.jetpack.migration.components

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
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

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
