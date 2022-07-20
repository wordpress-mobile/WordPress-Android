package org.wordpress.android.ui.qrcodeauth.compose.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.IndeterminateCircularProgress
import org.wordpress.android.ui.compose.components.ResourceImage
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedSecondaryActionButton
import org.wordpress.android.ui.qrcodeauth.compose.components.PrimaryButton
import org.wordpress.android.ui.qrcodeauth.compose.components.SecondaryButton
import org.wordpress.android.ui.qrcodeauth.compose.components.Subtitle
import org.wordpress.android.ui.qrcodeauth.compose.components.Title

@Suppress("LongParameterList", "LongMethod")
@Composable
fun ContentState(uiState: QRCodeAuthUiState.Content) = with(uiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(alpha),
    ) {
        image?.let { imageRes ->
            ResourceImage(
                modifier = Modifier
                    .padding(
                        top = Margin.ExtraLarge.value,
                        bottom = Margin.ExtraLarge.value
                    )
                    .wrapContentHeight()
                    .wrapContentWidth(),
                imageRes = imageRes,
                contentDescription = stringResource(R.string.qrcode_auth_flow_content_description)
            )
        }
        title?.let {
            Title(text = uiStringText(it))
        }
        subtitle?.let {
            Subtitle(text = uiStringText(it))
        }
        primaryActionButton?.let { actionButton ->
            if (actionButton.isVisible) {
                actionButton.label?.let { label ->
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = Margin.Small.value,
                                horizontal = Margin.ExtraExtraMediumLarge.value,
                            ),
                        text = uiStringText(label),
                        enabled = actionButton.isEnabled,
                        onClick = { actionButton.clickAction?.invoke() }
                    )
                }
            }
        }
        secondaryActionButton?.let { actionButton ->
            if (actionButton.isVisible) {
                actionButton.label?.let { label ->
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = Margin.Small.value,
                                horizontal = Margin.ExtraExtraMediumLarge.value,
                            ),
                        text = uiStringText(label),
                        enabled = actionButton.isEnabled,
                        onClick = { actionButton.clickAction?.invoke() }
                    )
                }
            }
        }
        if (isProgressShowing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                IndeterminateCircularProgress()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentStatePreview() {
    AppTheme {
        val state = QRCodeAuthUiState.Content.Validated(
            browser = "{browser}",
            location = "{location}",
            primaryActionButton = ValidatedPrimaryActionButton {},
            secondaryActionButton = ValidatedSecondaryActionButton {},
        )
        ContentState(state)
    }
}
