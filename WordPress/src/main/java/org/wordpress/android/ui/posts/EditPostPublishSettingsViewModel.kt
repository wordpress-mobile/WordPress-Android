package org.wordpress.android.ui.posts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
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
    private var siteModel: SiteModel? = null

    private val selectedConnections = mutableListOf<PostSocialConnection>()

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

    private var isStarted = false

    override fun start(postRepository: EditPostRepository?) {
        super.start(postRepository)
        if (isStarted) return
        isStarted = true

        siteModel = postRepository?.localSiteId?.let {
            siteStore.getSiteByLocalId(it)
        }
        loadAuthors()
        loadJetpackSocial()
    }

    fun onScreenShown() {
        if (actionEvents.value is ActionEvent.OpenSocialConnectionsList) {
            loadJetpackSocial()
        }
    }

    private fun loadAuthors() {
        siteModel?.let {
            if (it.hasCapabilityListUsers) {
                fetchAuthors(it)
            }
        }
    }

    private fun loadJetpackSocial() {
        if (!jetpackSocialFeatureConfig.isEnabled() || siteModel?.supportsPublicize() != true) {
            _showJetpackSocialContainer.value = false
            return
        }
        siteModel?.let {
            _showJetpackSocialContainer.value = true
            viewModelScope.launch {
                val allConnections =
                    PostSocialConnection.fromPublicizeConnectionList(
                        getPublicizeConnectionsForUserUseCase.execute(
                            it.siteId,
                            accountStore.account.userId
                        )
                    )
                val state = if (showNoConnections(allConnections)) {
                    jetpackUiStateMapper.mapNoConnections(::onJetpackSocialConnectProfilesClick)
                } else {
                    mapLoaded(it, allConnections)
                }
                _jetpackSocialUiState.postValue(state)
            }
        } ?: run {
            _showJetpackSocialContainer.value = false
        }
    }

    private fun showNoConnections(allConnections: List<PostSocialConnection>): Boolean =
        allConnections.isEmpty() && (siteModel?.supportsPublicize() ?: false)
                && publicizeTableWrapper.getServiceList().any { it.status != PublicizeService.Status.UNSUPPORTED }

    private suspend fun mapLoaded(
        it: SiteModel,
        allConnections: List<PostSocialConnection>
    ): JetpackSocialUiState.Loaded {
        val shareMessage = jetpackSocialShareMessage.ifEmpty {
            getJetpackSocialShareMessageUseCase.execute(editPostRepository?.getPost()?.id ?: -1)
        }
        val shareLimit = getJetpackSocialShareLimitStatusUseCase.execute(it)
        allConnections.map {
            if (shareLimit is ShareLimit.Enabled) {
                allConnections.mapIndexed { index, connection ->
                    for (i in 0..shareLimit.sharesRemaining) {
                        selectedConnections.add(connection)
                        if (i == index) {
                            break
                        }
                    }
                }
            }
        }
        _postSocialSharingModel.value =
            postSocialSharingModelMapper.map(allConnections, selectedConnections, shareLimit)
        return jetpackUiStateMapper.mapLoaded(
            allConnections = allConnections,
            selectedConnections = selectedConnections,
            shareLimit = shareLimit,
            onSubscribeClick = ::onJetpackSocialSubscribeClick,
            shareMessage = shareMessage,
            onShareMessageClick = ::onJetpackSocialMessageClick,
            onConnectionClick = ::onJetpackSocialConnectionClick,
            isPostPublished = editPostRepository?.getPost()?.status?.let {
                it == PostStatus.PUBLISHED.toString()
            } ?: false
        )
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

    @VisibleForTesting
    fun onJetpackSocialConnectProfilesClick() {
        siteModel?.let { siteModel ->
            _actionEvents.value = ActionEvent.OpenSocialConnectionsList(
                siteModel = siteModel
            )
        }
    }

    @VisibleForTesting
    fun onJetpackSocialConnectionClick(publicizeSocialConnection: PostSocialConnection, enabled: Boolean) {
        // TODO update selected connections using post metadata
        siteModel?.let {
            if (enabled) {
                selectedConnections.add(publicizeSocialConnection)
            } else {
                selectedConnections.removeAll {
                    it.service == publicizeSocialConnection.service
                }
            }
            viewModelScope.launch {
                val allConnections = PostSocialConnection.fromPublicizeConnectionList(
                    getPublicizeConnectionsForUserUseCase.execute(
                        siteId = it.siteId,
                        userId = accountStore.account.userId,
                        shouldForceUpdate = false,
                    )
                )
                val shareMessage = jetpackSocialShareMessage.ifEmpty {
                    getJetpackSocialShareMessageUseCase.execute(editPostRepository?.getPost()?.id ?: -1)
                }
                val shareLimit = getJetpackSocialShareLimitStatusUseCase.execute(it)
                _postSocialSharingModel.value =
                    postSocialSharingModelMapper.map(
                        allConnections, selectedConnections, shareLimit
                    )
                jetpackUiStateMapper.mapLoaded(
                    allConnections = allConnections,
                    selectedConnections = selectedConnections,
                    shareLimit = shareLimit,
                    onSubscribeClick = ::onJetpackSocialSubscribeClick,
                    shareMessage = shareMessage,
                    onShareMessageClick = ::onJetpackSocialMessageClick,
                    onConnectionClick = ::onJetpackSocialConnectionClick,
                    isPostPublished = editPostRepository?.getPost()?.status?.let {
                        it == PostStatus.PUBLISHED.toString()
                    } ?: false
                )
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
                    newValue = siteModel.url,
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
