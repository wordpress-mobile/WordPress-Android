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
        allConnections: List<PostSocialConnection>,
        selectedConnections: List<PostSocialConnection>,
        shareLimit: ShareLimit,
    ): PostSocialSharingModel {
        val title: String
        val description: String
        val iconModels: List<TrainOfIconsModel>
        val isLowOnShares: Boolean
        if (shareLimit is ShareLimit.Enabled) {
            title = mapTitle(selectedConnections, allConnections)
            description = stringProvider.getString(
                R.string.jetpack_social_social_shares_remaining,
                shareLimit.sharesRemaining
            )
            iconModels = mapIconModels(selectedConnections)
            isLowOnShares = mapIsLowOnShares(shareLimit, selectedConnections, allConnections)
        } else {
            title = ""
            description = ""
            iconModels = emptyList()
            isLowOnShares = false
        }
        return PostSocialSharingModel(
            title = title,
            description = description,
            iconModels = iconModels,
            isLowOnShares = isLowOnShares,
        )
    }

    private fun mapTitle(
        selectedConnections: List<PostSocialConnection>,
        allConnections: List<PostSocialConnection>
    ) = when {
        // Sharing to 0 accounts
        selectedConnections.isEmpty() ->
            stringProvider.getString(R.string.jetpack_social_social_shares_title_not_sharing)
        // Sharing to 1 out of 1 account
        selectedConnections.hasOneElement() && allConnections.hasOneElement() ->
            stringProvider.getString(
                R.string.jetpack_social_social_shares_title_single_account,
                selectedConnections.single().externalName
            )
        // Sharing to all accounts
        selectedConnections.size == allConnections.size ->
            stringProvider.getString(
                R.string.jetpack_social_social_shares_title_all_accounts,
                allConnections.size
            )
        // Sharing to some accounts
        else -> stringProvider.getString(
            R.string.jetpack_social_social_shares_title_part_of_the_accounts,
            selectedConnections.size,
            allConnections.size
        )
    }

    private fun mapIconModels(selectedConnections: List<PostSocialConnection>) =
        selectedConnections.mapNotNull {
            PublicizeServiceIcon.fromServiceId(it.service)?.iconResId
        }.map {
            TrainOfIconsModel(
                data = it,
                alpha = 1f // TODO
            )
        }

    private fun mapIsLowOnShares(
        shareLimit: ShareLimit.Enabled,
        selectedConnections: List<PostSocialConnection>,
        allConnections: List<PostSocialConnection>
    ) = when {
        // No more shares left.
        shareLimit.sharesRemaining == 0 -> true
        // Remaining shares < no. of accounts.
        shareLimit.sharesRemaining < selectedConnections.size -> true
        // Sharing to some accounts, but not enough shares for all.
        selectedConnections.isNotEmpty() && shareLimit.sharesRemaining < allConnections.size -> true
        else -> false
    }
}
