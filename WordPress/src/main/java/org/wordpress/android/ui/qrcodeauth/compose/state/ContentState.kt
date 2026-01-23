package org.wordpress.android.ui.qrcodeauth.compose.state

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.components.buttons.SecondaryButtonM3
import org.wordpress.android.ui.compose.components.text.SubtitleM3
import org.wordpress.android.ui.compose.components.text.TitleM3
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ValidatedSecondaryActionButton

@Composable
fun ContentState(uiState: QRCodeAuthUiState.Content): Unit = with(uiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(alpha)
    ) {
        title?.let {
            TitleM3(
                text = uiStringText(it),
                textAlign = TextAlign.Center
            )
        }
        subtitle?.let {
            SubtitleM3(
                text = uiStringText(it),
                textAlign = TextAlign.Center
            )
        }
        primaryActionButton?.let { actionButton ->
            if (actionButton.isVisible) {
                actionButton.label?.let { label ->
                    PrimaryButtonM3(
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
                    SecondaryButtonM3(
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
    AppThemeM3 {
        val state = QRCodeAuthUiState.Content.Validated(
            browser = "{browser}",
            location = "{location}",
            primaryActionButton = ValidatedPrimaryActionButton {},
            secondaryActionButton = ValidatedSecondaryActionButton {},
        )
        ContentState(state)
    }
}
