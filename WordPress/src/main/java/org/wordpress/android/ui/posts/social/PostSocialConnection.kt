package org.wordpress.android.ui.posts.social

import org.wordpress.android.models.PublicizeConnection

data class PostSocialConnection(
    val connectionId: Int,
    val service: String,
    val label: String,
    val externalId: String,
    val externalName: String,
    val iconUrl: String,
    val isSharingEnabled: Boolean,
) {
    companion object {
        fun fromPublicizeConnection(connection: PublicizeConnection): PostSocialConnection {
            return PostSocialConnection(
                connectionId = connection.connectionId,
                service = connection.service,
                label = connection.label,
                externalId = connection.externalId,
                externalName = connection.externalName,
                iconUrl = connection.externalProfilePictureUrl,
                isSharingEnabled = true, // default to true
            )
        }
    }
}
