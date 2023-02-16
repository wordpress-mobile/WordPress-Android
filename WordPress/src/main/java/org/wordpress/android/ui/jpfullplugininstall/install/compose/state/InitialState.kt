package org.wordpress.android.ui.jpfullplugininstall.install.compose.state

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.jpfullplugininstall.install.JetpackFullPluginInstallViewModel.UiState
import org.wordpress.android.ui.qrcodeauth.compose.components.PrimaryButton

@Composable
fun InitialState(
    content: UiState.Initial,
    onContinueClick: () -> Unit,
    onDismissScreenClick: () -> Unit,
) = Box(
    Modifier
        .fillMaxWidth()
        .fillMaxHeight()
) {
    with(content) {
        MainTopAppBar(
            title = stringResource(toolbarTitle),
            navigationIcon = NavigationIcons.BackIcon,
            onNavigationIconClick = onDismissScreenClick
        )
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = stringResource(imageContentDescription),
                modifier = Modifier
                    .height(dimensionResource(R.dimen.jetpack_remote_install_icon_size))
                    .width(dimensionResource(R.dimen.jetpack_remote_install_icon_size))
            )
            Text(
                text = stringResource(title),
                textAlign = TextAlign.Center,
                fontSize = FontSize.ExtraLarge.value,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = Margin.ExtraLarge.value),
            )
            Text(
                text = stringResource(description),
                textAlign = TextAlign.Center,
                fontSize = FontSize.Large.value,
                modifier = Modifier.padding(top = Margin.Medium.value),
            )
            PrimaryButton(
                text = stringResource(buttonText),
                onClick = onContinueClick,
                modifier = Modifier.padding(top = Margin.ExtraLarge.value),
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewInitialState() {
    AppTheme {
        val uiState = UiState.Initial
        InitialState(uiState, {}, {})
    }
}
