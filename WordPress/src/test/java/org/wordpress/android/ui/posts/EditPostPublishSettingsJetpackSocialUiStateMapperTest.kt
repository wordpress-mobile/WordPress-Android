package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
//import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState
//import org.wordpress.android.ui.posts.social.PostSocialConnection
//import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
//import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
//import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.util.StringProvider
import java.util.Locale

class EditPostPublishSettingsJetpackSocialUiStateMapperTest {
    private val stringProvider: StringProvider = mock()
    private val localProvider: LocaleProvider = mock()
    private val classToTest = EditPostPublishSettingsJetpackSocialUiStateMapper(
        stringProvider = stringProvider,
        localeProvider = localProvider,
    )

//    private val postSocialSharingModel = PostSocialSharingModel(
//        title = "Title",
//        description = "Description",
//        iconModels = listOf(
//            TrainOfIconsModel(R.drawable.ic_social_facebook)
//        ),
//        isLowOnShares = true,
//    )

    @Test
    fun `Should map loaded UI state with share limit enabled`() {
//        val shareLimit = ShareLimit.Enabled(
//            shareLimit = 10,
//            publicizedCount = 11,
//            sharedPostsCount = 12,
//            sharesRemaining = 13,
//        )
//        val shareMessage = "Message"
//        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_subscribe_share_more))
//            .thenReturn("Share more")
//        whenever(localProvider.getAppLocale())
//            .thenReturn(Locale.US)
//        val allConnections = listOf(
//            PostSocialConnection(
//                connectionId = 0,
//                service = "tumblr",
//                label = "Tumblr",
//                externalId = "myblog.tumblr.com",
//                externalName = "My blog",
//                iconUrl =
//                    "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png",
//                isSharingEnabled = true,
//            ),
//            PostSocialConnection(
//                connectionId = 1,
//                service = "linkedin",
//                label = "LinkedIn",
//                externalId = "linkedin.com",
//                externalName = "My Profile",
//                iconUrl =
//                    "https://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-linkedin-2x.png",
//                isSharingEnabled = true,
//            ),
//        )
//        val selectedConnections = listOf(
//            PostSocialConnection(
//                connectionId = 1,
//                service = "linkedin",
//                label = "LinkedIn",
//                externalId = "linkedin.com",
//                externalName = "My Profile",
//                iconUrl =
//                "https://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-linkedin-2x.png",
//                isSharingEnabled = true,
//            ),
//        )
//
//        val onConnectProfilesClick: () -> Unit = {}
//        val onShareMessageClick: () -> Unit = {}
//        val actual = classToTest.mapLoaded(
//            allConnections = allConnections,
//            selectedConnections = selectedConnections,
//            shareLimit = shareLimit,
//            onSubscribeClick = onConnectProfilesClick,
//            shareMessage = shareMessage,
//            onShareMessageClick = onShareMessageClick,
//        )
//        val expected = JetpackSocialUiState.Loaded(
////            val jetpackSocialConnectionDataList: List<JetpackSocialConnectionData>,
////        val showShareLimitUi: Boolean,
////        val isShareMessageEnabled: Boolean,
////        val shareMessage: String,
////        val onShareMessageClick: () -> Unit,
////        val subscribeButtonLabel: String,
////        val onSubscribeClick: () -> Unit,
//            jetpackSocialConnectionDataList = listOf(JetpackSocialConnectionData(
//                postSocialConnection = ,
//                onConnectionClick = onConnectionClick,
//                enabled = true,
//            )),
//            showShareLimitUi = true,
//            shareMessage = shareMessage,
//            onShareMessageClick = onShareMessageClick,
//            postSocialSharingModel = postSocialSharingModel,
//            subscribeButtonLabel = "SHARE MORE",
//            onSubscribeClick = onConnectProfilesClick,
//        )
//        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map loaded UI state with share limit disabled`() {
//        val shareLimit = ShareLimit.Disabled
//        val shareMessage = "Message"
//        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_subscribe_share_more))
//            .thenReturn("Share more")
//        whenever(localProvider.getAppLocale())
//            .thenReturn(Locale.US)
//        val allConnections = listOf(
//            PublicizeConnection().apply {
//                connectionId = 1
//                service = "linkedin"
//                label = "LinkedIn"
//                externalId = "linkedin.com"
//                externalName = "My Profile"
//                externalProfilePictureUrl =
//                    "https://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-linkedin-2x.png"
//            },
//        )
//        val selectedConnections = listOf(
//            PublicizeConnection().apply {
//                connectionId = 1
//                service = "linkedin"
//                label = "LinkedIn"
//                externalId = "linkedin.com"
//                externalName = "My Profile"
//                externalProfilePictureUrl =
//                    "https://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-linkedin-2x.png"
//            },
//        )
//
//        val onConnectProfilesClick: () -> Unit = mock()
//        val onShareMessageClick: () -> Unit = {}
//        val actual = classToTest.mapLoaded(
//            allConnections = allConnections,
//            selectedConnections = selectedConnections,
//            shareLimit = shareLimit,
//            onSubscribeClick = onConnectProfilesClick,
//            shareMessage = shareMessage,
//            onShareMessageClick = onShareMessageClick,
//        )
//        val expected = JetpackSocialUiState.Loaded(
//            postSocialConnectionList = PostSocialConnection.fromPublicizeConnectionList(allConnections),
//            showShareLimitUi = false,
//            shareMessage = "Message",
//            onShareMessageClick = onShareMessageClick,
//            postSocialSharingModel = postSocialSharingModel,
//            subscribeButtonLabel = "SHARE MORE",
//            onSubscribeClick = onConnectProfilesClick,
//        )
//        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map no connections UI state`() {
        val connectProfilesButtonLabel = "Connect profiles button"
        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_connect_social_profiles_button))
            .thenReturn(connectProfilesButtonLabel)
        whenever(localProvider.getAppLocale())
            .thenReturn(Locale.US)
        val connectProfilesMessage = "Connect profiles message"
        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_connect_social_profiles_message))
            .thenReturn(connectProfilesMessage)
        val onConnectProfilesClick: () -> Unit = mock()
        val actual = classToTest.mapNoConnections(onConnectProfilesClick)
        val expected = JetpackSocialUiState.NoConnections(
            trainOfIconsModels = PublicizeServiceIcon.values().map { TrainOfIconsModel(it.iconResId) },
            message = connectProfilesMessage,
            connectProfilesButtonLabel = "CONNECT PROFILES BUTTON",
            onConnectProfilesClick = onConnectProfilesClick,
        )
        assertThat(actual).isEqualTo(expected)
    }
}
