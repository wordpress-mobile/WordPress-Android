package org.wordpress.android.ui.reader.repository.usecases.tags

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderEvents.FollowedTagsChanged
import org.wordpress.android.ui.reader.actions.ReaderTagActions
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FollowInterestTagsUseCase @Inject constructor(
    private val eventBusWrapper: EventBusWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore
) {
    private var continuation: Continuation<ReaderRepositoryCommunication>? = null

    @Suppress("UseCheckOrError")
    suspend fun followInterestTags(tags: List<ReaderTag>): ReaderRepositoryCommunication {
        if (continuation != null) {
            throw IllegalStateException("Follow interest tags already in progress.")
        }
        val isLoggedIn = accountStore.hasAccessToken()
        if (isLoggedIn && !networkUtilsWrapper.isNetworkAvailable()) {
            return NetworkUnavailable
        }
        return suspendCoroutine { cont ->
            continuation = cont
            eventBusWrapper.register(this)

            ReaderTagActions.addTags(tags, isLoggedIn)
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
