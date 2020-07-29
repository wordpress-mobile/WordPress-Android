package org.wordpress.android.ui.reader.repository

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.BACKGROUND
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderEvents.UpdatePostsEnded
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.CHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject

class ReaderUpdatePostsEndedHandler @Inject constructor(
    private val eventBusWrapper: EventBusWrapper
) {
    private var isStarted = false
    private lateinit var readerTag: ReaderTag
    private lateinit var onReaderRepositoryUpdatePostsEndedListener: OnReaderRepositoryUpdatePostsEndedListener

    fun start(
        readerTag: ReaderTag,
        onReaderRepositoryUpdatePostsEndedListener: OnReaderRepositoryUpdatePostsEndedListener
    ) {
        if (isStarted) return

        this.onReaderRepositoryUpdatePostsEndedListener = onReaderRepositoryUpdatePostsEndedListener
        this.readerTag = readerTag

        eventBusWrapper.register(this)
    }

    fun stop() {
        eventBusWrapper.unregister(this)
    }

    private fun onNewPosts(event: UpdatePostsEnded) {
        onReaderRepositoryUpdatePostsEndedListener.onNewPosts(event)
    }
    private fun onChangedPosts(event: UpdatePostsEnded) {
        onReaderRepositoryUpdatePostsEndedListener.onChangedPosts(event)
    }
    private fun onUnchanged(event: UpdatePostsEnded) {
        onReaderRepositoryUpdatePostsEndedListener.onUnchanged(event)
    }
    private fun onFailed(event: UpdatePostsEnded) {
        onReaderRepositoryUpdatePostsEndedListener.onFailed(event)
    }

    @Subscribe(threadMode = BACKGROUND)
    fun onEventMainThread(event: UpdatePostsEnded) {
        if (event.readerTag != null && !ReaderTag.isSameTag(event.readerTag, readerTag)) {
            // ignore events not related to this instance of Repository
            return
        }

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

    companion object {
        fun setUpdatePostsEndedListeners(
            onNewPosts: (event: UpdatePostsEnded) -> Unit,
            onChangedPosts: (event: UpdatePostsEnded) -> Unit,
            onUnchanged: (event: UpdatePostsEnded) -> Unit,
            onFailed: (event: UpdatePostsEnded) -> Unit
        ): OnReaderRepositoryUpdatePostsEndedListener {
            return object : OnReaderRepositoryUpdatePostsEndedListener {
                override fun onNewPosts(event: UpdatePostsEnded) {
                    onNewPosts.invoke(event)
                }

                override fun onChangedPosts(event: UpdatePostsEnded) {
                    onChangedPosts.invoke(event)
                }

                override fun onUnchanged(event: UpdatePostsEnded) {
                    onUnchanged.invoke(event)
                }

                override fun onFailed(event: UpdatePostsEnded) {
                    onFailed.invoke(event)
                }
            }
        }
    }
}

interface OnReaderRepositoryUpdatePostsEndedListener {
    fun onNewPosts(event: UpdatePostsEnded)
    fun onChangedPosts(event: UpdatePostsEnded)
    fun onUnchanged(event: UpdatePostsEnded)
    fun onFailed(event: UpdatePostsEnded)
}
