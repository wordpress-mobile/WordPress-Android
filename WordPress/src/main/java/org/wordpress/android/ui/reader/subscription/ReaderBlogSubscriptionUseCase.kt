package org.wordpress.android.ui.reader.subscription

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class ReaderBlogSubscriptionUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val updateResultFlow = MutableSharedFlow<UpdateResult>(extraBufferCapacity = 1)

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
        val action = if (enable) SubscriptionAction.NEW else SubscriptionAction.DELETE
        val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
        dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionNotificationPostAction(payload))
        return updateResultFlow.first()
    }

    suspend fun updateEmailPosts(blogId: Long, enable: Boolean): UpdateResult {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return UpdateResult.NoNetwork
        }
        val action = if (enable) SubscriptionAction.NEW else SubscriptionAction.DELETE
        val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
        dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionEmailPostAction(payload))
        return updateResultFlow.first()
    }

    suspend fun updateEmailComments(blogId: Long, enable: Boolean): UpdateResult {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return UpdateResult.NoNetwork
        }
        val action = if (enable) SubscriptionAction.NEW else SubscriptionAction.DELETE
        val payload = AddOrDeleteSubscriptionPayload(blogId.toString(), action)
        dispatcher.dispatch(AccountActionBuilder.newUpdateSubscriptionEmailCommentAction(payload))
        return updateResultFlow.first()
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
        updateResultFlow.tryEmit(result)

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
