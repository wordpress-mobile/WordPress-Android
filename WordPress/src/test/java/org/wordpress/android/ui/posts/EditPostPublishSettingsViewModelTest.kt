package org.wordpress.android.ui.posts

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.ui.people.utils.PeopleUtilsWrapper
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.ActionEvent
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar

@ExperimentalCoroutinesApi
class EditPostPublishSettingsViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val postSettingsUtils: PostSettingsUtils = mock()
    private val peopleUtilsWrapper: PeopleUtilsWrapper = mock()
    private val localeManagerWrapper: LocaleManagerWrapper = mock()
    private val postSchedulingNotificationStore: PostSchedulingNotificationStore = mock()
    private val siteStore: SiteStore = mock()
    private val jetpackSocialFeatureConfig: JetpackSocialFeatureConfig = mock()
    private val accountStore: AccountStore = mock()
    private val getPublicizeConnectionsForUserUseCase: GetPublicizeConnectionsForUserUseCase = mock()
    private val getJetpackSocialShareMessageUseCase: GetJetpackSocialShareMessageUseCase = mock()
    private val getJetpackSocialShareLimitStatusUseCase: GetJetpackSocialShareLimitStatusUseCase = mock()
    private val jetpackUiStateMapper: EditPostPublishSettingsJetpackSocialUiStateMapper = mock()
    private val postSocialSharingModelMapper: PostSocialSharingModelMapper = mock()
    private val publicizeTableWrapper: PublicizeTableWrapper = mock()

    private val classToTest = EditPostPublishSettingsViewModel(
        resourceProvider = resourceProvider,
        postSettingsUtils = postSettingsUtils,
        peopleUtilsWrapper = peopleUtilsWrapper,
        localeManagerWrapper = localeManagerWrapper,
        postSchedulingNotificationStore = postSchedulingNotificationStore,
        siteStore = siteStore,
        jetpackSocialFeatureConfig = jetpackSocialFeatureConfig,
        accountStore = accountStore,
        getPublicizeConnectionsForUserUseCase = getPublicizeConnectionsForUserUseCase,
        getJetpackSocialShareMessageUseCase = getJetpackSocialShareMessageUseCase,
        getJetpackSocialShareLimitStatusUseCase = getJetpackSocialShareLimitStatusUseCase,
        jetpackUiStateMapper = jetpackUiStateMapper,
        postSocialSharingModelMapper = postSocialSharingModelMapper,
        publicizeTableWrapper = publicizeTableWrapper,
    )

    private val showJetpackSocialContainerObserver: Observer<Boolean> = mock()
    private val jetpackSocialUiStateObserver: Observer<JetpackSocialUiState> = mock()
    private val actionEventsObserver: Observer<ActionEvent> = mock()
    private val editPostRepository: EditPostRepository = mock()
    private val remoteSiteId = 123L
    private val userId = 456L
    private val siteModel = SiteModel().apply {
        siteId = 12345
    }

    @Before
    fun setup() {
        classToTest.showJetpackSocialContainer.observeForever(showJetpackSocialContainerObserver)
        classToTest.jetpackSocialUiState.observeForever(jetpackSocialUiStateObserver)
        classToTest.actionEvents.observeForever(actionEventsObserver)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(Calendar.getInstance())
        whenever(resourceProvider.getString(R.string.immediately)).thenReturn("Immediately")
    }

    @Test
    fun `Should NOT load jetpack social if FF is disabled`() = test {
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(false)
        classToTest.start(null)
        verify(getPublicizeConnectionsForUserUseCase, never()).execute(any(), any(), any())
    }

    @Test
    fun `Should hide jetpack social container if FF is disabled`() = test {
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(false)
        classToTest.start(null)
        verify(showJetpackSocialContainerObserver).onChanged(false)
    }

    @Test
    fun `Should hide jetpack social container if FF is enabled but SiteModel is null`() = test {
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(null)
        verify(showJetpackSocialContainerObserver).onChanged(false)
    }

    @Test
    fun `Should show jetpack social container if FF is enabled`() = test {
        mockSiteModel(true)
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(editPostRepository)
        verify(showJetpackSocialContainerObserver).onChanged(true)
    }

    @Test
    fun `Should get publicize connections for user if jetpack social FF is enabled`() = test {
        mockSiteModel()
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(editPostRepository)
        verify(getPublicizeConnectionsForUserUseCase).execute(remoteSiteId, userId)
    }

    @Test
    fun `Should get jetpack social share limit status if jetpack social FF is enabled`() = test {
        mockSiteModel()
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(editPostRepository)
        verify(getJetpackSocialShareLimitStatusUseCase).execute(siteModel)
    }

    @Test
    fun `Should map no connections UI state if connections list is empty and jetpack social FF is enabled`() = test {
        mockSiteModel(true)
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(PublicizeService().apply { status = PublicizeService.Status.OK }))
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)

        classToTest.start(editPostRepository)
        verify(jetpackUiStateMapper).mapNoConnections(any())
    }

    @Test
    fun `Should emit no connections UI state if connections list is empty and jetpack social FF is enabled`() = test {
        val noConnections = JetpackSocialUiState.NoConnections(
            trainOfIconsModels = listOf(),
            message = "message",
            connectProfilesButtonLabel = "label",
            onConnectProfilesClick = {},
        )
        mockSiteModel(true)
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(PublicizeService().apply { status = PublicizeService.Status.OK }))
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(jetpackUiStateMapper.mapNoConnections(any()))
            .thenReturn(noConnections)
        classToTest.start(editPostRepository)
        verify(jetpackSocialUiStateObserver).onChanged(noConnections)
    }

    @Test
    fun `Should map loaded UI state if connections list is NOT empty and jetpack social FF is enabled`() = test {
        mockSiteModel(true)
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(
                listOf(
                    PublicizeConnection().apply {
                        connectionId = 0
                        service = "tumblr"
                        label = "Tumblr"
                        externalId = "myblog.tumblr.com"
                        externalName = "My blog"
                        externalProfilePictureUrl =
                            "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png"
                    },
                )
            )
        whenever(getJetpackSocialShareMessageUseCase.execute(any()))
            .thenReturn("Message")
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        classToTest.start(editPostRepository)
        verify(jetpackUiStateMapper).mapLoaded(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `Should emit loaded UI state if connections list is NOT empty and jetpack social FF is enabled`() = test {
        val loaded = JetpackSocialUiState.Loaded(
            jetpackSocialConnectionDataList = listOf(
                JetpackSocialConnectionData(
                    postSocialConnection = PostSocialConnection(
                        connectionId = 1,
                        service = "service",
                        label = "label",
                        externalId = "externalId",
                        externalName = "externalName",
                        iconUrl = "iconUrl",
                        isSharingEnabled = true
                    ),
                    onConnectionClick = {},
                    enabled = false
                )
            ),
            showShareLimitUi = true,
            isShareMessageEnabled = false,
            shareMessage = "message",
            onShareMessageClick = {},
            subscribeButtonLabel = "label",
            onSubscribeClick = {}
        )

        mockSiteModel(true)
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(
                listOf(
                    PublicizeConnection().apply {
                        connectionId = 0
                        service = "tumblr"
                        label = "Tumblr"
                        externalId = "myblog.tumblr.com"
                        externalName = "My blog"
                        externalProfilePictureUrl =
                            "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png"
                    },
                )
            )
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(getJetpackSocialShareMessageUseCase.execute(any()))
            .thenReturn("Message")
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(jetpackUiStateMapper.mapLoaded(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(loaded)
        classToTest.start(editPostRepository)
        verify(jetpackSocialUiStateObserver).onChanged(loaded)
    }

    @Test
    fun `Should reload jetpack social on screen shown if last emitted action was OpenSocialConnectionsList`() = test {
        val loaded = JetpackSocialUiState.Loaded(
            jetpackSocialConnectionDataList = listOf(
                JetpackSocialConnectionData(
                    postSocialConnection = PostSocialConnection(
                        connectionId = 1,
                        service = "service",
                        label = "label",
                        externalId = "externalId",
                        externalName = "externalName",
                        iconUrl = "iconUrl",
                        isSharingEnabled = true
                    ),
                    onConnectionClick = {},
                    enabled = false
                )
            ),
            showShareLimitUi = true,
            isShareMessageEnabled = false,
            shareMessage = "message",
            onShareMessageClick = {},
            subscribeButtonLabel = "label",
            onSubscribeClick = {}
        )

        mockSiteModel(true)
        mockUserId()
        whenever(getPublicizeConnectionsForUserUseCase.execute(any(), any(), any()))
            .thenReturn(
                listOf(
                    PublicizeConnection().apply {
                        connectionId = 0
                        service = "tumblr"
                        label = "Tumblr"
                        externalId = "myblog.tumblr.com"
                        externalName = "My blog"
                        externalProfilePictureUrl =
                            "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png"
                    },
                )
            )
        whenever(getJetpackSocialShareLimitStatusUseCase.execute(any()))
            .thenReturn(ShareLimit.Disabled)
        whenever(getJetpackSocialShareMessageUseCase.execute(any()))
            .thenReturn("Message")
        whenever(jetpackSocialFeatureConfig.isEnabled())
            .thenReturn(true)
        whenever(jetpackUiStateMapper.mapLoaded(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(loaded)

        classToTest.start(editPostRepository)
        classToTest.onScreenShown()

        verify(jetpackSocialUiStateObserver).onChanged(loaded)
    }

    @Test
    fun `Should NOT reload jetpack social on screen shown if last emitted action was NOT OpenSocialConnectionsList`() {
        classToTest.onScreenShown()
        verify(jetpackSocialUiStateObserver, never()).onChanged(any())
    }

    @Test
    fun `Should emit OpenSocialConnectionList when onJetpackSocialConnectProfilesClick is called`() {
        mockSiteModel()
        classToTest.start(editPostRepository)
        classToTest.onJetpackSocialConnectProfilesClick()
        verify(actionEventsObserver).onChanged(
            ActionEvent.OpenSocialConnectionsList(
                siteModel = siteModel,
            )
        )
    }

    private fun mockSiteModel(supportsPublicize: Boolean = false) {
        whenever(editPostRepository.localSiteId)
            .thenReturn(1)
        whenever(siteStore.getSiteByLocalId(1))
            .thenReturn(siteModel.apply {
                siteId = remoteSiteId
            }.also {
                if (supportsPublicize) {
                    it.apply {
                        origin = SiteModel.ORIGIN_WPCOM_REST
                        hasCapabilityPublishPosts = true
                        setIsPublicizePermanentlyDisabled(false)
                    }
                }
            })
    }

    private fun mockUserId() {
        val accountModel: AccountModel = mock()
        whenever(accountStore.account)
            .thenReturn(accountModel)
        whenever(accountModel.userId)
            .thenReturn(userId)
    }
}
