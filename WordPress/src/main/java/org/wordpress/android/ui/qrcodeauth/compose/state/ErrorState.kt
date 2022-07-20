package org.wordpress.android.ui.qrcodeauth.compose.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ResourceImage
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.qrcodeauth.compose.components.PrimaryButton
import org.wordpress.android.ui.qrcodeauth.compose.components.SecondaryButton
import org.wordpress.android.ui.qrcodeauth.compose.components.Subtitle
import org.wordpress.android.ui.qrcodeauth.compose.components.Title

@Composable
fun ErrorState(
    @DrawableRes imageRes: Int,
    @StringRes contentDescriptionRes: Int,
    titleText: String,
    subtitleText: String,
    primaryButtonText: String,
    primaryButtonClick: () -> Unit,
    secondaryButtonText: String,
    secondaryButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ResourceImage(
            modifier = Modifier
                .padding(
                    top = Margin.ExtraLarge.value,
                    bottom = Margin.ExtraLarge.value
                )
                .wrapContentHeight()
                .wrapContentWidth(),
            imageRes = imageRes,
            contentDescription = stringResource(contentDescriptionRes),
        )
        Title(text = titleText)
        Subtitle(text = subtitleText)
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = Margin.Small.value,
                    bottom = Margin.Small.value,
                    start = Margin.ExtraExtraMediumLarge.value,
                    end = Margin.ExtraExtraMediumLarge.value
                ),
            text = primaryButtonText,
            onClick = { primaryButtonClick() }
        )
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = Margin.Small.value,
                    bottom = Margin.Small.value,
                    start = Margin.ExtraExtraMediumLarge.value,
                    end = Margin.ExtraExtraMediumLarge.value
                ),
            text = secondaryButtonText,
            onClick = { secondaryButtonClick() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    AppTheme {
        ErrorState(
            imageRes = R.drawable.img_illustration_empty_results_216dp,
            contentDescriptionRes = R.string.qrcode_auth_flow_error_content_description,
            titleText = stringResource(R.string.qrcode_auth_flow_error_no_connection_title),
            subtitleText = stringResource(R.string.qrcode_auth_flow_error_no_connection_subtitle),
            primaryButtonText = stringResource(R.string.qrcode_auth_flow_scan_again),
            primaryButtonClick = {},
            secondaryButtonText = stringResource(R.string.cancel),
            secondaryButtonClick = {}
        )
    }
}
