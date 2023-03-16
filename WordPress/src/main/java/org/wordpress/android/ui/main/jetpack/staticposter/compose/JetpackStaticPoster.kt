package org.wordpress.android.ui.main.jetpack.staticposter.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.staticposter.UiData
import org.wordpress.android.ui.main.jetpack.staticposter.UiState
import org.wordpress.android.ui.main.jetpack.staticposter.UiState.Content
import org.wordpress.android.ui.main.jetpack.staticposter.UiState.Loading
import org.wordpress.android.ui.main.jetpack.staticposter.toContentUiState

@Composable
fun JetpackStaticPoster(uiState: UiState, onBackClick: () -> Unit = {}) {
    Scaffold(
        topBar = topBar(onBackClick),
    ) {
        when (uiState) {
            is Content -> Content(uiState)
            is Loading -> Loading()
        }
    }
}

private fun topBar(onBackClick: () -> Unit) = @Composable {
    MainTopAppBar(
        title = null,
        navigationIcon = NavigationIcons.BackIcon,
        onNavigationIconClick = onBackClick,
    )
}

@Composable
private fun Content(uiState: Content) = with(uiState) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(text = uiStringText(featureName))
    }
}

@Composable
private fun Loading() {
    CircularProgressIndicator()
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
private fun PreviewJetpackStaticPoster() {
    AppTheme {
        Box {
            val uiState = UiData.STATS.toContentUiState()
            JetpackStaticPoster(uiState)
        }
    }
}
