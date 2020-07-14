package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.usecases.FetchPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetNumPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagWithCountUseCase
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
    private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher

    private var isStarted = false

    private val _postsForTag = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActivePostsForTag() }, onInactive = { onInactivePostsForTag() })
    val postsForTag: LiveData<ReaderPostList> = _postsForTag

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

    private fun onActivePostsForTag() {
        loadPosts()
    }

    private fun onInactivePostsForTag() {
    }

    private fun loadPosts() {
        launch {
            val existsInMemory = postsForTag.value?.let {
                !it.isEmpty()
            } ?: false
            val refresh = shouldAutoUpdateTagUseCase.get(readerTag)

            if (!existsInMemory) {
                val result = getPostsForTagUseCase.get(readerTag)
                _postsForTag.postValue(result)
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
            _postsForTag.postValue(result)
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
        private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler
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
                    readerUpdatePostsEndedHandler
            )
        }
    }
}
