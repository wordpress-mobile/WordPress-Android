package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R.string

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White,
            ),
            modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(string.continue_with_wpcom_no_signup))
    }
}
