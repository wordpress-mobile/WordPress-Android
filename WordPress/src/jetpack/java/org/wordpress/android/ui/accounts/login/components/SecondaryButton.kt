package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
            onClick = onClick,
            elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                    contentColor = colorResource(R.color.text_color_jetpack_login_splash_secondary_button),
                    backgroundColor = Color.Transparent,
            ),
            modifier = modifier
                    .padding(horizontal = dimensionResource(R.dimen.login_prologue_revamped_buttons_padding))
                    .padding(bottom = 60.dp)
                    .fillMaxWidth(),
    ) {
        Text(
                text = stringResource(R.string.enter_your_site_address),
                style = TextStyle(
                        fontWeight = FontWeight.Medium,
                ),
        )
    }
}
