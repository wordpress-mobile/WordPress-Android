package org.wordpress.android.ui.engagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewCommentInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewPostInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteById
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteByUrl
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.InitialLoading
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_COMMENT_LIKES
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_POST_LIKES
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class EngagedPeopleListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val getLikesHandler: GetLikesHandler,
    private val readerUtilsWrapper: ReaderUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private var getLikesJob: Job? = null

    private var listScenario: ListScenario? = null

    private val _onSnackbarMessage = MediatorLiveData<Event<SnackbarMessageHolder>>()
    private val _updateLikesState = MediatorLiveData<GetLikesState>()
    private val _onNavigationEvent = MutableLiveData<Event<EngagedListNavigationEvent>>()

    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage
    val uiState: LiveData<EngagedPeopleListUiState> = _updateLikesState.map {
        state -> buildUiState(state, listScenario)
    }
    val onNavigationEvent: LiveData<Event<EngagedListNavigationEvent>> = _onNavigationEvent

    data class EngagedPeopleListUiState(
        val showLikeFacesTrain: Boolean,
        val numLikes: Int = 0,
        val showLoading: Boolean,
        val engageItemsList: List<EngageItem>,
        val showEmptyState: Boolean,
        val emptyStateTitle: UiString? = null,
        val emptyStateAction: (() -> Unit)? = null,
        val emptyStateButtonText: UiString? = null
    )

    fun start(listScenario: ListScenario) {
        if (isStarted) return
        isStarted = true

        this.listScenario = listScenario

        _onSnackbarMessage.addSource(getLikesHandler.snackbarEvents) { event ->
            _onSnackbarMessage.value = event
        }

        _updateLikesState.addSource(getLikesHandler.likesStatusUpdate) { state ->
            _updateLikesState.value = state
        }

        onRefreshData()
    }

    private fun onRefreshData() {
        listScenario?.let {
            onLoadRequest(it.type, it.siteId, it.postOrCommentId)
        }
    }

    private fun onLoadRequest(
        loadRequestType: ListScenarioType,
        siteId: Long,
        entityId: Long
    ) {
        getLikesJob?.cancel()
        getLikesJob = launch(bgDispatcher) {
            // TODO: currently API is not sorting the likes as the list in notifications does,
            // use case logic has code to sort based on a list of ids (ideally the available likers ids taken
            // from the notification).
            // Keeping the logic for now, but remove empty listOf and relevant logic when API will sort likes
            when (loadRequestType) {
                LOAD_POST_LIKES -> getLikesHandler.handleGetLikesForPost(siteId, entityId)
                LOAD_COMMENT_LIKES -> getLikesHandler.handleGetLikesForComment(siteId, entityId)
            }
        }
    }

    private fun buildUiState(updateLikesState: GetLikesState?, listScenario: ListScenario?): EngagedPeopleListUiState {
        val likedItem = listScenario?.headerData?.let {
            listOf(
                    LikedItem(
                            author = it.authorName,
                            postOrCommentText = it.snippetText,
                            authorAvatarUrl = it.authorAvatarUrl,
                            likedItemId = listScenario.postOrCommentId,
                            likedItemSiteId = listScenario.siteId,
                            likedItemSiteUrl = listScenario.commentSiteUrl,
                            likedItemPostId = listScenario.commentPostId,
                            authorUserId = it.authorUserId,
                            authorPreferredSiteId = it.authorPreferredSiteId,
                            authorPreferredSiteUrl = it.authorPreferredSiteUrl,
                            onGravatarClick = ::onSiteLinkHolderClicked,
                            onHeaderClicked = ::onHeaderClicked
                    )
            )
        } ?: listOf()

        val likers = when (updateLikesState) {
            is LikesData -> {
                likesToEngagedPeople(updateLikesState.likes)
            }
            is Failure -> {
                likesToEngagedPeople(updateLikesState.cachedLikes)
            }
            InitialLoading, null -> listOf()
        }

        var showEmptyState = false
        var emptyStateTitle: UiString? = null
        var emptyStateAction: (() -> Unit)? = null

        if (updateLikesState is Failure) {
            updateLikesState.emptyStateData?.let {
                showEmptyState = it.showEmptyState
                emptyStateTitle = it.title
                emptyStateAction = ::onRefreshData
            }
        }

        return EngagedPeopleListUiState(
                showLikeFacesTrain = false,
                showLoading = updateLikesState is InitialLoading,
                engageItemsList = likedItem + likers,
                showEmptyState = showEmptyState,
                emptyStateTitle = emptyStateTitle,
                emptyStateAction = emptyStateAction,
                emptyStateButtonText = emptyStateAction?.let { UiStringRes(string.retry) }
        )
    }

    private fun likesToEngagedPeople(likes: List<LikeModel>): List<EngageItem> {
        return likes.map { likeData ->
            Liker(
                    name = likeData.likerName!!,
                    login = likeData.likerLogin!!,
                    userSiteId = likeData.likerSiteId,
                    userSiteUrl = likeData.likerSiteUrl!!,
                    userAvatarUrl = likeData.likerAvatarUrl!!,
                    remoteId = likeData.remoteLikeId,
                    onClick = ::onSiteLinkHolderClicked
            )
        }
    }

    private fun onSiteLinkHolderClicked(siteId: Long, siteUrl: String) {
        if (siteId == 0L && siteUrl.isNotEmpty()) {
            _onNavigationEvent.value = Event(PreviewSiteByUrl(siteUrl))
        } else if (siteId != 0L) {
            _onNavigationEvent.value = Event(PreviewSiteById(siteId))
        }
    }

    private fun onHeaderClicked(siteId: Long, siteUrl: String, postOrCommentId: Long, commentPostId: Long) {
        _onNavigationEvent.value = Event(
                if (commentPostId > 0) {
                    if (readerUtilsWrapper.postAndCommentExists(siteId, commentPostId, postOrCommentId)) {
                        PreviewCommentInReader(siteId, commentPostId, postOrCommentId)
                    } else {
                        PreviewSiteByUrl(siteUrl)
                    }
                } else {
                    PreviewPostInReader(siteId, postOrCommentId)
                }
        )
    }

    override fun onCleared() {
        super.onCleared()
        getLikesJob?.cancel()
        getLikesHandler.clear()
    }
}
