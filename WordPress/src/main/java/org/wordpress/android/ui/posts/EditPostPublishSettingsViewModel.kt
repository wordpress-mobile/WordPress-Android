package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.Person
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.ui.people.utils.PeopleUtilsWrapper
import org.wordpress.android.ui.posts.social.PostSocialConnection
import org.wordpress.android.usecase.social.GetJetpackSocialShareLimitStatusUseCase
import org.wordpress.android.usecase.social.GetJetpackSocialShareMessageUseCase
import org.wordpress.android.usecase.social.GetPublicizeConnectionsForUserUseCase
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.config.JetpackSocialFeatureConfig
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
) : PublishSettingsViewModel(
    resourceProvider,
    postSettingsUtils,
    localeManagerWrapper,
    postSchedulingNotificationStore,
    siteStore
) {
    private var postModel: PostImmutableModel? = null

    private var editPostRepository: EditPostRepository? = null

    private val _authors = MutableLiveData<List<Person>>()
    val authors: LiveData<List<Person>> = _authors

    // Used for combining fetched users
    private val fetchedAuthors = mutableListOf<Person>()

    private val _showJetpackSocialContainer = MutableLiveData<Boolean>()
    val showJetpackSocialContainer: LiveData<Boolean> = _showJetpackSocialContainer

    private val _jetpackSocialUiState = MutableLiveData<JetpackSocialUiState>()
    val jetpackSocialUiState: LiveData<JetpackSocialUiState> = _jetpackSocialUiState

    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private var jetpackSocialShareMessage = ""

    private var isStarted = false

    override fun start(postRepository: EditPostRepository?) {
        super.start(postRepository)
        if (isStarted) return
        isStarted = true

        val siteModel = postRepository?.localSiteId?.let {
            siteStore.getSiteByLocalId(it)
        }
        postModel = postRepository?.getPost()
        loadAuthors(siteModel)
        loadJetpackSocial(siteModel)

        editPostRepository = postRepository
    }

    private fun loadAuthors(siteModel: SiteModel?) {
        siteModel?.let {
            if (it.hasCapabilityListUsers) {
                fetchAuthors(it)
            }
        }
    }

    private fun loadJetpackSocial(siteModel: SiteModel?) {
        if (!jetpackSocialFeatureConfig.isEnabled()) {
            _showJetpackSocialContainer.value = false
            return
        }
        siteModel?.let {
            _showJetpackSocialContainer.value = true
            viewModelScope.launch {
                val connections = getPublicizeConnectionsForUserUseCase.execute(it.siteId, accountStore.account.userId)
                val shareMessage = jetpackSocialShareMessage.ifEmpty {
                    getJetpackSocialShareMessageUseCase.execute(postModel?.id ?: -1)
                }
                val shareLimit = getJetpackSocialShareLimitStatusUseCase.execute(it)
                val state = if (connections.isEmpty()) {
                    jetpackUiStateMapper.mapNoConnections(::onJetpackSocialConnectProfilesClick)
                } else {
                    jetpackUiStateMapper.mapLoaded(
                        connections = connections,
                        shareLimit = shareLimit,
                        onSubscribeClick = ::onJetpackSocialSubscribeClick,
                        shareMessage = shareMessage,
                        shareMessageClick = ::onJetpackSocialMessageClick
                    )
                }
                _jetpackSocialUiState.postValue(state)
            }
        } ?: run {
            _showJetpackSocialContainer.value = false
        }
    }

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

    private fun onJetpackSocialConnectProfilesClick() {
        // TODO
    }

    fun onJetpackSocialConnectionClick() {
        // TODO
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
        // TODO
    }

    fun onJetpackSocialShareMessageChanged(newShareMessage: String?) {
        val currentState = _jetpackSocialUiState.value
        if (newShareMessage != null && currentState is JetpackSocialUiState.Loaded) {
            val shareMessage = newShareMessage.ifEmpty { postModel?.title ?: "" }
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
            val postSocialConnectionList: List<PostSocialConnection>,
            val showShareLimitUi: Boolean,
            val shareMessage: String,
            val onShareMessageClick: () -> Unit,
            val remainingSharesMessage: String,
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
    }
}
