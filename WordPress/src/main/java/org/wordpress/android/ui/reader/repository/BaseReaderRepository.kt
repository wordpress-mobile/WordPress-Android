package org.wordpress.android.ui.reader.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsStarted
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.CHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event

abstract class BaseReaderRepository(
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val contextProvider: ContextProvider
) {
    protected val _communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()
    val communicationChannel: LiveData<Event<ReaderRepositoryCommunication>> = _communicationChannel

    private var isStarted = false

    open fun start() {
        if (isStarted) return
        eventBusWrapper.register(this)
    }

    open fun stop() {
        eventBusWrapper.unregister(this)
    }

    private fun isTag(tag: ReaderTag): Boolean {
        return ReaderTag.isSameTag(tag, getTag())
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsEnded) {
        if (event.readerTag != null && !isTag(event.readerTag)) {
            // ignore events not related to this instance of Repository
            return
        }

        if (!event.readerTag.isDiscover) return

        // todo: annmarie remove log lines
        Log.i(javaClass.simpleName, "***=> Received UpdatePostsEnded for " +
                " Tag: ${event.readerTag?.tagNameForLog}" +
                " Action ${event.action}")

        event.result?.let {
            when (it) {
                HAS_NEW -> {
                    onNewPosts(event)
                }
                CHANGED -> {
                    onChangedPosts(event)
                }
                UNCHANGED -> {
                    onUnchanged(event)
                }
                FAILED -> {
                    onFailed(event)
                }
            }
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdatePostsStarted) {
        val eventTag = event.readerTag ?: return
        if (isTag(eventTag)) return
        Log.i(javaClass.simpleName,
                "***=> Received UpdatePostsStarted for ${event.readerTag?.tagNameForLog}"
        )
    }

    abstract fun getTag(): ReaderTag
    abstract fun onNewPosts(event: UpdatePostsEnded)
    abstract fun onChangedPosts(event: UpdatePostsEnded)
    abstract fun onUnchanged(event: UpdatePostsEnded)
    abstract fun onFailed(event: UpdatePostsEnded)
}

sealed class ReaderRepositoryCommunication {
    object Success : ReaderRepositoryCommunication()
    sealed class Error : ReaderRepositoryCommunication() {
        object NetworkUnavailable : Error()
        object RemoteRequestFailure : Error()
        class ReaderRepositoryException(val exception: Exception) : Error()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}
