package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.OnReaderRepositoryEvent.OnPostLikeEnded.OnPostLikeFailure
import org.wordpress.android.ui.reader.repository.OnReaderRepositoryEvent.OnPostLikeEnded.OnPostLikeSuccess
import org.wordpress.android.ui.reader.repository.OnReaderRepositoryEvent.OnPostLikeEnded.OnPostLikeUnChanged
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.usecases.FetchPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetNumPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagWithCountUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeActionUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReaderPostRepository(
    private val bgDispatcher: CoroutineDispatcher,
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

    private val _mutablePosts = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActivePosts() }, onInactive = { onInactivePosts() })
    val posts: LiveData<ReaderPostList> = _mutablePosts

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
        getPostsForTagUseCase.stop()
        getNumPostsForTagUseCase.stop()
        shouldAutoUpdateTagUseCase.stop()
        getPostsForTagWithCountUseCase.stop()
        readerUpdatePostsEndedHandler.stop()
    }

    fun getTag(): ReaderTag {
        return readerTag
    }

    fun performLikeAction(post: ReaderPost, isAskingToLike: Boolean, wpComUserId: Long) {
        // todo: annmarie - think about this, do I really want to handle all of these returns
        // in the repository itself? It will get cluttered awfully quickly. Is there an
        // opportunity to use a type of "Manager" that will call the "UseCase".
        launch {
            val event: OnReaderRepositoryEvent
            try {
                event = postLikeActionUseCase.perform(post, isAskingToLike, wpComUserId)
            } catch (e: IllegalStateException) {
                return@launch
            }
            // todo: annmarie - what does the caller want state ?
            when (event) {
                is OnPostLikeSuccess -> {
                    // The db table has been updated - new post needed?
                }
                is OnPostLikeFailure -> {
                    // The db table has been updated - new post needed?
                }
                is OnPostLikeUnChanged -> { }
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
                Event(ReaderRepositoryCommunication.Error.RemoteRequestFailure))
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
            val refresh = shouldAutoUpdateTagUseCase.get(readerTag)

            if (!existsInMemory) {
                val result = getPostsForTagUseCase.get(readerTag)
                _mutablePosts.postValue(result)
            }

            if (refresh) {
                val response = fetchPostsForTagUseCase.fetch(readerTag)
                if (response != Success) _communicationChannel.postValue(Event(response))
            }
        }
    }

    private fun reloadPosts() {
        launch {
            val result = getPostsForTagUseCase.get(readerTag)
            _mutablePosts.postValue(result)
        }
    }

    class Factory
    @Inject constructor(
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
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

// todo: annmarie once it's decided to keep this, then move out of repository file
sealed class OnReaderRepositoryEvent {
    sealed class OnPostLikeEnded : OnReaderRepositoryEvent() {
        // todo: on both the success and failure, we prob need to send post to caller
        class OnPostLikeSuccess(val postId: Long, val blogId: Long) : OnPostLikeEnded()
        object OnPostLikeFailure : OnPostLikeEnded()
        object OnPostLikeUnChanged : OnPostLikeEnded()
    }
}
