package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Failure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeSuccess
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.PostLikeEnded.PostLikeUnChanged
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.ReaderPostTableActionEnded
import org.wordpress.android.ui.reader.repository.usecases.FetchPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetNumPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagWithCountUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeActionUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReaderPostRepository(
    private val bgDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper,
    private val readerTag: ReaderTag,
    private val getPostsForTagUseCase: GetPostsForTagUseCase,
    private val getNumPostsForTagUseCase: GetNumPostsForTagUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val getPostsForTagWithCountUseCase: GetPostsForTagWithCountUseCase,
    private val fetchPostsForTagUseCase: FetchPostsForTagUseCase,
    private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler,
    private val postLikeActionUseCase: PostLikeActionUseCase
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher

    private var isStarted = false
    private val isDirty = AtomicBoolean()

    private val _posts = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActivePosts() }, onInactive = { onInactivePosts() })
    val posts: LiveData<ReaderPostList> = _posts

    private val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    fun start() {
        if (isStarted) return

        isStarted = true
        eventBusWrapper.register(this)
        readerUpdatePostsEndedHandler.start(
                readerTag,
                ReaderUpdatePostsEndedHandler.setUpdatePostsEndedListeners(
                        this::onNewPosts,
                        this::onChangedPosts, this::onUnchanged, this::onFailed
                )
        )
    }

    fun stop() {
        eventBusWrapper.unregister(this)
        getPostsForTagUseCase.stop()
        getNumPostsForTagUseCase.stop()
        shouldAutoUpdateTagUseCase.stop()
        getPostsForTagWithCountUseCase.stop()
        readerUpdatePostsEndedHandler.stop()
    }

    fun getTag(): ReaderTag {
        return readerTag
    }

    // todo: annmarie - Possibly implement a "LikeManager" that will encapsulate all the "UseCase".
    fun performLikeAction(post: ReaderPost, isAskingToLike: Boolean, wpComUserId: Long) {
        launch {
            when (val event = postLikeActionUseCase.perform(post, isAskingToLike, wpComUserId)) {
                is PostLikeSuccess -> {
                    reloadPosts()
                }
                is PostLikeFailure -> {
                    _communicationChannel.postValue(Event(Failure(event)))
                    reloadPosts()
                }
                is PostLikeUnChanged -> {
                    // Unused
                }
            }
        }
    }

    private fun onNewPosts(event: UpdatePostsEnded) {
        reloadPosts()
    }

    private fun onChangedPosts(event: UpdatePostsEnded) {
        reloadPosts()
    }

    private fun onUnchanged(event: UpdatePostsEnded) {
    }

    private fun onFailed(event: UpdatePostsEnded) {
        _communicationChannel.postValue(
                Event(ReaderRepositoryCommunication.Error.RemoteRequestFailure)
        )
    }

    private fun onActivePosts() {
        loadPosts()
    }

    private fun onInactivePosts() {
    }

    private fun loadPosts() {
        launch {
            val existsInMemory = posts.value?.let {
                !it.isEmpty()
            } ?: false
            val refresh =
                    shouldAutoUpdateTagUseCase.get(readerTag) || isDirty.getAndSet(false)

            if (!existsInMemory) {
                reloadPosts()
            }

            if (refresh) {
                val response = fetchPostsForTagUseCase.fetch(readerTag)
                if (response != Success) _communicationChannel.postValue(Event(response))
                reloadPosts()
            }
        }
    }

    private fun reloadPosts() {
        launch {
            val result = getPostsForTagUseCase.get(readerTag)
            _posts.postValue(result)
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onReaderPostTableAction(event: ReaderPostTableActionEnded) {
        if (_posts.hasObservers()) {
            isDirty.compareAndSet(true, false)
            reloadPosts()
        } else {
            isDirty.compareAndSet(false, true)
        }
    }

    class Factory
    @Inject constructor(
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val eventBusWrapper: EventBusWrapper,
        private val getPostsForTagUseCase: GetPostsForTagUseCase,
        private val getNumPostsForTagUseCase: GetNumPostsForTagUseCase,
        private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
        private val getPostsForTagWithCountUseCase: GetPostsForTagWithCountUseCase,
        private val fetchPostsForTagUseCase: FetchPostsForTagUseCase,
        private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler,
        private val postLikeActionUseCase: PostLikeActionUseCase
    ) {
        fun create(readerTag: ReaderTag): ReaderPostRepository {
            return ReaderPostRepository(
                    bgDispatcher,
                    eventBusWrapper,
                    readerTag,
                    getPostsForTagUseCase,
                    getNumPostsForTagUseCase,
                    shouldAutoUpdateTagUseCase,
                    getPostsForTagWithCountUseCase,
                    fetchPostsForTagUseCase,
                    readerUpdatePostsEndedHandler,
                    postLikeActionUseCase

            )
        }
    }
}
