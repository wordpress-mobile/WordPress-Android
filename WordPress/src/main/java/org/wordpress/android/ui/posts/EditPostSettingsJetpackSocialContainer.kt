package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeConnectionList
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialMessageItem
import org.wordpress.android.ui.posts.social.compose.PostSocialSharesText

@Composable
fun EditPostSettingsJetpackSocialContainer(
    postSocialConnectionList: List<PostSocialConnection>,
    showShareLimitUi: Boolean,
    shareMessage: String,
    remainingSharesMessage: String,
    subscribeButtonLabel: String,
    onSubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        EditPostSettingsJetpackSocialConnectionsList(
            postSocialConnectionList = postSocialConnectionList,
        )
        PostSocialMessageItem(
            message = shareMessage,
            modifier = Modifier.padding(
                vertical = Margin.MediumLarge.value,
            ),
        )
        if (showShareLimitUi) {
            Divider()
            PostSocialSharesText(
                message = remainingSharesMessage,
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
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialContainerWithShareLimitPreview() {
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
        EditPostSettingsJetpackSocialContainer(
            postSocialConnectionList = PostSocialConnection.fromPublicizeConnectionList(connections),
            showShareLimitUi = true,
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
fun EditPostSettingsJetpackSocialContainerWithoutShareLimitPreview() {
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
        EditPostSettingsJetpackSocialContainer(
            postSocialConnectionList = PostSocialConnection.fromPublicizeConnectionList(connections),
            showShareLimitUi = false,
            shareMessage = "Share message.",
            remainingSharesMessage = "",
            subscribeButtonLabel = "",
            onSubscribeClick = {},
        )
    }
}
