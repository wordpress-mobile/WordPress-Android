package org.wordpress.android.ui.main.jetpack.migration.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
            onClick = onClick,
            elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
            ),
            modifier = modifier
                    .padding(top = 20.dp, bottom = 10.dp)
                    .padding(horizontal = dimensionResource(R.dimen.jp_migration_padding_horizontal))
                    .fillMaxWidth(),
    ) {
        Text(text = text)
    }
}
