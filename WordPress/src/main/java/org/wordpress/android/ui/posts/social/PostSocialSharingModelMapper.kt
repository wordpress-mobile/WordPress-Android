package org.wordpress.android.ui.posts.social

import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.StringProvider
import org.wordpress.android.util.extensions.hasOneElement
import javax.inject.Inject

class PostSocialSharingModelMapper @Inject constructor(
    private val stringProvider: StringProvider,
) {
    fun map(
        connections: List<PostSocialConnection>,
        shareLimit: ShareLimit,
    ): PostSocialSharingModel {
        return if (shareLimit is ShareLimit.Enabled) {
            val title = mapTitle(connections)
            val description = stringProvider.getString(
                R.string.jetpack_social_social_shares_remaining,
                (shareLimit.sharesRemaining - connections.filter { it.isSharingEnabled }.size)
                    .coerceAtLeast(0)
            )
            val iconModels = mapIconModels(connections)
            val isLowOnShares = mapIsLowOnShares(shareLimit, connections)
            PostSocialSharingModel(
                title = title,
                description = description,
                iconModels = iconModels,
                isLowOnShares = isLowOnShares,
            )
        } else {
            PostSocialSharingModel(
                title = "",
                description = "",
                iconModels = emptyList(),
                isLowOnShares = false,
            )
        }
    }

    private fun mapTitle(connections: List<PostSocialConnection>): String {
        val sharingEnabledConnections = connections.filter { it.isSharingEnabled }
        return when {
            // Sharing to 0 accounts
            sharingEnabledConnections.isEmpty() ->
                stringProvider.getString(R.string.jetpack_social_social_shares_title_not_sharing)
            // Sharing to 1 out of 1 account
            sharingEnabledConnections.hasOneElement() && connections.hasOneElement() ->
                stringProvider.getString(
                    R.string.jetpack_social_social_shares_title_single_account,
                    sharingEnabledConnections.single().externalName
                )
            // Sharing to all accounts
            sharingEnabledConnections.size == connections.size ->
                stringProvider.getString(
                    R.string.jetpack_social_social_shares_title_all_accounts,
                    connections.size
                )
            // Sharing to some accounts
            else -> stringProvider.getString(
                R.string.jetpack_social_social_shares_title_part_of_the_accounts,
                sharingEnabledConnections.size,
                connections.size
            )
        }
    }

    private fun mapIconModels(connections: List<PostSocialConnection>) =
        connections.map {
            Pair(PublicizeServiceIcon.fromServiceId(it.service)?.iconResId, it)
        }.map { (iconResId, connection) ->
            TrainOfIconsModel(
                data = iconResId,
                alpha = if (connection.isSharingEnabled) 1f else 0.5f
            )
        }

    private fun mapIsLowOnShares(
        shareLimit: ShareLimit.Enabled,
        connections: List<PostSocialConnection>
    ): Boolean {
        val sharingEnabledConnections = connections.filter { it.isSharingEnabled }
        return when {
            // No more shares left.
            shareLimit.sharesRemaining == 0 -> true
            // Remaining shares < no. of accounts.
            shareLimit.sharesRemaining < sharingEnabledConnections.size -> true
            // Sharing to some accounts, but not enough shares for all.
            sharingEnabledConnections.isNotEmpty() && shareLimit.sharesRemaining < connections.size -> true
            else -> false
        }
    }
}
