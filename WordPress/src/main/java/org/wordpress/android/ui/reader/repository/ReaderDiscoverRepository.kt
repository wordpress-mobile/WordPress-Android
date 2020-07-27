package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Failure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeSuccess
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeUnChanged
import org.wordpress.android.ui.reader.repository.usecases.FetchDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeActionUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FORCE
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReaderDiscoverRepository constructor(
    private val bgDispatcher: CoroutineDispatcher,
    private val readerTag: ReaderTag,
    private val getDiscoverCardsUseCase: GetDiscoverCardsUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase,
    private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler,
    private val postLikeActionUseCase: PostLikeActionUseCase
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private var isStarted = false

    private val _discoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>(
            onActive = { onActiveDiscoverFeed() }, onInactive = { onInactiveDiscoverFeed() })
    val discoverFeed: LiveData<ReaderDiscoverCards> = _discoverFeed

    private val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    fun start() {
        if (isStarted) return

        isStarted = true
        readerUpdatePostsEndedHandler.start(readerTag,
                ReaderUpdatePostsEndedHandler.setUpdatePostsEndedListeners(
                        this::onNewPosts,
                        this::onChangedPosts, this::onUnchanged, this::onFailed
                ))
    }

    fun stop() {
        job.cancel()
    }

    fun getTag(): ReaderTag = readerTag

    suspend fun refreshPosts() {
        withContext(bgDispatcher) {
            val response = fetchDiscoverCardsUseCase.fetch(REQUEST_FORCE)
            if (response != Success) _communicationChannel.postValue(Event(response))
        }
    }

    suspend fun performLikeAction(post: ReaderPost, isAskingToLike: Boolean, wpComUserId: Long) {
        withContext(bgDispatcher) {
            when (val event = postLikeActionUseCase.perform(post, isAskingToLike, wpComUserId)) {
                is PostLikeSuccess -> {
                    reloadPosts()
                }
                is PostLikeFailure -> {
                    _communicationChannel.postValue(Event(Failure(event)))
                    reloadPosts()
                }
                is PostLikeUnChanged -> { }
            }
        }
    }

    // Internal functionality
    private suspend fun loadPosts() {
        withContext(bgDispatcher) {
            val existsInMemory = discoverFeed.value?.cards?.isNotEmpty() ?: false
            val refresh = shouldAutoUpdateTagUseCase.get(readerTag)
            if (!existsInMemory) {
                val result = getDiscoverCardsUseCase.get()
                _discoverFeed.postValue(result)
            }
            if (refresh) {
                val response = fetchDiscoverCardsUseCase.fetch(REQUEST_FORCE)
                if (response != Success) _communicationChannel.postValue(Event(response))
            }
        }
    }

    private suspend fun reloadPosts() {
        withContext(bgDispatcher) {
            val result = getDiscoverCardsUseCase.get()
            _discoverFeed.postValue(result)
        }
    }

    // Handlers for ReaderPostServices
    private fun onNewPosts(event: UpdatePostsEnded) {
        launch {
            reloadPosts()
        }
    }

    private fun onChangedPosts(event: UpdatePostsEnded) {
        launch {
            reloadPosts()
        }
    }

    private fun onUnchanged(event: UpdatePostsEnded) {
    }

    private fun onFailed(event: UpdatePostsEnded) {
        _communicationChannel.postValue(
                Event(ReaderRepositoryCommunication.Error.RemoteRequestFailure))
    }

    // React to discoverFeed observers
    private fun onActiveDiscoverFeed() {
        launch {
            loadPosts()
        }
    }

    private fun onInactiveDiscoverFeed() {
    }

    class Factory
    @Inject constructor(
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val readerUtilsWrapper: ReaderUtilsWrapper,
        private val getDiscoverCardsUseCase: GetDiscoverCardsUseCase,
        private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
        private val fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase,
        private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler,
        private val postLikeActionUseCase: PostLikeActionUseCase
    ) {
        fun create(readerTag: ReaderTag? = null): ReaderDiscoverRepository {
            val tag = readerTag
                    ?: readerUtilsWrapper.getTagFromTagName(ReaderConstants.KEY_DISCOVER, DEFAULT)

            return ReaderDiscoverRepository(
                    bgDispatcher,
                    tag,
                    getDiscoverCardsUseCase,
                    shouldAutoUpdateTagUseCase,
                    fetchDiscoverCardsUseCase,
                    readerUpdatePostsEndedHandler,
                    postLikeActionUseCase
            )
        }
    }
}
