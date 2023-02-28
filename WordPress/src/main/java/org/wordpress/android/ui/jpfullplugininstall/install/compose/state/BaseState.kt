package org.wordpress.android.ui.jpfullplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.jpfullplugininstall.install.UiState
import org.wordpress.android.util.extensions.fixWidows

private val TitleTextStyle
    @ReadOnlyComposable
    @Composable
    get() = TextStyle(
        fontSize = 22.sp,
        letterSpacing = 0.35.sp,
        textAlign = TextAlign.Center,
    )

private val DescriptionTextStyle
    @ReadOnlyComposable
    @Composable
    get() = TextStyle(
        color = LocalContentColor.current.copy(alpha = 0.6f),
        fontSize = FontSize.Large.value,
        lineHeight = 21.sp,
        textAlign = TextAlign.Center,
    )

@Composable
fun BaseState(
    uiState: UiState,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    with(uiState) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(image),
                contentDescription = stringResource(imageContentDescription),
                modifier = Modifier
                    .size(dimensionResource(R.dimen.jetpack_new_remote_install_icon_size))
            )
            Text(
                text = stringResource(title),
                style = TitleTextStyle,
                modifier = Modifier.padding(top = Margin.ExtraMediumLarge.value),
            )
            Text(
                text = stringResource(description).fixWidows(),
                style = DescriptionTextStyle,
                modifier = Modifier.padding(top = Margin.Medium.value),
            )
            Spacer(modifier = Modifier.weight(1f))
            content()
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewInitialState() {
    AppTheme {
        val uiState = UiState.Installing
        BaseState(uiState, {})
    }
}
