package org.wordpress.android.ui.jetpackplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ContentAlphaProvider
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.jetpackplugininstall.install.UiState
import org.wordpress.android.util.extensions.fixWidows

private val TitleTextStyle
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.typography.h6.copy(
        textAlign = TextAlign.Center,
    )

private val DescriptionTextStyle
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.typography.body1.copy(
        textAlign = TextAlign.Center,
    )

@Composable
fun BaseState(
    uiState: UiState,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    with(uiState) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val height = this.maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(height * 0.25f))
                Image(
                    painter = painterResource(image),
                    contentDescription = stringResource(imageContentDescription),
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.jetpack_new_remote_install_icon_size))
                )
                ContentAlphaProvider(ContentAlpha.high) {
                    Text(
                        text = stringResource(title),
                        style = TitleTextStyle,
                        modifier = Modifier.padding(top = Margin.ExtraMediumLarge.value),
                    )
                }
                ContentAlphaProvider(ContentAlpha.medium) {
                    Text(
                        text = stringResource(description).fixWidows(),
                        style = DescriptionTextStyle,
                        modifier = Modifier.padding(top = Margin.Medium.value),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                content()
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewInitialState() {
    AppThemeM2 {
        val uiState = UiState.Installing
        BaseState(uiState, {})
    }
}
