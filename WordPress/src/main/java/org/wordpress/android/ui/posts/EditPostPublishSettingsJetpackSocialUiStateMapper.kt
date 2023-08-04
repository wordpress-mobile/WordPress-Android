package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.Loaded
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.NoConnections
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.util.StringProvider
import javax.inject.Inject

class EditPostPublishSettingsJetpackSocialUiStateMapper @Inject constructor(
    private val stringProvider: StringProvider,
    private val localeProvider: LocaleProvider,
    private val publicizeTableWrapper: PublicizeTableWrapper,
) {
    @Suppress("LongParameterList")
    fun mapLoaded(
        connections: List<PostSocialConnection>,
        shareLimit: ShareLimit,
        socialSharingModel: PostSocialSharingModel,
        onSubscribeClick: (JetpackSocialFlow) -> Unit,
        shareMessage: String,
        onShareMessageClick: () -> Unit,
        onConnectionClick: (PostSocialConnection, Boolean, JetpackSocialFlow) -> Unit,
        isPostPublished: Boolean,
    ): Loaded {
        val selectedConnectionsSize = connections.filter { it.isSharingEnabled }.size
        return Loaded(
            jetpackSocialConnectionDataList = connections.map { connection ->
                JetpackSocialConnectionData(
                    postSocialConnection = connection,
                    onConnectionClick = { newValue, flow -> onConnectionClick(connection, newValue, flow) },
                    enabled = !isPostPublished && if (shareLimit is ShareLimit.Enabled) {
                        if (!connection.isSharingEnabled) {
                            shareLimit.sharesRemaining > selectedConnectionsSize
                        } else shareLimit.sharesRemaining > 0
                    } else true
                )
            },
            socialSharingModel = socialSharingModel,
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
        onConnectProfilesClick: (JetpackSocialFlow) -> Unit,
        onNotNowClick: (JetpackSocialFlow) -> Unit,
    ): NoConnections =
        NoConnections(
            trainOfIconsModels = publicizeTableWrapper.getServiceList()
                .filter { it.status != PublicizeService.Status.UNSUPPORTED }
                .mapNotNull { PublicizeServiceIcon.fromServiceId(it.id) }
                .map { TrainOfIconsModel(it.iconResId) },
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
