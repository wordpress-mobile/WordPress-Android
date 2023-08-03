package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.Loaded
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.NoConnections
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.util.StringProvider
import javax.inject.Inject

class EditPostPublishSettingsJetpackSocialUiStateMapper @Inject constructor(
    private val stringProvider: StringProvider,
    private val localeProvider: LocaleProvider,
) {
    @Suppress("LongParameterList")
    fun mapLoaded(
        connections: List<PostSocialConnection>,
        shareLimit: ShareLimit,
        onSubscribeClick: () -> Unit,
        shareMessage: String,
        onShareMessageClick: () -> Unit,
        onConnectionClick: (PostSocialConnection, Boolean) -> Unit,
        isPostPublished: Boolean,
    ): Loaded {
        val selectedConnectionsSize = connections.filter { it.isSharingEnabled }.size
        return Loaded(
            jetpackSocialConnectionDataList = connections.map { connection ->
                JetpackSocialConnectionData(
                    postSocialConnection = connection,
                    onConnectionClick = { onConnectionClick(connection, it) },
                    enabled = !isPostPublished && if (shareLimit is ShareLimit.Enabled) {
                        if (!connection.isSharingEnabled) {
                            shareLimit.sharesRemaining > selectedConnectionsSize
                        } else shareLimit.sharesRemaining > 0
                    } else true
                )
            },
            showShareLimitUi = !isPostPublished && shareLimit is ShareLimit.Enabled,
            isShareMessageEnabled = !isPostPublished,
            shareMessage = shareMessage,
            onShareMessageClick = onShareMessageClick,
            subscribeButtonLabel = stringProvider.getString(R.string.post_settings_jetpack_social_subscribe_share_more)
                .uppercase(localeProvider.getAppLocale()),
            onSubscribeClick = onSubscribeClick,
        )
    }

    fun mapNoConnections(
        onConnectProfilesClick: () -> Unit,
        onNotNowClick: (JetpackSocialFlow) -> Unit,
    ): NoConnections =
        NoConnections(
            trainOfIconsModels = PublicizeServiceIcon.values().map { TrainOfIconsModel(it.iconResId) },
            message = stringProvider.getString(
                R.string.post_settings_jetpack_social_connect_social_profiles_message
            ),
            connectProfilesButtonLabel = stringProvider.getString(
                R.string.post_settings_jetpack_social_connect_social_profiles_button
            ),
            onConnectProfilesClick = onConnectProfilesClick,
            notNowButtonLabel = stringProvider.getString(
                R.string.post_settings_jetpack_social_connect_not_now_button
            ),
            onNotNowClick = onNotNowClick,
        )
}
