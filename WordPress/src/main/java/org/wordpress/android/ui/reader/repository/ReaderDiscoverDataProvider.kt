package org.wordpress.android.ui.reader.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.ReaderPostTableActionEnded
import org.wordpress.android.ui.reader.repository.usecases.FetchDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReaderDiscoverDataProvider constructor(
    private val ioDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper,
    private val readerTag: ReaderTag,
    private val getDiscoverCardsUseCase: GetDiscoverCardsUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase,
    private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + job

    private var isStarted = false
    private val isDirty = AtomicBoolean()

    private val _discoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>(
            onActive = { onActiveDiscoverFeed() }, onInactive = { onInactiveDiscoverFeed() })
    val discoverFeed: LiveData<ReaderDiscoverCards> = _discoverFeed

    private val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    fun start() {
        if (isStarted) return

        isStarted = true
        eventBusWrapper.register(this)
        readerUpdatePostsEndedHandler.start(readerTag,
                ReaderUpdatePostsEndedHandler.setUpdatePostsEndedListeners(
                        this::onNewPosts,
                        this::onChangedPosts, this::onUnchanged, this::onFailed
                ))
    }

    fun stop() {
        eventBusWrapper.unregister(this)
        job.cancel()
    }

    fun getTag(): ReaderTag = readerTag

    suspend fun refreshPosts() {
        withContext(ioDispatcher) {
            val response = fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)
            // todo annmarie do we want to post all responses on the communication channel
            if (response != Started) _communicationChannel.postValue(Event(response))
        }
    }

    // Internal functionality
    private suspend fun loadPosts() {
        withContext(ioDispatcher) {
            val forceReload = isDirty.getAndSet(false)
            val existsInMemory = discoverFeed.value?.cards?.isNotEmpty() ?: false
            val refresh = shouldAutoUpdateTagUseCase.get(readerTag)
            if (forceReload || !existsInMemory) {
                val result = getDiscoverCardsUseCase.get()
                _discoverFeed.postValue(result)
            }

            if (refresh) {
                val response = fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)
                // todo annmarie do we want to post all responses on the communication channel
                if (response != Started) _communicationChannel.postValue(Event(response))
            }
        }
    }

    private suspend fun reloadPosts() {
        withContext(ioDispatcher) {
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

    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onReaderPostTableAction(event: ReaderPostTableActionEnded) {
        if (_discoverFeed.hasObservers()) {
            isDirty.compareAndSet(true, false)
            launch {
                reloadPosts()
            }
        } else {
            isDirty.compareAndSet(false, true)
        }
    }

    class Factory
    @Inject constructor(
        @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
        private val eventBusWrapper: EventBusWrapper,
        private val readerUtilsWrapper: ReaderUtilsWrapper,
        private val getDiscoverCardsUseCase: GetDiscoverCardsUseCase,
        private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
        private val fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase,
        private val readerUpdatePostsEndedHandler: ReaderUpdatePostsEndedHandler
    ) {
        fun create(readerTag: ReaderTag? = null): ReaderDiscoverDataProvider {
            val tag = readerTag
                    ?: readerUtilsWrapper.getTagFromTagName(ReaderConstants.KEY_DISCOVER, DEFAULT)

            return ReaderDiscoverDataProvider(
                    ioDispatcher,
                    eventBusWrapper,
                    tag,
                    getDiscoverCardsUseCase,
                    shouldAutoUpdateTagUseCase,
                    fetchDiscoverCardsUseCase,
                    readerUpdatePostsEndedHandler
            )
        }
    }
}
