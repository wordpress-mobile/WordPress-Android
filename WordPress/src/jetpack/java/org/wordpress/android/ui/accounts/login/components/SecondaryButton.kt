package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.string

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White,
                    contentColor = Color.Black,
            ),
            modifier = modifier
                    .padding(bottom = 60.dp)
                    .fillMaxWidth(),
    ) {
        Text(stringResource(string.enter_your_site_address))
    }
}
