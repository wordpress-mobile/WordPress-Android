package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState.Loaded
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState.NoConnections
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.util.StringProvider
import javax.inject.Inject

class EditPostPublishSettingsJetpackSocialUiStateMapper @Inject constructor(
    private val stringProvider: StringProvider,
    private val localeProvider: LocaleProvider,
) {
    fun mapLoaded(
        connections: List<PublicizeConnection>,
        shareLimit: ShareLimit,
        onSubscribeClick: () -> Unit
    ): Loaded =
        Loaded(
            postSocialConnectionList = PostSocialConnection.fromPublicizeConnectionList(connections),
            showShareLimitUi = shareLimit is ShareLimit.Enabled,
            // TODO
            shareMessage = "Message",
            remainingSharesMessage = mapRemainingSharesMessage(shareLimit),
            subscribeButtonLabel = stringProvider.getString(R.string.post_settings_jetpack_social_subscribe_share_more)
                .uppercase(localeProvider.getAppLocale()),
            onSubscribeClick = onSubscribeClick,
        )

    private fun mapRemainingSharesMessage(shareLimit: ShareLimit) =
        if (shareLimit is ShareLimit.Enabled) {
            stringProvider.getString(R.string.jetpack_social_social_shares_remaining, shareLimit.sharesRemaining)
        } else ""

    fun mapNoConnections(
        onConnectProfilesClick: () -> Unit,
    ): NoConnections =
        NoConnections(
            // TODO
            trainOfIconsModels = PublicizeServiceIcon.values().map { TrainOfIconsModel(it.iconResId) },
            message = stringProvider.getString(
                R.string.post_settings_jetpack_social_connect_social_profiles_message
            ),
            connectProfilesButtonLabel = stringProvider.getString(
                R.string.post_settings_jetpack_social_connect_social_profiles_button
            ).uppercase(localeProvider.getAppLocale()),
            onConnectProfilesClick = onConnectProfilesClick,
        )
}
