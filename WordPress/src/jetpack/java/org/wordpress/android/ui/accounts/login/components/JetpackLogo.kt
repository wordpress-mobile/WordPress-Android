package org.wordpress.android.ui.accounts.login.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R

@Composable
fun JetpackLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_jetpack_logo_green_24dp),
        contentDescription = stringResource(
            R.string.login_prologue_revamped_content_description_jetpack_logo
        ),
        modifier = modifier
    )
}
