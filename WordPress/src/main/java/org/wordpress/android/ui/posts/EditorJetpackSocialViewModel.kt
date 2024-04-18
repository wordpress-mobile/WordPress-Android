package org.wordpress.android.ui.posts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PublicizeSkipConnection
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.social.JetpackSocialSharingTracker
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.extensions.doesNotContain
import org.wordpress.android.util.extensions.updatePublicizeSkipConnections
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class EditorJetpackSocialViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val getPublicizeConnectionsForUserUseCase: GetPublicizeConnectionsForUserUseCase,
    private val getJetpackSocialShareMessageUseCase: GetJetpackSocialShareMessageUseCase,
    private val getJetpackSocialShareLimitStatusUseCase: GetJetpackSocialShareLimitStatusUseCase,
    private val jetpackUiStateMapper: EditPostPublishSettingsJetpackSocialUiStateMapper,
    private val postSocialSharingModelMapper: PostSocialSharingModelMapper,
    private val publicizeTableWrapper: PublicizeTableWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val jetpackSocialSharingTracker: JetpackSocialSharingTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private lateinit var siteModel: SiteModel
    private lateinit var editPostRepository: EditPostRepository

    private val _jetpackSocialContainerVisibility = MutableLiveData<JetpackSocialContainerVisibility>()
    val jetpackSocialContainerVisibility: LiveData<JetpackSocialContainerVisibility> = _jetpackSocialContainerVisibility

    private val _jetpackSocialUiState = MutableLiveData<JetpackSocialUiState>()
    val jetpackSocialUiState: LiveData<JetpackSocialUiState> = _jetpackSocialUiState

    // this is a SingleLiveEvent so it should have one, and only one, observer (the Activity)
    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private var jetpackSocialShareMessage = ""
    private lateinit var shareLimit: ShareLimit
    private val connections = mutableListOf<PostSocialConnection>()

    var isStarted: Boolean = false

    private var isLastActionOnResumeHandled = false

    private val currentPost: PostImmutableModel?
        get() = editPostRepository.getPost()

    fun start(siteModel: SiteModel, editPostRepository: EditPostRepository) {
        if (isStarted) return

        dispatcher.register(this)

        isStarted = true
        this.siteModel = siteModel
        this.editPostRepository = editPostRepository

        startJetpackSocial()
    }

    private fun startJetpackSocial() {
        if (shouldShowJetpackSocial()) {
            launch {
                shareLimit = getJetpackSocialShareLimitStatusUseCase.execute(siteModel)
                loadConnections()
                loadJetpackSocialIfSupported()
            }
        } else {
            _jetpackSocialContainerVisibility.value = JetpackSocialContainerVisibility.ALL_HIDDEN
        }
    }

    fun onResume(jetpackSocialFlow: JetpackSocialFlow) {
        _jetpackSocialUiState.value?.let { uiState ->
            if (uiState is JetpackSocialUiState.Loaded && uiState.showShareLimitUi) {
                jetpackSocialSharingTracker.trackShareLimitDisplayed(jetpackSocialFlow)
            } else if (uiState is JetpackSocialUiState.NoConnections) {
                trackAddConnectionCtaDisplayedIfVisible(jetpackSocialFlow)
            }
        }
        if (!isLastActionOnResumeHandled) {
            isLastActionOnResumeHandled = true
            when (actionEvents.value) {
                is ActionEvent.OpenSocialConnectionsList -> {
                    // When getting back from publicize connections screen, we should update connections to
                    // make sure we have the latest data.
                    launch {
                        updateConnections()
                        loadJetpackSocialIfSupported()
                    }
                }

                is ActionEvent.OpenSubscribeJetpackSocial -> {
                    // We have to update SiteModel to make sure we have the latest data regarding user hired plans
                    dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(siteModel))
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    private suspend fun loadConnections() {
        val publicizeConnections = getPublicizeConnectionsForUserUseCase.execute(
            siteId = siteModel.siteId,
            userId = accountStore.account.userId
        )
        connections.clear()
        connections.addAll(publicizeConnections.map { connection ->
            PostSocialConnection.fromPublicizeConnection(connection, false)
        })
        updateInitialConnectionListSharingStatus()
    }

    private suspend fun updateConnections() {
        val publicizeConnections = getPublicizeConnectionsForUserUseCase.execute(
            siteId = siteModel.siteId,
            userId = accountStore.account.userId,
            shouldForceUpdate = false,
        )
        // Update connections list with new connections
        val currentConnectionIds = connections.map { it.connectionId }
        publicizeConnections.forEach { publicizeConnection ->
            if (currentConnectionIds.doesNotContain(publicizeConnection.connectionId)) {
                connections.add(PostSocialConnection.fromPublicizeConnection(publicizeConnection, false))
            }
        }
        // Update connections list with removed connections
        currentConnectionIds.forEach { currentConnectionId ->
            if (publicizeConnections.map { connection -> connection.connectionId }
                    .doesNotContain(currentConnectionId)) {
                connections.removeAll { connection -> connection.connectionId == currentConnectionId }
            }
        }
    }

    private suspend fun loadJetpackSocialIfSupported() {
        if (!shouldShowJetpackSocial()) {
            _jetpackSocialContainerVisibility.postValue(JetpackSocialContainerVisibility.ALL_HIDDEN)
            return
        }

        if (showNoConnections()) {
            // If user previously dismissed the no connections container by tapping the "Not now" button,
            // we should hide the container.
            _jetpackSocialContainerVisibility.postValue(getJetpackSocialContainerVisibilityFromPrefs())

            _jetpackSocialUiState.postValue(
                jetpackUiStateMapper.mapNoConnections(
                    ::onJetpackSocialConnectProfilesClick,
                    ::onJetpackSocialNotNowClick
                )
            )
        } else {
            _jetpackSocialContainerVisibility.postValue(JetpackSocialContainerVisibility.ALL_VISIBLE)
            _jetpackSocialUiState.postValue(mapLoaded())
        }
    }

    private fun shouldShowJetpackSocial() = ::editPostRepository.isInitialized
            && !editPostRepository.isPage
            && siteModel.supportsPublicize()
            && currentPost?.status != PostStatus.PRIVATE.toString()

    private suspend fun mapLoaded(): JetpackSocialUiState.Loaded {
        val shareMessage = jetpackSocialShareMessage.ifEmpty {
            getJetpackSocialShareMessageUseCase.execute(currentPost?.id ?: -1)
        }
        val socialSharingModel = postSocialSharingModelMapper.map(connections, shareLimit)
        return jetpackUiStateMapper.mapLoaded(
            connections = connections,
            shareLimit = shareLimit,
            socialSharingModel = socialSharingModel,
            onSubscribeClick = ::onJetpackSocialSubscribeClick,
            shareMessage = shareMessage,
            onShareMessageClick = ::onJetpackSocialMessageClick,
            onConnectionClick = ::onJetpackSocialConnectionClick,
            isPostPublished = isPostPublished(),
        )
    }

    private fun showNoConnections(): Boolean =
        connections.isEmpty()
                && publicizeTableWrapper.getServiceList().any { it.status != PublicizeService.Status.UNSUPPORTED }

    private fun updateInitialConnectionListSharingStatus() {
        if (isPostPublished()) {
            connections.map { it.isSharingEnabled = false }
            return
        }
        val shareLimitValue = shareLimit
        val skipConnections = postPublicizeSkipConnections()
        if (shareLimitValue is ShareLimit.Enabled) {
            // If shares remaining < number of connections, all connections are unchecked by default
            updateInitialConnectionListShareLimitEnabled(shareLimitValue, skipConnections)
        } else {
            updateInitialConnectionListShareLimitDisabled(skipConnections)
        }
    }

    private fun updateInitialConnectionListShareLimitEnabled(
        shareLimitValue: ShareLimit.Enabled,
        skipConnections: List<PublicizeSkipConnection>
    ) {
        if (shareLimitValue.sharesRemaining < connections.size) {
            connections.map { it.isSharingEnabled = false }
        } else {
            connections.take(shareLimitValue.sharesRemaining).map {
                it.isSharingEnabled = true
            }
            connections.forEach { connection ->
                // Use metadata to verify if this connection was previously disabled by the user
                skipConnections.firstOrNull { it.connectionId() == connection.connectionId.toString() }
                    ?.let { connection.isSharingEnabled = it.isConnectionEnabled() }
            }
        }
    }

    private fun updateInitialConnectionListShareLimitDisabled(skipConnections: List<PublicizeSkipConnection>) {
        // With share limit disabled we can enable all connections but still considering the skip
        // connections metadata
        connections.map { it.isSharingEnabled = true }
        connections.forEach { connection ->
            // Use metadata to verify if this connection was previously disabled by the user
            skipConnections.firstOrNull { it.connectionId() == connection.connectionId.toString() }
                ?.let { connection.isSharingEnabled = it.isConnectionEnabled() }
        }
    }

    private fun getJetpackSocialContainerVisibilityFromPrefs(): JetpackSocialContainerVisibility {
        return JetpackSocialContainerVisibility(
            showInPrepublishingSheet = appPrefsWrapper.getShouldShowJetpackSocialNoConnections(
                siteModel.siteId,
                JetpackSocialFlow.PRE_PUBLISHING
            ),
            showInPostSettings = appPrefsWrapper.getShouldShowJetpackSocialNoConnections(
                siteModel.siteId,
                JetpackSocialFlow.POST_SETTINGS
            )
        )
    }

    private fun postPublicizeSkipConnections(): List<PublicizeSkipConnection> =
        currentPost?.publicizeSkipConnectionsList ?: emptyList()

    private fun isPostPublished(): Boolean =
        currentPost?.status?.let { status ->
            status == PostStatus.PUBLISHED.toString()
        } ?: false

    @VisibleForTesting
    fun onJetpackSocialConnectProfilesClick(jetpackSocialFlow: JetpackSocialFlow) {
        isLastActionOnResumeHandled = false
        jetpackSocialSharingTracker.trackAddConnectionTapped(jetpackSocialFlow)
        _actionEvents.value = ActionEvent.OpenSocialConnectionsList(siteModel = siteModel)
    }

    @VisibleForTesting
    fun onJetpackSocialNotNowClick(jetpackSocialFlow: JetpackSocialFlow) {
        appPrefsWrapper.setShouldShowJetpackSocialNoConnections(false, siteModel.siteId, jetpackSocialFlow)
        jetpackSocialSharingTracker.trackAddConnectionDismissCtaTapped(jetpackSocialFlow)
        _jetpackSocialContainerVisibility.value = getJetpackSocialContainerVisibilityFromPrefs()
    }

    @VisibleForTesting
    fun onJetpackSocialConnectionClick(
        connection: PostSocialConnection,
        enabled: Boolean,
        jetpackSocialFlow: JetpackSocialFlow
    ) {
        jetpackSocialSharingTracker.trackConnectionToggled(jetpackSocialFlow, enabled)
        launch {
            // Update UI
            connections.firstOrNull { it.connectionId == connection.connectionId }?.let {
                it.isSharingEnabled = enabled
            }
            _jetpackSocialUiState.postValue(mapLoaded())
            // Update local post
            editPostRepository.updateAsync({ postModel ->
                val connectionId = connection.connectionId.toString()
                with(postModel.publicizeSkipConnectionsList.toMutableList()) {
                    firstOrNull { it.connectionId() == connectionId }?.updateValue(enabled)
                        ?: run {
                            // Connection wasn't part of skip connections before, so we must add it
                            // with the correct value
                            add(PublicizeSkipConnection.createNew(connectionId, enabled))
                        }
                    postModel.updatePublicizeSkipConnections(this)
                }
                true
            })
        }
    }

    private fun onJetpackSocialMessageClick() {
        val shareMessage = with(_jetpackSocialUiState.value) {
            if (this is JetpackSocialUiState.Loaded) {
                shareMessage
            } else ""
        }
        _actionEvents.value = ActionEvent.OpenEditShareMessage(shareMessage)
    }

    @VisibleForTesting
    fun onJetpackSocialSubscribeClick(jetpackSocialFlow: JetpackSocialFlow) {
        isLastActionOnResumeHandled = false
        jetpackSocialSharingTracker.trackUpgradeLinkTapped(jetpackSocialFlow)
        _actionEvents.value = ActionEvent.OpenSubscribeJetpackSocial(
            siteModel = siteModel,
            url = HIRE_JETPACK_SOCIAL_BASIC_URL.format(
                siteModel.url.replace(Regex("^(https?://)", RegexOption.IGNORE_CASE), "")
            ),
        )
    }

    fun onJetpackSocialShareMessageChanged(newShareMessage: String?) {
        val currentState = _jetpackSocialUiState.value
        if (newShareMessage != null && currentState is JetpackSocialUiState.Loaded) {
            val shareMessage = newShareMessage.ifEmpty { currentPost?.title ?: "" }
            editPostRepository.updateAsync({ postModel ->
                postModel.setAutoShareMessage(shareMessage)
                true
            })
            _jetpackSocialUiState.value = currentState.copy(shareMessage = shareMessage)
            jetpackSocialShareMessage = shareMessage
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            return
        }
        launch {
            val updatedSite = siteStore.getSiteByLocalId(siteModel.id)
            if (updatedSite != null) {
                siteModel = updatedSite
                shareLimit = getJetpackSocialShareLimitStatusUseCase.execute(siteModel)
                loadJetpackSocialIfSupported()
            }
        }
    }

    private fun trackAddConnectionCtaDisplayedIfVisible(jetpackSocialFlow: JetpackSocialFlow) {
        val trackEvent = when (jetpackSocialFlow) {
            JetpackSocialFlow.PRE_PUBLISHING ->
                jetpackSocialContainerVisibility.value?.showInPrepublishingSheet == true
            JetpackSocialFlow.POST_SETTINGS ->
                _jetpackSocialContainerVisibility.value?.showInPostSettings == true
            else -> false
        }

        if (trackEvent) jetpackSocialSharingTracker.trackAddConnectionCtaDisplayed(jetpackSocialFlow)
    }

    fun onPostStatusChanged() {
        startJetpackSocial()
    }

    data class JetpackSocialContainerVisibility(
        val showInPrepublishingSheet: Boolean,
        val showInPostSettings: Boolean,
    ) {
        companion object {
            @VisibleForTesting
            val ALL_HIDDEN = JetpackSocialContainerVisibility(
                showInPrepublishingSheet = false,
                showInPostSettings = false,
            )

            @VisibleForTesting
            val ALL_VISIBLE = JetpackSocialContainerVisibility(
                showInPrepublishingSheet = true,
                showInPostSettings = true,
            )
        }
    }

    sealed class JetpackSocialUiState {
        object Loading : JetpackSocialUiState()

        data class Loaded(
            val jetpackSocialConnectionDataList: List<JetpackSocialConnectionData>,
            val socialSharingModel: PostSocialSharingModel,
            val showShareLimitUi: Boolean,
            val isShareMessageEnabled: Boolean,
            val shareMessage: String,
            val onShareMessageClick: () -> Unit,
            val subscribeButtonLabel: String,
            val onSubscribeClick: (JetpackSocialFlow) -> Unit,
        ) : JetpackSocialUiState()

        data class NoConnections(
            val trainOfIconsModels: List<TrainOfIconsModel>,
            val message: String,
            val connectProfilesButtonLabel: String,
            val onConnectProfilesClick: (JetpackSocialFlow) -> Unit,
            val notNowButtonLabel: String,
            val onNotNowClick: (JetpackSocialFlow) -> Unit,
        ) : JetpackSocialUiState()
    }

    sealed class ActionEvent {
        data class OpenEditShareMessage(val shareMessage: String) : ActionEvent()

        data class OpenSocialConnectionsList(val siteModel: SiteModel) : ActionEvent()

        data class OpenSubscribeJetpackSocial(
            val siteModel: SiteModel,
            val url: String,
        ) : ActionEvent()
    }

    companion object {
        @VisibleForTesting
        const val HIRE_JETPACK_SOCIAL_BASIC_URL =
            "https://wordpress.com/checkout/%s/jetpack_social_basic_yearly"
    }
}
