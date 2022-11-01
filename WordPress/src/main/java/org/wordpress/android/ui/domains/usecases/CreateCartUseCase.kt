package org.wordpress.android.ui.domains.usecases

import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Wraps an [OnShoppingCartCreated] into a coroutine.
 */
class CreateCartUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val transactionsStore: TransactionsStore // needed for events to work
) {
    private var continuation: Continuation<OnShoppingCartCreated>? = null

    init {
        dispatcher.register(this)
    }

    fun clear() {
        dispatcher.unregister(this)
    }

    @Suppress("UseCheckOrError")
    suspend fun execute(
        site: SiteModel,
        productId: Int,
        domainName: String,
        isPrivacyEnabled: Boolean,
        isTemporary: Boolean
    ): OnShoppingCartCreated {
        if (continuation != null) {
            throw IllegalStateException("Cart creation is already in progress!")
        }
        return suspendCoroutine {
            continuation = it
            val payload = CreateShoppingCartPayload(site, productId, domainName, isPrivacyEnabled, isTemporary)
            dispatcher.dispatch(TransactionActionBuilder.newCreateShoppingCartAction(payload))
        }
    }

    @Subscribe
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        continuation?.resume(event)
        continuation = null
    }
}
