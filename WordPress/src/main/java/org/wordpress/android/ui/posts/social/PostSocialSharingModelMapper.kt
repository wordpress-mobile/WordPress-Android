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
        val title = mapTitle(connections)
        val description = if (shareLimit is ShareLimit.Enabled) mapDescription(shareLimit) else ""
        val iconModels = mapIconModels(connections)
        val isLowOnShares = mapIsLowOnShares(shareLimit, connections)
        return PostSocialSharingModel(
            title = title,
            description = description,
            iconModels = iconModels,
            isLowOnShares = isLowOnShares,
        )
    }

    private fun mapDescription(shareLimit: ShareLimit.Enabled): String {
        val sharesRemaining = shareLimit.sharesRemaining.coerceAtLeast(0)
        return if (sharesRemaining == 1) {
            stringProvider.getString(R.string.jetpack_social_social_shares_remaining_one)
        } else {
            stringProvider.getString(R.string.jetpack_social_social_shares_remaining, sharesRemaining)
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
        connections.map { connection ->
            val iconResId = PublicizeServiceIcon.fromServiceId(connection.service)?.iconResId
            TrainOfIconsModel(
                data = iconResId,
                alpha = if (connection.isSharingEnabled) 1f else 0.3f
            )
        }.sortedBy { it.alpha }

    private fun mapIsLowOnShares(
        shareLimit: ShareLimit,
        connections: List<PostSocialConnection>
    ): Boolean {
        if (shareLimit !is ShareLimit.Enabled) return false

        val sharingEnabledConnections = connections.filter { it.isSharingEnabled }
        return when {
            // No more shares left.
            shareLimit.sharesRemaining <= 0 -> true
            // Sharing to some accounts, but not enough shares for all.
            connections.size > sharingEnabledConnections.size && shareLimit.sharesRemaining < connections.size -> true
            // Remaining shares < no. of accounts.
            shareLimit.sharesRemaining < connections.size -> true
            else -> false
        }
    }
}
