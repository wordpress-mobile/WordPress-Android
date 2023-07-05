package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.theme.AppThemeEditor

@Composable
fun EditPostSettingsJetpackSocialEmpty(
    trainOfIconsModels: List<TrainOfIconsModel>
) {
    TrainOfIcons(
        iconModels = trainOfIconsModels,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialEmptyPreview() {
    AppThemeEditor {
        EditPostSettingsJetpackSocialEmpty(
            trainOfIconsModels = listOf(
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_three),
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_two),
                TrainOfIconsModel(R.drawable.login_prologue_third_asset_one),
                TrainOfIconsModel(R.mipmap.app_icon)
            ),
        )
    }
}
