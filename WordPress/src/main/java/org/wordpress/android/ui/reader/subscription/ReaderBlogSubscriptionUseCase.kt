package org.wordpress.android.ui.reader.subscription

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.SubscriptionModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload.SubscriptionFrequency
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReaderBlogSubscriptionUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var updateContinuation: Continuation<UpdateResult>? = null

    init {
        dispatcher.register(this)
    }

    fun cleanup() {
        dispatcher.unregister(this)
    }

    suspend fun getSubscriptionForBlog(blogId: Long): SubscriptionModel? = withContext(bgDispatcher) {
        accountStore.subscriptions.find { it.blogId == blogId.toString() }
    }

    suspend fun updateNotifyPosts(blogId: Long, enable: Boolean): UpdateResult {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return UpdateResult.NoNetwork
        }
        return suspendCoroutine { continuation ->
            updateContinuation = continuation
            val action = if (enable) SubscriptionAction.NEW else SubscriptionAction.DELETE
            val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
            dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(payload))
        }
    }

    suspend fun updateEmailPosts(blogId: Long, enable: Boolean): UpdateResult {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return UpdateResult.NoNetwork
        }
        return suspendCoroutine { continuation ->
            updateContinuation = continuation
            val action = if (enable) SubscriptionAction.NEW else SubscriptionAction.DELETE
            val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
            dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionEmailPostAction(payload))
        }
    }

    suspend fun updateEmailPostsFrequency(blogId: Long, frequency: SubscriptionFrequency): UpdateResult =
        suspendCoroutine { continuation ->
            updateContinuation = continuation
            val payload = UpdateSubscriptionPayload(blogId.toString(), frequency)
            dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionEmailPostFrequencyAction(payload))
        }

    suspend fun updateEmailComments(blogId: Long, enable: Boolean): UpdateResult {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return UpdateResult.NoNetwork
        }
        return suspendCoroutine { continuation ->
            updateContinuation = continuation
            val action = if (enable) SubscriptionAction.NEW else SubscriptionAction.DELETE
            val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
            dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionEmailCommentAction(payload))
        }
    }

    fun refreshSubscriptions() {
        dispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSubscriptionUpdated(event: OnSubscriptionUpdated) {
        val result = if (event.isError) {
            UpdateResult.Failure(event.error?.message)
        } else {
            UpdateResult.Success
        }
        updateContinuation?.resume(result)
        updateContinuation = null

        // Refresh subscriptions after successful update
        if (!event.isError) {
            refreshSubscriptions()
        }
    }

    sealed class UpdateResult {
        object Success : UpdateResult()
        object NoNetwork : UpdateResult()
        data class Failure(val errorMessage: String?) : UpdateResult()
    }
}
