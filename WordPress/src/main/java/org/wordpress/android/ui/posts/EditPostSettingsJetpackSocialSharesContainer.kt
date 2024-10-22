package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM2Editor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.social.compose.DescriptionText
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.publicize.PublicizeServiceIcon

@Composable
fun EditPostSettingsJetpackSocialSharesContainer(
    postSocialSharingModel: PostSocialSharingModel,
    subscribeButtonLabel: String,
    onSubscribeClick: () -> Unit,
) {
    DescriptionText(
        text = postSocialSharingModel.description,
        isLowOnShares = postSocialSharingModel.isLowOnShares,
        baseTextStyle = MaterialTheme.typography.body2
            .copy(color = AppColor.Gray30),
        modifier = Modifier.padding(
            vertical = Margin.ExtraLarge.value,
            horizontal = Margin.ExtraLarge.value,
        )
    )
    PrimaryButton(
        text = subscribeButtonLabel,
        onClick = onSubscribeClick,
        fillMaxWidth = false,
        padding = PaddingValues(
            horizontal = Margin.ExtraLarge.value
        ),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialSharesContainerPreview() {
    AppThemeM2Editor {
        EditPostSettingsJetpackSocialSharesContainer(
            postSocialSharingModel = PostSocialSharingModel(
                title = "Sharing to 2 of 3 accounts",
                description = "27/30 social shares remaining",
                iconModels = PublicizeServiceIcon.values().map { TrainOfIconsModel(it.iconResId) },
                isLowOnShares = true,
            ),
            subscribeButtonLabel = "Subscribe",
            onSubscribeClick = {},
        )
    }
}
