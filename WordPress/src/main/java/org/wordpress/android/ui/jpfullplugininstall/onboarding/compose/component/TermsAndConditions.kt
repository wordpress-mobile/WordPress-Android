package org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun TermsAndConditions(
    modifier: Modifier = Modifier,
    onTermsAndConditionsClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 30.dp),
            text = stringResource(R.string.jetpack_full_plugin_install_onboarding_terms_and_conditions_text),
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            style = TextStyle(
                letterSpacing = (-0.02).sp,
            ),
            color = AppColor.Gray40,
        )
        Text(
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .clickable { onTermsAndConditionsClick() },
            text = stringResource(R.string.jetpack_full_plugin_install_onboarding_terms_and_conditions_button),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(
                textDecoration = TextDecoration.Underline,
                letterSpacing = (-0.02).sp,
            ),
            color = AppColor.Gray40,
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewTermsAndConditions() {
    AppTheme {
        TermsAndConditions {}
    }
}
