package org.wordpress.android.ui.reader.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.SearchPostsStarted
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsStarted
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.CHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class ReaderPostRepository
constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val bgDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val readerTag: ReaderTag
) : CoroutineScope {
    private val tagPostsMap: HashMap<ReaderTag, ReaderPostList> = hashMapOf()

    // todo: annmarie does the payload need the request with it
    private val _postsForTag = ReactiveMutableLiveData<ReaderPostList>(
            onActive = { onActivePostsForTag() }, onInactive = { onInactivePostsForTag() })
    val postsForTag: ReactiveMutableLiveData<ReaderPostList> = _postsForTag

    // todo: annmarie does the payload need the request with it
    private val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true
        eventBusWrapper.register(this)
    }

    fun stop() {
        eventBusWrapper.unregister(this)
        parentJob.cancelChildren()
    }

    // Notifications on observers
    private fun onActivePostsForTag() {
        loadPostsForTag()
    }

    private fun onInactivePostsForTag() {
        // TODO: annmarie Not sure if we are going to use this callback method for anything
    }

    // Local storage
    private fun getPostsForTagFromLocalStorage(readerTag: ReaderTag): ReaderPostList {
        return ReaderPostTable.getPostsWithTag(
                readerTag,
                MAX_ROWS,
                EXCLUDE_TEXT_COLUMN
        )
    }

    private fun getNumPostsForTagFromLocalStorage(readerTag: ReaderTag): Int {
        return ReaderPostTable.getNumPostsWithTag(readerTag)
    }

    // The initial request is always REQUEST_NEWER, so make that the default
    private fun requestPostsForTagFromRemoteStorage(
        updateAction: UpdateAction = REQUEST_NEWER
    ) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _communicationChannel.postValue(
                    Event(ReaderRepositoryCommunication.Error.NetworkUnavailable)
            )
            return
        }

        ReaderPostServiceStarter.startServiceForTag(
                contextProvider.getContext(),
                readerTag,
                updateAction
        )
    }

    private suspend fun shouldAutoUpdateTag(): Boolean {
        return withContext(bgDispatcher) {
            ReaderTagTable.shouldAutoUpdateTag(readerTag)
        }
    }

    // Load posts
    // todo: refactor method and names
    private fun loadPostsForTag() {
        launch {
            val existsInMap = tagPostsMap[readerTag]?.let {
                !it.isEmpty() }?:false
            val refresh = shouldAutoUpdateTag()

            if (existsInMap) {
                _postsForTag.postValue(tagPostsMap[readerTag])
            } else {
                val result = runCatching { getPostsForTag(readerTag) }
                result.onSuccess {
                    tagPostsMap[readerTag] = it
                    _postsForTag.postValue(tagPostsMap[readerTag])
                }
                        .onFailure {
                            _communicationChannel.postValue(
                                    Event(
                                            ReaderRepositoryCommunication.Error.ReaderRepositoryException(
                                                    Exception(it)
                                            )
                                    )
                            )
                        }
            }
            if (refresh) {
                requestPostsForTagFromRemoteStorage()
            }
        }
    }

    // Reload posts
    private fun reloadPostsForTag() {
        val tag = readerTag ?: return

        launch {
            val result = runCatching { getPostsForTag(tag) }
            result.onSuccess {
                tagPostsMap[tag] = it
                _postsForTag.postValue(tagPostsMap[tag])
            }
                    .onFailure {
                        _communicationChannel.postValue(
                                Event(
                                        ReaderRepositoryCommunication.Error.ReaderRepositoryException(
                                                Exception(it)
                                        )
                                )
                        )
                    }
        }
    }

    // todo: annmarie - if we need to include the count, then we have to rework the
    // payload to include it. If not, drop the deferred and let it rip, I still like
    // the runCatching only because it gives us a nifty way to capture and handle the
    // error properly.
    // Need to get the count & the data at the same time together
    // once you call await, you suspend the parent (the "withContext" until the value arrives)
    // works like a future promise but with coroutines
    private suspend fun getPostsForTag(readerTag: ReaderTag): ReaderPostList =
            withContext(ioDispatcher) {
                val postsForTagFromLocalDeferred = async {
                    getPostsForTagFromLocalStorage(readerTag)
                }

                val totalPostsForTagFromLocalDeferred = async {
                    getNumPostsForTagFromLocalStorage(readerTag)
                }

                val readerPostList = postsForTagFromLocalDeferred.await()
                val totalEntriesForTag = totalPostsForTagFromLocalDeferred.await()

                readerPostList
            }

    fun requestPostsForTag(action: UpdateAction) {
        requestPostsForTagFromRemoteStorage(action)
    }


    // coroutine related - there may be some injectables that I am not aware of - ask jirka
    private val parentJob = SupervisorJob() // a job that can cancel all its children at once

    private val coroutineExceptionHandler =
            CoroutineExceptionHandler { context, throwable ->
                throwable.printStackTrace()
            }

    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + parentJob + coroutineExceptionHandler

    // Event Bus events emitted from ReaderPostLogic (
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsStarted) {
        val eventTag = event.readerTag ?: return
        if (isTag(eventTag)) {
            // ignore events not related to this instance of ReaderPostRepository
            return
        }
        Log.i(
                javaClass.simpleName,
                "***=> Received UpdatePostsStarted for ${event.readerTag?.tagNameForLog}"
        )
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsEnded) {
        if (event.readerTag != null && !isTag(event.readerTag)) {
            // ignore events not related to this instance of ReaderPostRepository
            return
        }

        Log.i(javaClass.simpleName, "***=> Received UpdatePostsEnded for Action ${event.action}")
        Log.i(
                javaClass.simpleName,
                "***=> Received UpdatePostsEnded for Tag ${event.readerTag?.tagNameForLog}"
        )

        event.result?.let {
            when (it) {
                HAS_NEW -> {
                    Log.i(javaClass.simpleName, "***=> is new")
                    reloadPostsForTag()
                }
                CHANGED -> {
                    Log.i(javaClass.simpleName, "***=> is changed")
                    reloadPostsForTag()
                }
                UNCHANGED -> {
                    Log.i(javaClass.simpleName, "***=> is unchanged")
                }
                FAILED -> {
                    Log.i(javaClass.simpleName, "***=> is failed")
                    // todo: what are we doing about failures?
                }
            }
        }
    }

    private fun isTag(tag: ReaderTag): Boolean {
        return ReaderTag.isSameTag(tag, readerTag)
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SearchPostsStarted) {
        Log.i(javaClass.simpleName, "***=> TODO")
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: SearchPostsEnded) {
        Log.i(javaClass.simpleName, "***=> TODO")
    }

    companion object {
        private const val EXCLUDE_TEXT_COLUMN = true
        private const val MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
    }

    class Factory
    @Inject constructor(
        private val contextProvider: ContextProvider,
        private val eventBusWrapper: EventBusWrapper,
        private val networkUtilsWrapper: NetworkUtilsWrapper,
        @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
    ) {
        fun create(readerTag: ReaderTag): ReaderPostRepository {
            return ReaderPostRepository(
                    contextProvider,
                    eventBusWrapper,
                    networkUtilsWrapper,
                    bgDispatcher,
                    ioDispatcher,
                    readerTag
            )
        }
    }
}

// todo: annmarie - these may or may not stay
sealed class ReaderRepositoryAction(val action: UpdateAction) {
    class Discover(val interests: List<String>, action: UpdateAction) :
            ReaderRepositoryAction(action)

    class PostsForTag(action: UpdateAction) : ReaderRepositoryAction(action)

    class PostsForBlog(action: UpdateAction) : ReaderRepositoryAction(action)

    class PostsForFeed(action: UpdateAction) : ReaderRepositoryAction(action)

    override fun toString(): String {
        return "${this.javaClass.simpleName}($action)"
    }
}

// todo: annmarie - decide if we need to include the action that was requested
sealed class ReaderRepositoryCommunication {
    object Success : ReaderRepositoryCommunication()
    sealed class Error : ReaderRepositoryCommunication() {
        object NetworkUnavailable : Error()
        class ReaderRepositoryException(val exception: Exception) : Error()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}


