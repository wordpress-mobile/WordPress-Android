package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.usecases.FetchPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetNumPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetPostsForTagWithCountUseCase
import org.wordpress.android.ui.reader.repository.usecases.ReaderRepositoryEventBusHandler
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import javax.inject.Inject
import javax.inject.Named

class ReaderDiscoverRepository constructor(
    private val bgDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper,
    private val readerTag: ReaderTag,
    private val getPostsForTagUseCase: GetPostsForTagUseCase,
    private val getNumPostsForTagUseCase: GetNumPostsForTagUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val getPostsForTagWithCountUseCase: GetPostsForTagWithCountUseCase,
    private val fetchPostsForTagUseCase: FetchPostsForTagUseCase
) {
    private var isStarted = false

    private val _discoverFeed = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActiveDiscoverFeed() }, onInactive = { onInactiveDiscoverFeed() })
    val discoverFeed: ReactiveMutableLiveData<ReaderPostList> = _discoverFeed

    private val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    private lateinit var readerRepositoryEventBusHandler: ReaderRepositoryEventBusHandler

    fun start() {
        if (isStarted) return

        isStarted = true
        initEventBusHandler()
    }

    private fun initEventBusHandler() {
        readerRepositoryEventBusHandler = ReaderRepositoryEventBusHandler(
                ReaderRepositoryEventBusHandler.setUpdatePostsEndedListeners(this::onNewPosts,
                this::onChangedPosts, this::onUnchanged, this::onFailed), eventBusWrapper, readerTag)
        readerRepositoryEventBusHandler.start()
    }

    fun stop() {
        getPostsForTagUseCase.stop()
        getNumPostsForTagUseCase.stop()
        shouldAutoUpdateTagUseCase.stop()
        getPostsForTagWithCountUseCase.stop()
        readerRepositoryEventBusHandler.stop()
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
        // todo: annmarie implement
    }

    private fun onFailed(event: UpdatePostsEnded) {
        _communicationChannel.postValue(
                Event(ReaderRepositoryCommunication.Error.RemoteRequestFailure))
    }

    private fun onActiveDiscoverFeed() {
        loadPosts()
    }

    private fun onInactiveDiscoverFeed() {
        // todo: annmarie this may not be used
    }

    private fun loadPosts() {
        GlobalScope.launch(bgDispatcher) {
            val existsInMemory = discoverFeed.value?.let {
                !it.isEmpty()
            } ?: false
            val refresh = shouldAutoUpdateTagUseCase.get(readerTag)

            if (!existsInMemory) {
                val result = getPostsForTagUseCase.get(readerTag)
                _discoverFeed.postValue(result)
            }

            if (refresh) {
                val response = fetchPostsForTagUseCase.fetch(readerTag)
                _communicationChannel.postValue(Event(response))
            }
        }
    }

    private fun reloadPosts() {
        GlobalScope.launch(bgDispatcher) {
            val result = getPostsForTagUseCase.get(readerTag)
            _discoverFeed.postValue(result)
        }
    }

    class Factory
    @Inject constructor(
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val eventBusWrapper: EventBusWrapper,
        private val readerUtilsWrapper: ReaderUtilsWrapper,
        private val getPostsForTagUseCase: GetPostsForTagUseCase,
        private val getNumPostsForTagUseCase: GetNumPostsForTagUseCase,
        private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
        private val getPostsForTagWithCountUseCase: GetPostsForTagWithCountUseCase,
        private val fetchPostsForTagUseCase: FetchPostsForTagUseCase
    ) {
        fun create(readerTag: ReaderTag? = null): ReaderDiscoverRepository {
            val tag = readerTag
                    ?: readerUtilsWrapper.getTagFromTagName(ReaderConstants.KEY_DISCOVER, DEFAULT)

            return ReaderDiscoverRepository(
                    bgDispatcher,
                    eventBusWrapper,
                    tag,
                    getPostsForTagUseCase,
                    getNumPostsForTagUseCase,
                    shouldAutoUpdateTagUseCase,
                    getPostsForTagWithCountUseCase,
                    fetchPostsForTagUseCase
            )
        }
    }
}
