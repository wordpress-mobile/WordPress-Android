package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialMessageItem

@Composable
fun ColumnScope.EditPostSettingsJetpackSocialConnectionsContainer(
    jetpackSocialConnectionDataList: List<JetpackSocialConnectionData>,
    shareMessage: String,
    isShareMessageEnabled: Boolean,
    onShareMessageClick: () -> Unit,
) {
    EditPostSettingsJetpackSocialConnectionsList(
        jetpackSocialConnectionDataList = jetpackSocialConnectionDataList,
    )
    PostSocialMessageItem(
        message = shareMessage,
        modifier = Modifier
            .padding(
                vertical = Margin.MediumLarge.value,
            )
            .fillMaxWidth(),
        enabled = isShareMessageEnabled,
        onClick = onShareMessageClick
    )
    Divider()
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialContainerWithShareLimitPreview() {
    AppThemeEditor {
        Column {
            val connections = mutableListOf<JetpackSocialConnectionData>()
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
            connections.add(
                JetpackSocialConnectionData(
                    postSocialConnection = PostSocialConnection.fromPublicizeConnection(connection1, true),
                    onConnectionClick = { _, _ -> },
                )
            )
            connections.add(
                JetpackSocialConnectionData(
                    postSocialConnection = PostSocialConnection.fromPublicizeConnection(connection2, false),
                    onConnectionClick = { _, _ -> }
                )
            )
            EditPostSettingsJetpackSocialConnectionsContainer(
                jetpackSocialConnectionDataList = connections,
                shareMessage = "Share message.",
                isShareMessageEnabled = true,
                onShareMessageClick = {},
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialContainerWithoutShareLimitPreview() {
    AppThemeEditor {
        Column {
            val connections = mutableListOf<JetpackSocialConnectionData>()
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
            connections.add(
                JetpackSocialConnectionData(
                    postSocialConnection = PostSocialConnection.fromPublicizeConnection(connection1, true),
                    onConnectionClick = { _, _ -> },
                )
            )
            connections.add(
                JetpackSocialConnectionData(
                    postSocialConnection = PostSocialConnection.fromPublicizeConnection(connection2, false),
                    onConnectionClick = { _, _ -> }
                )
            )
            EditPostSettingsJetpackSocialConnectionsContainer(
                jetpackSocialConnectionDataList = connections,
                shareMessage = "Share message.",
                isShareMessageEnabled = true,
                onShareMessageClick = {},
            )
        }
    }
}
