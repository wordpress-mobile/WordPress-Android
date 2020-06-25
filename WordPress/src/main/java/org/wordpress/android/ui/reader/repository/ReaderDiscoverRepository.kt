package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryUseCaseType.FETCH_NUM_POSTS_BY_TAG
import org.wordpress.android.ui.reader.repository.ReaderRepositoryUseCaseType.FETCH_POSTS_BY_TAG
import org.wordpress.android.ui.reader.repository.ReaderRepositoryUseCaseType.FETCH_POSTS_BY_TAG_WITH_COUNT
import org.wordpress.android.ui.reader.repository.ReaderRepositoryUseCaseType.SHOULD_AUTO_UDPATE_TAG
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import javax.inject.Inject
import javax.inject.Named

class ReaderDiscoverRepository constructor(
    private val bgDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val readerTag: ReaderTag,
    private val fetchPostsForTagUseCase: FetchPostsForTagUseCase,
    private val fetchNumPostsForTagUseCase: FetchNumPostsForTagUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val fetchPostsForTagWithCountUseCase: FetchPostsForTagWithCountUseCase
) : BaseReaderRepository(eventBusWrapper,
        networkUtilsWrapper,
        contextProvider) {
    private val discoverPostsMap: HashMap<ReaderTag, ReaderPostList> = hashMapOf()
    private val readerRepositoryUseCases:
            HashMap<ReaderRepositoryUseCaseType, ReaderRepositoryDispatchingUseCase> = hashMapOf()
    private var isStarted = false

    private val _discoverFeed = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActiveDiscoverFeed() }, onInactive = { onInactiveDiscoverFeed() })
    val discoverFeed: ReactiveMutableLiveData<ReaderPostList> = _discoverFeed

    init {
        readerRepositoryUseCases[FETCH_POSTS_BY_TAG] = fetchPostsForTagUseCase
        readerRepositoryUseCases[FETCH_NUM_POSTS_BY_TAG] = fetchNumPostsForTagUseCase
        readerRepositoryUseCases[SHOULD_AUTO_UDPATE_TAG] = shouldAutoUpdateTagUseCase
        readerRepositoryUseCases[FETCH_POSTS_BY_TAG_WITH_COUNT] = fetchPostsForTagWithCountUseCase
    }

    override fun start() {
        if (isStarted) return

        isStarted = true
        super.start()
    }

    override fun stop() {
        readerRepositoryUseCases.values.forEach { it.stop() }
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
        // todo: annmarie implement
    }

    override fun onFailed(event: UpdatePostsEnded) {
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
            val existsInMemory = discoverPostsMap[readerTag]?.let {
                !it.isEmpty()
            } ?: false
            val refresh = shouldAutoUpdateTagUseCase.fetch(readerTag)

            if (existsInMemory) {
                _discoverFeed.postValue(discoverPostsMap[readerTag])
            } else {
                val result = fetchPostsForTagUseCase.fetch(readerTag)
                discoverPostsMap[readerTag] = result
                _discoverFeed.postValue(discoverPostsMap[readerTag])
            }

            if (refresh) {
                requestDiscoverFeedFromRemoteStorage(readerTag)
            }
        }
    }

    private fun reloadPosts() {
        GlobalScope.launch(bgDispatcher) {
            val result = fetchPostsForTagUseCase.fetch(readerTag)
            discoverPostsMap[readerTag] = result
            _discoverFeed.postValue(discoverPostsMap[readerTag])
        }
    }

    class Factory
    @Inject constructor(
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val eventBusWrapper: EventBusWrapper,
        private val networkUtilsWrapper: NetworkUtilsWrapper,
        private val contextProvider: ContextProvider,
        private val readerUtilsWrapper: ReaderUtilsWrapper,
        private val fetchPostsForTagUseCase: FetchPostsForTagUseCase,
        private val fetchNumPostsForTagUseCase: FetchNumPostsForTagUseCase,
        private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
        private val fetchPostsForTagWithCountUseCase: FetchPostsForTagWithCountUseCase
    ) {
        fun create(readerTag: ReaderTag? = null): ReaderDiscoverRepository {
            val tag = readerTag ?:
            readerUtilsWrapper.getTagFromTagName(ReaderConstants.KEY_DISCOVER, DEFAULT)

            return ReaderDiscoverRepository(
                    bgDispatcher,
                    eventBusWrapper,
                    networkUtilsWrapper,
                    contextProvider,
                    tag,
                    fetchPostsForTagUseCase,
                    fetchNumPostsForTagUseCase,
                    shouldAutoUpdateTagUseCase,
                    fetchPostsForTagWithCountUseCase
            )
        }
    }
}
