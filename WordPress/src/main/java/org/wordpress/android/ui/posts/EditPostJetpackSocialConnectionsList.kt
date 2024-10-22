package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.ui.compose.theme.AppThemeM2Editor
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialConnectionItem
import org.wordpress.android.usecase.social.JetpackSocialFlow

@Composable
fun EditPostJetpackSocialConnectionsList(
    jetpackSocialConnectionDataList: List<JetpackSocialConnectionData>,
    jetpackSocialFlow: JetpackSocialFlow,
) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        jetpackSocialConnectionDataList.forEach {
            PostSocialConnectionItem(
                connection = it.postSocialConnection,
                onSharingChange = { newValue -> it.onConnectionClick(newValue, jetpackSocialFlow) },
                enabled = it.enabled,
            )
            Divider()
        }
    }
}

data class JetpackSocialConnectionData(
    val postSocialConnection: PostSocialConnection,
    val onConnectionClick: (Boolean, JetpackSocialFlow) -> Unit,
    val enabled: Boolean = true,
)

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEditPostJetpackSocialConnectionsList() {
    AppThemeM2Editor {
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
                onConnectionClick = { _, _ -> },
            )
        )
        EditPostJetpackSocialConnectionsList(
            jetpackSocialConnectionDataList = connections,
            jetpackSocialFlow = JetpackSocialFlow.POST_SETTINGS
        )
    }
}
