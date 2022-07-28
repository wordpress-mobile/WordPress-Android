package org.wordpress.android.ui.reader.repository.usecases.tags

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.ui.reader.ReaderEvents.InterestTagsFetchEnded
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask.INTEREST_TAGS
import org.wordpress.android.ui.reader.services.update.wrapper.ReaderUpdateServiceStarterWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.util.EnumSet
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FetchInterestTagsUseCase @Inject constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerUpdateServiceStarterWrapper: ReaderUpdateServiceStarterWrapper
) {
    private var continuation: Continuation<ReaderRepositoryCommunication>? = null

    @Suppress("UseCheckOrError")
    suspend fun fetch(): ReaderRepositoryCommunication {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }
        return suspendCoroutine { cont ->
            continuation = cont
            eventBusWrapper.register(this)

            readerUpdateServiceStarterWrapper.startService(
                contextProvider.getContext(),
                EnumSet.of(INTEREST_TAGS)
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onInterestTagsFetchEnded(event: InterestTagsFetchEnded) {
        val result = if (event.didSucceed()) {
                SuccessWithData(event.interestTags)
            } else {
                RemoteRequestFailure
            }

        continuation?.resume(result)

        eventBusWrapper.unregister(this)
        continuation = null
    }
}
