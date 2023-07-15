package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeConnectionList
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialConnectionItem

@Composable
fun EditPostSettingsJetpackSocialConnectionsList(postSocialConnectionList: List<PostSocialConnection>) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        postSocialConnectionList.forEachIndexed { _, connection ->
            PostSocialConnectionItem(
                connection = connection,
                onSharingChange = {},//TODO
            )
            Divider()
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEditPostSettingsJetpackSocialConnectionsList() {
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
        EditPostSettingsJetpackSocialConnectionsList(
            postSocialConnectionList = PostSocialConnection.fromPublicizeConnectionList(connections),
        )
    }
}
