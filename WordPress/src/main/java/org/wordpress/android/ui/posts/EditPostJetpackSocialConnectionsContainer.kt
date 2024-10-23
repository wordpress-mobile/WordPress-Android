package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.ui.compose.theme.AppThemeM2Editor
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialMessageItem
import org.wordpress.android.usecase.social.JetpackSocialFlow

@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.EditPostJetpackSocialConnectionsContainer(
    jetpackSocialConnectionDataList: List<JetpackSocialConnectionData>,
    jetpackSocialFlow: JetpackSocialFlow,
    shareMessage: String,
    isShareMessageEnabled: Boolean,
    onShareMessageClick: () -> Unit,
) {
    EditPostJetpackSocialConnectionsList(
        jetpackSocialConnectionDataList = jetpackSocialConnectionDataList,
        jetpackSocialFlow = jetpackSocialFlow,
    )
    PostSocialMessageItem(
        message = shareMessage,
        modifier = Modifier
            .fillMaxWidth(),
        enabled = isShareMessageEnabled,
        onClick = onShareMessageClick
    )
    Divider()
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostJetpackSocialContainerWithShareLimitPreview() {
    AppThemeM2Editor {
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
            EditPostJetpackSocialConnectionsContainer(
                jetpackSocialConnectionDataList = connections,
                shareMessage = "Share message.",
                isShareMessageEnabled = true,
                onShareMessageClick = {},
                jetpackSocialFlow = JetpackSocialFlow.POST_SETTINGS,
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostJetpackSocialContainerWithoutShareLimitPreview() {
    AppThemeM2Editor {
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
            EditPostJetpackSocialConnectionsContainer(
                jetpackSocialConnectionDataList = connections,
                shareMessage = "Share message.",
                isShareMessageEnabled = true,
                onShareMessageClick = {},
                jetpackSocialFlow = JetpackSocialFlow.POST_SETTINGS,
            )
        }
    }
}
