package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingItem
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.publicize.PublicizeServiceIcon

@Composable
fun EditPostSettingsJetpackSocialSharesContainer(
    postSocialSharingModel: PostSocialSharingModel,
    subscribeButtonLabel: String,
    onSubscribeClick: () -> Unit,
) {
    PostSocialSharingItem(
        model = postSocialSharingModel,
        modifier = Modifier.padding(
            vertical = Margin.ExtraLarge.value,
            horizontal = Margin.ExtraLarge.value,
        ),
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
    AppThemeEditor {
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
