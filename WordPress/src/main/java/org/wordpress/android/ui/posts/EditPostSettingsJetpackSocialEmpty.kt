package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun EditPostSettingsJetpackSocialEmpty(
    trainOfIconsModels: List<Any>
) {
    TrainOfIcons(
        iconModels = trainOfIconsModels,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialEmptyPreview() {
    AppTheme {
        EditPostSettingsJetpackSocialEmpty(
            trainOfIconsModels = listOf(
                R.drawable.login_prologue_second_asset_three,
                R.drawable.login_prologue_second_asset_two,
                R.drawable.login_prologue_third_asset_one,
                R.mipmap.app_icon
            ),
        )
    }
}
