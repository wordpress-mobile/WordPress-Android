package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeConnectionList
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialMessageItem
import org.wordpress.android.ui.posts.social.compose.PostSocialSharesText

@Composable
fun EditPostSettingsJetpackSocialContainerView(
    trainOfIconsModels: List<TrainOfIconsModel>,
    postSocialConnectionList: List<PostSocialConnection>,
    shareMessage: String,
    remainingSharesMessage: String,
    subscribeButtonLabel: String,
    onSubscribeClick: () -> Unit,
) {
    Column {
        if (postSocialConnectionList.isNotEmpty()) {
            EditPostSettingsJetpackSocialConnectionsList(
                postSocialConnectionList = postSocialConnectionList,
            )
            PostSocialMessageItem(
                message = shareMessage,
                modifier = Modifier.padding(
                    vertical = Margin.MediumLarge.value,
                ),
            )
            Divider()
            PostSocialSharesText(
                message = remainingSharesMessage,
            )
            PrimaryButton(
                text = subscribeButtonLabel,
                onClick = onSubscribeClick,
                shouldFillMaxWidth = false,
            )
        } else {
            EditPostSettingsJetpackSocialEmpty(
                trainOfIconsModels = trainOfIconsModels,
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEditPostSettingsJetpackSocialContainerConnectionsListView() {
    AppThemeEditor {
        val connections = PublicizeConnectionList()
        val connection1 = PublicizeConnection().apply {
            connectionId = 0
            service = "tumblr"
            label = "Tumblr"
            externalId = "myblog.tumblr.com"
            externalName = "My blog"
            externalProfilePictureUrl =
                "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png"
        }
        val connection2 = PublicizeConnection().apply {
            connectionId = 1
            service = "linkedin"
            label = "LinkedIn"
            externalId = "linkedin.com"
            externalName = "My Profile"
            externalProfilePictureUrl =
                "https://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-linkedin-2x.png"
        }
        connections.add(connection1)
        connections.add(connection2)
        EditPostSettingsJetpackSocialContainerView(
            trainOfIconsModels = listOf(
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_three),
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_two),
                TrainOfIconsModel(R.drawable.login_prologue_third_asset_one),
                TrainOfIconsModel(R.mipmap.app_icon)
            ),
            postSocialConnectionList = PostSocialConnection.fromPublicizeConnectionList(connections),
            shareMessage = "Share message.",
            remainingSharesMessage = "27/30 Social shares remaining in the next 30 days",
            subscribeButtonLabel = "Subscribe to share more",
            onSubscribeClick = {},
        )
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEditPostSettingsJetpackSocialContainerEmptyView() {
    AppThemeEditor {
        EditPostSettingsJetpackSocialContainerView(
            trainOfIconsModels = listOf(
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_three),
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_two),
                TrainOfIconsModel(R.drawable.login_prologue_third_asset_one),
                TrainOfIconsModel(R.mipmap.app_icon)
            ),
            postSocialConnectionList = emptyList(),
            shareMessage = "Share message.",
            remainingSharesMessage = "27/30 Social shares remaining in the next 30 days",
            subscribeButtonLabel = "Subscribe to share more",
            onSubscribeClick = {},
        )
    }
}
