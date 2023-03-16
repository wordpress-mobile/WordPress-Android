package org.wordpress.android.ui.main.jetpack.staticposter.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.staticposter.UiData
import org.wordpress.android.ui.main.jetpack.staticposter.UiState
import org.wordpress.android.ui.main.jetpack.staticposter.UiState.Content
import org.wordpress.android.ui.main.jetpack.staticposter.UiState.Loading
import org.wordpress.android.ui.main.jetpack.staticposter.toContentUiState

@Suppress("SpellCheckingInspection")
private val bodyText = buildString {
    append("Esse sit dolor cillum veniam. Proident exercitation nisi in")
    append("elit ea magna esse quis laboris nulla veniam ad. Amet non ullamco do eu.")
    append("Voluptate reprehenderit nulla culpa veniam mollit dolore eu irure cillum et irure dolore.")
    append("Adipisicing mollit ut ad Lorem aliqua.")
    append("Excepteur reprehenderit dolor amet aute aute officia. Irure anim amet.")
    append("Nisi ullamco ipsum pariatur aliquip laboris cupidatat commodo excepteur ea do anim cupidatat.")
}

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
            .padding(horizontal = 30.dp)
            .fillMaxSize()
    ) {
        Text(
            text = uiStringText(featureName),
            style = MaterialTheme.typography.h1,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = bodyText,
            style = MaterialTheme.typography.body1,
        )
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
