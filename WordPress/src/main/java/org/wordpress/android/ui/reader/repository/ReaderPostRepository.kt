package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.usecases.GetNumPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagWithCountUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import javax.inject.Inject
import javax.inject.Named

class ReaderPostRepository(
    private val bgDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val readerTag: ReaderTag,
    private val getPostsForTagUseCase: GetPostsForTagUseCase,
    private val getNumPostsForTagUseCase: GetNumPostsForTagUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val getPostsForTagWithCountUseCase: GetPostsForTagWithCountUseCase
) : BaseReaderRepository(eventBusWrapper,
        networkUtilsWrapper,
        contextProvider) {
    private val _postsForTag = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActivePostsForTag() }, onInactive = { onInactivePostsForTag() })
    val postsForTag: ReactiveMutableLiveData<ReaderPostList> = _postsForTag

    private var isStarted = false

    override fun start() {
        if (isStarted) return

        isStarted = true
        super.start()
    }

    override fun stop() {
        getPostsForTagUseCase.stop()
        getNumPostsForTagUseCase.stop()
        shouldAutoUpdateTagUseCase.stop()
        getPostsForTagWithCountUseCase.stop()
    }

    override fun getTag(): ReaderTag {
        return readerTag
    }

    override fun onNewPosts(event: UpdatePostsEnded) {
        reloadPosts()
    }

    override fun onChangedPosts(event: UpdatePostsEnded) {
        reloadPosts()
    }

    override fun onUnchanged(event: UpdatePostsEnded) {
        // todo: annmarie Handle the refresh situation but nothing changed
    }

    override fun onFailed(event: UpdatePostsEnded) {
        _communicationChannel.postValue(
                Event(RemoteRequestFailure)
        )
    }

    private fun onActivePostsForTag() {
        loadPosts()
    }

    private fun onInactivePostsForTag() {
        // todo: annmarie this may not be used
    }

    private fun loadPosts() {
        GlobalScope.launch(bgDispatcher) {
            val existsInMemory = postsForTag.value?.let {
                !it.isEmpty()
            } ?: false
            val refresh = shouldAutoUpdateTagUseCase.fetch(readerTag)

            if (!existsInMemory) {
                val result = getPostsForTagUseCase.fetch(readerTag)
                _postsForTag.postValue(result)
            }

            if (refresh) {
                requestPostsFromRemoteStorage(readerTag)
            }
        }
    }

    private fun reloadPosts() {
        GlobalScope.launch(bgDispatcher) {
            val result = getPostsForTagUseCase.fetch(readerTag)
            _postsForTag.postValue(result)
        }
    }

    class Factory
    @Inject constructor(
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val eventBusWrapper: EventBusWrapper,
        private val networkUtilsWrapper: NetworkUtilsWrapper,
        private val contextProvider: ContextProvider,
        private val getPostsForTagUseCase: GetPostsForTagUseCase,
        private val fetchNumPostsForTagUseCase: GetNumPostsForTagUseCase,
        private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
        private val getPostsForTagWithCountUseCase: GetPostsForTagWithCountUseCase
    ) {
        fun create(readerTag: ReaderTag): ReaderPostRepository {

            return ReaderPostRepository(
                    bgDispatcher,
                    eventBusWrapper,
                    networkUtilsWrapper,
                    contextProvider,
                    readerTag,
                    getPostsForTagUseCase,
                    fetchNumPostsForTagUseCase,
                    shouldAutoUpdateTagUseCase,
                    getPostsForTagWithCountUseCase
            )
        }
    }
}
