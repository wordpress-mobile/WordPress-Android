package org.wordpress.android.ui.posts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PublicizeSkipConnection
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
import org.wordpress.android.util.extensions.doesNotContain
import org.wordpress.android.util.extensions.updatePublicizeSkipConnections
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class EditorJetpackSocialViewModel @Inject constructor(
    private val jetpackSocialFeatureConfig: JetpackSocialFeatureConfig,
    private val accountStore: AccountStore,
    private val getPublicizeConnectionsForUserUseCase: GetPublicizeConnectionsForUserUseCase,
    private val getJetpackSocialShareMessageUseCase: GetJetpackSocialShareMessageUseCase,
    private val getJetpackSocialShareLimitStatusUseCase: GetJetpackSocialShareLimitStatusUseCase,
    private val jetpackUiStateMapper: EditPostPublishSettingsJetpackSocialUiStateMapper,
    private val postSocialSharingModelMapper: PostSocialSharingModelMapper,
    private val publicizeTableWrapper: PublicizeTableWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private lateinit var siteModel: SiteModel
    private lateinit var editPostRepository: EditPostRepository

    // TODO thomashorta clean up and extract common logic from these live data and functions, so they can be reused in
    //  different places in the Editor (and maybe even in the dashboard card)
    private val _jetpackSocialContainerVisibility = MutableLiveData<JetpackSocialContainerVisibility>()
    val jetpackSocialContainerVisibility: LiveData<JetpackSocialContainerVisibility> = _jetpackSocialContainerVisibility

    private val _jetpackSocialUiState = MutableLiveData<JetpackSocialUiState>()
    val jetpackSocialUiState: LiveData<JetpackSocialUiState> = _jetpackSocialUiState

    private val _postSocialSharingModel = MutableLiveData<PostSocialSharingModel>()
    val postSocialSharingModel: LiveData<PostSocialSharingModel> = _postSocialSharingModel

    // this is a SingleLiveEvent so it should have one, and only one, observer (the Activity)
    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private var jetpackSocialShareMessage = ""
    private lateinit var shareLimit: ShareLimit
    private val connections = mutableListOf<PostSocialConnection>()

    private val currentPost: PostImmutableModel?
        get() = editPostRepository.getPost()

    fun start(siteModel: SiteModel, editPostRepository: EditPostRepository) {
        this.siteModel = siteModel
        this.editPostRepository = editPostRepository

        if (jetpackSocialFeatureConfig.isEnabled()) {
            launch {
                shareLimit = getJetpackSocialShareLimitStatusUseCase.execute(siteModel)
                loadConnections()
                loadJetpackSocialIfSupported()
            }
        } else {
            _jetpackSocialContainerVisibility.value = JetpackSocialContainerVisibility.ALL_HIDDEN
        }
    }

    fun onResume() {
        if (jetpackSocialFeatureConfig.isEnabled() && actionEvents.value is ActionEvent.OpenSocialConnectionsList) {
            // When getting back from publicize connections screen, we should update connections to
            // make sure we have the latest data.
            launch {
                updateConnections()
                loadJetpackSocialIfSupported()
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
        _postSocialSharingModel.postValue(postSocialSharingModelMapper.map(connections, shareLimit))
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
        val showJetpackSocial = jetpackSocialFeatureConfig.isEnabled() && siteModel.supportsPublicize()
        if (!showJetpackSocial) {
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

    private suspend fun mapLoaded(): JetpackSocialUiState.Loaded {
        val shareMessage = jetpackSocialShareMessage.ifEmpty {
            getJetpackSocialShareMessageUseCase.execute(currentPost?.id ?: -1)
        }
        return jetpackUiStateMapper.mapLoaded(
            connections = connections,
            shareLimit = shareLimit,
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
                siteModel.id,
                JetpackSocialFlow.PRE_PUBLISHING
            ),
            showInPostSettings = appPrefsWrapper.getShouldShowJetpackSocialNoConnections(
                siteModel.id,
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
    fun onJetpackSocialConnectProfilesClick() {
        _actionEvents.value = ActionEvent.OpenSocialConnectionsList(siteModel = siteModel)
    }

    @VisibleForTesting
    fun onJetpackSocialNotNowClick(jetpackSocialFlow: JetpackSocialFlow) {
        appPrefsWrapper.setShouldShowJetpackSocialNoConnections(false, siteModel.id, jetpackSocialFlow)
        _jetpackSocialContainerVisibility.value = getJetpackSocialContainerVisibilityFromPrefs()
    }

    @VisibleForTesting
    fun onJetpackSocialConnectionClick(connection: PostSocialConnection, enabled: Boolean) {
        launch {
            // Update UI
            connections.firstOrNull { it.connectionId == connection.connectionId }?.let {
                it.isSharingEnabled = enabled
            }
            _jetpackSocialUiState.postValue(mapLoaded())
            _postSocialSharingModel.postValue(postSocialSharingModelMapper.map(connections, shareLimit))
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

    private fun onJetpackSocialSubscribeClick() {
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
            val showShareLimitUi: Boolean,
            val isShareMessageEnabled: Boolean,
            val shareMessage: String,
            val onShareMessageClick: () -> Unit,
            val subscribeButtonLabel: String,
            val onSubscribeClick: () -> Unit,
        ) : JetpackSocialUiState()

        data class NoConnections(
            val trainOfIconsModels: List<TrainOfIconsModel>,
            val message: String,
            val connectProfilesButtonLabel: String,
            val onConnectProfilesClick: () -> Unit,
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
