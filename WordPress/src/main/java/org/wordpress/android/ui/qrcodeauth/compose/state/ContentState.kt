package org.wordpress.android.ui.qrcodeauth.compose.state

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
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

@Composable
fun ContentState(uiState: QRCodeAuthUiState.Content) = with(uiState) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .alpha(alpha)
    ) {
        image?.let { imageRes ->
            Image(
                    painter = painterResource(imageRes),
                    contentDescription = stringResource(R.string.qrcode_auth_flow_content_description),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier
                            .padding(vertical = Margin.ExtraLarge.value)
                            .wrapContentSize()
            )
        }
        title?.let {
            Title(text = uiStringText(it))
        }
        subtitle?.let {
            Subtitle(text = uiStringText(it), color = MaterialTheme.colors.onBackground)
        }
        primaryActionButton?.let { actionButton ->
            if (actionButton.isVisible) {
                actionButton.label?.let { label ->
                    PrimaryButton(
                            text = uiStringText(label),
                            onClick = { actionButton.clickAction?.invoke() },
                            enabled = actionButton.isEnabled,
                            modifier = Modifier
                                    .fillMaxWidth()
                    )
                }
            }
        }
        secondaryActionButton?.let { actionButton ->
            if (actionButton.isVisible) {
                actionButton.label?.let { label ->
                    SecondaryButton(
                            text = uiStringText(label),
                            onClick = { actionButton.clickAction?.invoke() },
                            enabled = actionButton.isEnabled,
                            modifier = Modifier
                                    .fillMaxWidth()
                    )
                }
            }
        }
        if (isProgressShowing) {
            CircularProgressIndicator()
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
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
