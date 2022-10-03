package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string

@Composable
fun TopLinearGradient(modifier: Modifier = Modifier) {
    Image(
            painter = painterResource(drawable.bg_jetpack_login_splash_top_gradient),
            contentDescription = stringResource(string.login_prologue_revamped_content_description_top_bg),
            contentScale = ContentScale.FillBounds,
            modifier = modifier
                    .fillMaxWidth()
                    .height(height = 292.dp)
    )
}
