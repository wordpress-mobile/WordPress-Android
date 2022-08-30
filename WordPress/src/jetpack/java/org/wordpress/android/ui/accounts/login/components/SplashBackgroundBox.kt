package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string

@Composable
fun SplashBackgroundBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Image(
                painter = painterResource(drawable.bg_jetpack_login_splash),
                contentDescription = stringResource(string.login_prologue_revamped_content_description_bg),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
        )
        LargeTexts(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
        )
        Image(
                painter = painterResource(drawable.bg_jetpack_login_splash_top_gradient),
                contentDescription = stringResource(string.login_prologue_revamped_content_description_top_bg),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 292.dp),
        )
        content()
    }
}
