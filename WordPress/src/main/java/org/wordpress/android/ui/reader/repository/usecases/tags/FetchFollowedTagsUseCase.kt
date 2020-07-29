package org.wordpress.android.ui.reader.repository.usecases.tags

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.ui.reader.ReaderEvents.FollowedTagsChanged
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.TAGS
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.util.EnumSet
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FetchFollowedTagsUseCase @Inject constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    private var continuation: Continuation<ReaderRepositoryCommunication>? = null

    suspend fun fetch(): ReaderRepositoryCommunication {
        if (continuation != null) {
            throw IllegalStateException("Follow tags already in progress.")
        }

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }
        return suspendCoroutine { cont ->
            continuation = cont
            eventBusWrapper.register(this)

            ReaderUpdateServiceStarter.startService(
                contextProvider.getContext(),
                EnumSet.of(TAGS)
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onFollowedTagsChanged(event: FollowedTagsChanged) {
        val result = if (event.didSucceed()) {
            Success
        } else {
            RemoteRequestFailure
        }

        continuation?.resume(result)

        eventBusWrapper.unregister(this)
        continuation = null
    }
}
