package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.bg_jetpack_login_splash_primary_button),
            contentColor = colorResource(R.color.text_color_jetpack_login_splash_primary_button),
        ),
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.login_prologue_revamped_buttons_padding))
            .padding(top = Margin.ExtraLarge.value)
            .widthIn(0.dp, dimensionResource(R.dimen.login_prologue_revamped_buttons_max_width))
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.continue_with_wpcom_no_signup),
            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
