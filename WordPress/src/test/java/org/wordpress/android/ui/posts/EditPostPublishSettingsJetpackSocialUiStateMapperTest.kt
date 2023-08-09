package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleProvider
import org.wordpress.android.util.StringProvider
import java.util.Locale

class EditPostPublishSettingsJetpackSocialUiStateMapperTest {
    private val stringProvider: StringProvider = mock()
    private val localProvider: LocaleProvider = mock()
    private val publicizeTableWrapper: PublicizeTableWrapper = mock()
    private lateinit var connection1: PostSocialConnection
    private lateinit var connection2: PostSocialConnection

    private val classToTest = EditPostPublishSettingsJetpackSocialUiStateMapper(
        stringProvider = stringProvider,
        localeProvider = localProvider,
        publicizeTableWrapper = publicizeTableWrapper,
    )

    @Before
    fun setUp() {
        connection1 = PostSocialConnection(
            connectionId = 0,
            service = "tumblr",
            label = "Tumblr",
            externalId = "myblog.tumblr.com",
            externalName = "My blog",
            iconResId = R.drawable.ic_social_tumblr,
            isSharingEnabled = true,
        )

        connection2 = PostSocialConnection(
            connectionId = 1,
            service = "linkedin",
            label = "LinkedIn",
            externalId = "linkedin.com",
            externalName = "My Profile",
            iconResId = R.drawable.ic_social_linkedin,
            isSharingEnabled = true,
        )

        whenever(localProvider.getAppLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `Should map loaded UI state with share limit enabled`() {
        val shareLimit = ShareLimit.Enabled(
            shareLimit = 10,
            publicizedCount = 11,
            sharedPostsCount = 12,
            sharesRemaining = 13,
        )
        val shareMessage = "Message"
        val subscribeButtonLabel = "SHARE MORE"
        mockStringResource(R.string.post_settings_jetpack_social_subscribe_share_more, subscribeButtonLabel)
        val allConnections = listOf(connection1, connection2)
        val onConnectProfilesClick: (JetpackSocialFlow) -> Unit = {}
        val onShareMessageClick: () -> Unit = {}
        val onConnectionClick: (
            connection: PostSocialConnection,
            enable: Boolean,
            flow: JetpackSocialFlow
        ) -> Unit = mock()

        val actual = classToTest.mapLoaded(
            connections = allConnections,
            shareLimit = shareLimit,
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            onSubscribeClick = onConnectProfilesClick,
            shareMessage = shareMessage,
            onShareMessageClick = onShareMessageClick,
            onConnectionClick = onConnectionClick,
            isPostPublished = false
        )
        val expected = JetpackSocialUiState.Loaded(
            jetpackSocialConnectionDataList = listOf(
                JetpackSocialConnectionData(
                    postSocialConnection = connection1,
                    onConnectionClick = actual.jetpackSocialConnectionDataList[0].onConnectionClick,
                    enabled = true
                ),
                JetpackSocialConnectionData(
                    postSocialConnection = connection2,
                    onConnectionClick = actual.jetpackSocialConnectionDataList[1].onConnectionClick,
                    enabled = true
                )
            ),
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            showShareLimitUi = true,
            isShareMessageEnabled = true,
            shareMessage = shareMessage,
            onShareMessageClick = onShareMessageClick,
            subscribeButtonLabel = subscribeButtonLabel,
            onSubscribeClick = onConnectProfilesClick
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Loaded UI state should map the expected lambda for on onConnectionClick`() {
        val shareLimit = ShareLimit.Enabled(
            shareLimit = 10,
            publicizedCount = 11,
            sharedPostsCount = 12,
            sharesRemaining = 13,
        )
        val shareMessage = "Message"
        val subscribeButtonLabel = "SHARE MORE"
        mockStringResource(R.string.post_settings_jetpack_social_subscribe_share_more, subscribeButtonLabel)
        val allConnections = listOf(connection1)

        val onConnectProfilesClick: (JetpackSocialFlow) -> Unit = {}
        val onShareMessageClick: () -> Unit = {}
        val onConnectionClick: (
            connection: PostSocialConnection,
            enable: Boolean,
            flow: JetpackSocialFlow
        ) -> Unit = mock()

        val actual = classToTest.mapLoaded(
            connections = allConnections,
            shareLimit = shareLimit,
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            onSubscribeClick = onConnectProfilesClick,
            shareMessage = shareMessage,
            onShareMessageClick = onShareMessageClick,
            onConnectionClick = onConnectionClick,
            isPostPublished = true
        )

        actual.jetpackSocialConnectionDataList[0].onConnectionClick.invoke(false, JetpackSocialFlow.POST_SETTINGS)
        verify(onConnectionClick, times(1)).invoke(connection1, false, JetpackSocialFlow.POST_SETTINGS)
    }

    @Test
    fun `Should map loaded UI state with share limit disabled`() {
        val shareLimit = ShareLimit.Disabled
        val shareMessage = "Message"
        val subscribeButtonLabel = "SHARE MORE"
        mockStringResource(R.string.post_settings_jetpack_social_subscribe_share_more, subscribeButtonLabel)
        val allConnections = listOf(connection2)

        val onConnectProfilesClick: (JetpackSocialFlow) -> Unit = mock()
        val onShareMessageClick: () -> Unit = {}
        val onConnectionClick: (
            connection: PostSocialConnection,
            enable: Boolean,
            flow: JetpackSocialFlow
        ) -> Unit = mock()

        val actual = classToTest.mapLoaded(
            connections = allConnections,
            shareLimit = shareLimit,
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            onSubscribeClick = onConnectProfilesClick,
            shareMessage = shareMessage,
            onShareMessageClick = onShareMessageClick,
            onConnectionClick = onConnectionClick,
            isPostPublished = true
        )
        val expected = JetpackSocialUiState.Loaded(
            jetpackSocialConnectionDataList = listOf(
                JetpackSocialConnectionData(
                    postSocialConnection = connection2,
                    onConnectionClick = actual.jetpackSocialConnectionDataList[0].onConnectionClick,
                    enabled = false
                ),
            ),
            socialSharingModel = FAKE_SOCIAL_SHARING_MODEL,
            showShareLimitUi = false,
            isShareMessageEnabled = false,
            shareMessage = "Message",
            onShareMessageClick = onShareMessageClick,
            subscribeButtonLabel = subscribeButtonLabel,
            onSubscribeClick = onConnectProfilesClick
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should map no connections UI state`() {
        val connectAccountsButtonLabel = "Connect accounts"
        mockStringResource(
            R.string.post_settings_jetpack_social_connect_social_profiles_button, connectAccountsButtonLabel
        )
        val connectAccountsMessage = "Connect accounts message"
        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_connect_social_profiles_message))
            .thenReturn(connectAccountsMessage)
        val connectAccountsNotNowButtonLabel = "Not now"
        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_connect_not_now_button))
            .thenReturn(connectAccountsNotNowButtonLabel)
        val publicizeServices = listOf(
            // Publicize services with ID that matches any serviceId in PublicizeServiceIcon and also
            // have OK status will have their icons shown
            PublicizeService().apply {
                id = "tumblr"
                status = PublicizeService.Status.OK
            },
            // Unsupported publicize services will be filtered
            PublicizeService().apply {
                id = "tumblr"
                status = PublicizeService.Status.UNSUPPORTED
            },
            // Publicize services with unknown or null ID will be filtered
            PublicizeService().apply {
                id = null
            },
        )
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(publicizeServices)
        val onConnectProfilesClick: (JetpackSocialFlow) -> Unit = mock()
        val onNotNowClick: (JetpackSocialFlow) -> Unit = mock()
        val actual = classToTest.mapNoConnections(onConnectProfilesClick, onNotNowClick)
        val expected = JetpackSocialUiState.NoConnections(
            trainOfIconsModels = listOf(TrainOfIconsModel(PublicizeServiceIcon.TUMBLR.iconResId)),
            message = connectAccountsMessage,
            connectProfilesButtonLabel = connectAccountsButtonLabel,
            onConnectProfilesClick = onConnectProfilesClick,
            notNowButtonLabel = "Not now",
            onNotNowClick = onNotNowClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun mockStringResource(stringResId: Int, stringLabel: String) {
        whenever(stringProvider.getString(stringResId)).thenReturn(stringLabel)
    }

    companion object {
        private val FAKE_SOCIAL_SHARING_MODEL = PostSocialSharingModel(
            title = "Title",
            description = "Description",
            iconModels = emptyList(),
        )
    }
}
