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
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderEvents.FetchDiscoverCardsEnded
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.CHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.ReaderPostTableActionEnded
import org.wordpress.android.ui.reader.repository.usecases.FetchDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.GetDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.ShouldAutoUpdateTagUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReaderDiscoverDataProvider @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val eventBusWrapper: EventBusWrapper,
    private val getDiscoverCardsUseCase: GetDiscoverCardsUseCase,
    private val shouldAutoUpdateTagUseCase: ShouldAutoUpdateTagUseCase,
    private val fetchDiscoverCardsUseCase: FetchDiscoverCardsUseCase
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + job

    private var isStarted = false
    // Indicates that the data were changed in the db while no-one was subscribed to the feed.
    private val isDirty = AtomicBoolean()

    private val _discoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>(
            onActive = { onActiveDiscoverFeed() }, onInactive = { onInactiveDiscoverFeed() })
    val discoverFeed: LiveData<ReaderDiscoverCards> = _discoverFeed
    private var hasMoreCards = true

    private val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    // TODO malinjir/annmarie The UI might need to know if a request is in progress, wdyt?
    // TODO malinjir/annmarie The UI might need to know if there are more data (next page) available

    fun start() {
        if (isStarted) return

        isStarted = true
        eventBusWrapper.register(this)
    }

    fun stop() {
        eventBusWrapper.unregister(this)
        job.cancel()
    }

    suspend fun refreshCards() {
        withContext(ioDispatcher) {
            val response = fetchDiscoverCardsUseCase.fetch(REQUEST_FIRST_PAGE)
            // todo annmarie do we want to post all responses on the communication channel
            if (response != Started) _communicationChannel.postValue(Event(response))
        }
    }

    suspend fun loadMoreCards() {
        // TODO malinjir check that the request isn't already in progress
        if (hasMoreCards) {
            withContext(ioDispatcher) {
                val response = fetchDiscoverCardsUseCase.fetch(REQUEST_MORE)
                // todo annmarie do we want to post all responses on the communication channel
                if (response != Started) _communicationChannel.postValue(Event(response))
            }
        }
    }

    // Internal functionality
    private suspend fun loadCards() {
        withContext(ioDispatcher) {
            val forceReload = isDirty.getAndSet(false)
            val existsInMemory = discoverFeed.value?.cards?.isNotEmpty() ?: false
            val refresh = shouldAutoUpdateTagUseCase.get(ReaderTag.createDiscoverPostCardsTag())
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
    private fun onUpdated() {
        hasMoreCards = true
        launch {
            reloadPosts()
        }
    }

    private fun onUnchanged() {
        hasMoreCards = false
    }

    private fun onFailed() {
        _communicationChannel.postValue(
                Event(ReaderRepositoryCommunication.Error.RemoteRequestFailure)
        )
    }

    // React to discoverFeed observers
    private fun onActiveDiscoverFeed() {
        launch {
            loadCards()
        }
    }

    private fun onInactiveDiscoverFeed() {
    }

    // Event bus events
    @Subscribe(threadMode = BACKGROUND)
    @SuppressWarnings("unused")
    fun onReaderPostTableAction(event: ReaderPostTableActionEnded) {
        if (_discoverFeed.hasObservers()) {
            isDirty.compareAndSet(true, false)
            onUpdated()
        } else {
            isDirty.compareAndSet(false, true)
        }
    }

    @Subscribe(threadMode = BACKGROUND)
    fun onCardsUpdated(event: FetchDiscoverCardsEnded) {
        event.result?.let {
            when (it) {
                HAS_NEW, CHANGED -> onUpdated()
                UNCHANGED -> onUnchanged()
                FAILED -> onFailed()
            }
        }
    }
}
