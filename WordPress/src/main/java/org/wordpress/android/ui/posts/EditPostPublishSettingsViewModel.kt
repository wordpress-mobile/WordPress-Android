package org.wordpress.android.ui.posts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.fluxc.model.PublicizeSkipConnection
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.Person
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.ui.people.utils.PeopleUtilsWrapper
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.ui.posts.social.PostSocialSharingModelMapper
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingModel
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.usecase.social.ShareLimit
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
import org.wordpress.android.util.extensions.doesNotContain
import org.wordpress.android.util.extensions.updatePublicizeSkipConnections
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class EditPostPublishSettingsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    postSettingsUtils: PostSettingsUtils,
    private val peopleUtilsWrapper: PeopleUtilsWrapper,
    localeManagerWrapper: LocaleManagerWrapper,
    postSchedulingNotificationStore: PostSchedulingNotificationStore,
    private val siteStore: SiteStore,
    private val jetpackSocialFeatureConfig: JetpackSocialFeatureConfig,
    private val accountStore: AccountStore,
    private val getPublicizeConnectionsForUserUseCase: GetPublicizeConnectionsForUserUseCase,
    private val getJetpackSocialShareMessageUseCase: GetJetpackSocialShareMessageUseCase,
    private val getJetpackSocialShareLimitStatusUseCase: GetJetpackSocialShareLimitStatusUseCase,
    private val jetpackUiStateMapper: EditPostPublishSettingsJetpackSocialUiStateMapper,
    private val postSocialSharingModelMapper: PostSocialSharingModelMapper,
    private val publicizeTableWrapper: PublicizeTableWrapper,
) : PublishSettingsViewModel(
    resourceProvider,
    postSettingsUtils,
    localeManagerWrapper,
    postSchedulingNotificationStore,
    siteStore
) {
    private val _authors = MutableLiveData<List<Person>>()
    val authors: LiveData<List<Person>> = _authors

    // Used for combining fetched users
    private val fetchedAuthors = mutableListOf<Person>()

    private val _showJetpackSocialContainer = MutableLiveData<Boolean>()
    val showJetpackSocialContainer: LiveData<Boolean> = _showJetpackSocialContainer

    private val _jetpackSocialUiState = MutableLiveData<JetpackSocialUiState>()
    val jetpackSocialUiState: LiveData<JetpackSocialUiState> = _jetpackSocialUiState

    private val _postSocialSharingModel = MutableLiveData<PostSocialSharingModel>()
    val postSocialSharingModel: LiveData<PostSocialSharingModel> = _postSocialSharingModel

    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private var jetpackSocialShareMessage = ""
    private var siteModel: SiteModel? = null
    private lateinit var shareLimit: ShareLimit
    private val connections = mutableListOf<PostSocialConnection>()

    private var isStarted = false

    override fun start(postRepository: EditPostRepository?) {
        super.start(postRepository)
        if (isStarted) return
        isStarted = true

        siteModel = postRepository?.localSiteId?.let {
            siteStore.getSiteByLocalId(it)
        }
        loadAuthors()

        if (jetpackSocialFeatureConfig.isEnabled()) {
            viewModelScope.launch {
                shareLimit = siteModel?.let {
                    getJetpackSocialShareLimitStatusUseCase.execute(it)
                } ?: ShareLimit.Disabled
                loadConnections()
                loadJetpackSocialIfSupported()
            }
        } else {
            _showJetpackSocialContainer.value = false
        }
    }

    fun onResume() {
        if (jetpackSocialFeatureConfig.isEnabled() && actionEvents.value is ActionEvent.OpenSocialConnectionsList) {
            // When getting back from publicize connections screen, we should update connections to
            // make sure we have the latest data.
            viewModelScope.launch {
                updateConnections()
                loadJetpackSocialIfSupported()
            }
        }
    }

    private suspend fun loadConnections() {
        siteModel?.let {
            val publicizeConnections = getPublicizeConnectionsForUserUseCase.execute(
                siteId = it.siteId,
                userId = accountStore.account.userId
            )
            connections.clear()
            connections.addAll(publicizeConnections.map { connection ->
                PostSocialConnection.fromPublicizeConnection(connection, false)
            })
            updateInitialConnectionListSharingStatus()
            _postSocialSharingModel.value =
                postSocialSharingModelMapper.map(connections, shareLimit)
        }
    }

    private suspend fun updateConnections() {
        siteModel?.let {
            val publicizeConnections = getPublicizeConnectionsForUserUseCase.execute(
                siteId = it.siteId,
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
    }

    private suspend fun loadJetpackSocialIfSupported() {
        val showJetpackSocial = jetpackSocialFeatureConfig.isEnabled() && siteModel?.supportsPublicize() == true
        if (!showJetpackSocial) {
            _showJetpackSocialContainer.value = false
            return
        }
        siteModel?.let {
            _showJetpackSocialContainer.value = true
            val state = if (showNoConnections()) {
                jetpackUiStateMapper.mapNoConnections(::onJetpackSocialConnectProfilesClick)
            } else {
                mapLoaded()
            }
            _jetpackSocialUiState.postValue(state)
        } ?: run {
            _showJetpackSocialContainer.value = false
        }
    }

    private suspend fun mapLoaded(): JetpackSocialUiState.Loaded {
        val shareMessage = jetpackSocialShareMessage.ifEmpty {
            getJetpackSocialShareMessageUseCase.execute(editPostRepository?.getPost()?.id ?: -1)
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

    private fun loadAuthors() {
        siteModel?.let {
            if (it.hasCapabilityListUsers) {
                fetchAuthors(it)
            }
        }
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
            if (shareLimitValue.sharesRemaining < connections.size) {
                connections.map { it.isSharingEnabled = false }
            } else {
                run loop@{
                    connections.take(shareLimitValue.sharesRemaining).map {
                        it.isSharingEnabled = true
                    }
                    connections.forEachIndexed { index, connection ->
                        // Use metadata to verify if this connection was previously disabled by the user
                        skipConnections.firstOrNull { it.connectionId() == connection.connectionId.toString() }
                            ?.let { connection.isSharingEnabled = it.isConnectionEnabled() }
                    }
                }
            }
        } else {
            // With share limit disabled we can enable all connections but still considering the skip
            // connections metadata
            connections.map { it.isSharingEnabled = true }
            connections.forEach { connection ->
                // Use metadata to verify if this connection was previously disabled by the user
                skipConnections.firstOrNull { it.connectionId() == connection.connectionId.toString() }
                    ?.let { connection.isSharingEnabled = it.isConnectionEnabled() }
            }
        }
    }

    private fun postPublicizeSkipConnections(): List<PublicizeSkipConnection> =
        editPostRepository?.getPost()?.publicizeSkipConnectionsList ?: emptyList()

    private fun isPostPublished(): Boolean =
        editPostRepository?.getPost()?.status?.let { status ->
            status == PostStatus.PUBLISHED.toString()
        } ?: false

    // This fetches authors page by page and combine the result in fetchedAuthors.
    private fun fetchAuthors(site: SiteModel) {
        peopleUtilsWrapper.fetchAuthors(site, fetchedAuthors.size, object : FetchUsersCallback {
            override fun onSuccess(peopleList: List<Person>, isEndOfList: Boolean) {
                fetchedAuthors.addAll(peopleList)
                if (isEndOfList) {
                    _authors.value = fetchedAuthors
                } else {
                    fetchAuthors(site)
                }
            }

            override fun onError() {
                _onToast.postValue(Event(resourceProvider.getString(R.string.error_fetch_authors_list)))
            }
        })
    }

    fun getAuthorIndex(authorId: Long) = authors.value?.indexOfFirst { it.personID == authorId } ?: -1

    @VisibleForTesting
    fun onJetpackSocialConnectProfilesClick() {
        siteModel?.let { siteModel ->
            _actionEvents.value = ActionEvent.OpenSocialConnectionsList(
                siteModel = siteModel
            )
        }
    }

    @VisibleForTesting
    fun onJetpackSocialConnectionClick(connection: PostSocialConnection, enabled: Boolean) {
        siteModel?.let {
            viewModelScope.launch {
                // Update UI
                connections.firstOrNull { it.connectionId == connection.connectionId }?.let {
                    it.isSharingEnabled = enabled
                }
                _jetpackSocialUiState.postValue(mapLoaded())
                _postSocialSharingModel.value =
                    postSocialSharingModelMapper.map(connections, shareLimit)
                // Update local post
                editPostRepository?.updateAsync({ postModel ->
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
        siteModel?.let { siteModel ->
            _actionEvents.value = ActionEvent.OpenSubscribeJetpackSocial(
                siteModel = siteModel,
                url = HIRE_JETPACK_SOCIAL_BASIC_URL.replace(
                    oldValue = HIRE_JETPACK_SOCIAL_BASIC_SITE_PLACEHOLDER,
                    newValue = siteModel.url.replace(Regex("^(http[s]?://)", RegexOption.IGNORE_CASE), "")
                ),
            )
        }
    }

    fun onJetpackSocialShareMessageChanged(newShareMessage: String?) {
        val currentState = _jetpackSocialUiState.value
        if (newShareMessage != null && currentState is JetpackSocialUiState.Loaded) {
            val shareMessage = newShareMessage.ifEmpty { editPostRepository?.getPost()?.title ?: "" }
            editPostRepository?.updateAsync({ postModel ->
                postModel.setAutoShareMessage(shareMessage)
                true
            })
            _jetpackSocialUiState.value = currentState.copy(
                shareMessage = shareMessage
            )
            jetpackSocialShareMessage = shareMessage
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
}

private const val HIRE_JETPACK_SOCIAL_BASIC_SITE_PLACEHOLDER = "{site}"

@VisibleForTesting
const val HIRE_JETPACK_SOCIAL_BASIC_URL =
    "https://wordpress.com/checkout/$HIRE_JETPACK_SOCIAL_BASIC_SITE_PLACEHOLDER/jetpack_social_basic_yearly"
