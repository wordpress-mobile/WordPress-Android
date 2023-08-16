package org.wordpress.android.ui.posts.social

import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.ui.publicize.PublicizeServiceIcon

@Suppress("LongParameterList", "DataClassShouldBeImmutable")
data class PostSocialConnection(
    val connectionId: Int,
    val service: String,
    val label: String,
    val externalId: String,
    val externalName: String,
    val iconResId: Int?,
    var isSharingEnabled: Boolean,
) {
    companion object {
        fun fromPublicizeConnection(connection: PublicizeConnection, isSharingEnabled: Boolean): PostSocialConnection {
            return PostSocialConnection(
                connectionId = connection.connectionId,
                service = connection.service,
                label = connection.label,
                externalId = connection.externalId,
                externalName = connection.externalDisplayName,
                iconResId = PublicizeServiceIcon.fromServiceId(connection.service)?.iconResId,
                isSharingEnabled = isSharingEnabled,
            )
        }
    }
}
