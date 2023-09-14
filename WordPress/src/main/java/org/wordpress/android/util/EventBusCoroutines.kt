package org.wordpress.android.util

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import kotlin.coroutines.resume
import kotlin.reflect.KClass

suspend inline fun <PAYLOAD, reified EVENT : Any> Dispatcher.dispatchAndAwait(
    action: Action<PAYLOAD>
): EVENT = coroutineScope {
    val deferred = async { awaitEvent<EVENT>() }
    dispatch(action)

    return@coroutineScope deferred.await()
}

suspend inline fun <reified EVENT : Any> Dispatcher.awaitEvent(): EVENT = suspendCancellableCoroutine { continuation ->
    val listener = object : GenericEventBusListener<EVENT>(EVENT::class) {
        override fun handleEvent(event: EVENT) {
            unregister(this)
            if (!continuation.isActive) {
                AppLog.w(AppLog.T.UTILS, "Listener for ${EVENT::class} invoked after cancellation")
                return
            }
            continuation.resume(event)
        }
    }
    register(listener)

    continuation.invokeOnCancellation {
        unregister(listener)
    }
}

inline fun <reified EVENT : Any> Dispatcher.observeEvents(): Flow<EVENT> = callbackFlow {
    val listener = object : GenericEventBusListener<EVENT>(EVENT::class) {
        override fun handleEvent(event: EVENT) {
            trySend(event)
                .onFailure {
                    AppLog.w(AppLog.T.UTILS, "Failure to emit EventBus's event $event as Flow, ${it?.toString()}")
                }
        }
    }
    register(listener)

    awaitClose {
        unregister(listener)
    }
}

abstract class GenericEventBusListener<EVENT : Any>(private val kClass: KClass<EVENT>) {
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onEvent(event: EVENT) {
        // Since generic types are suppressed at runtime, this listener will be registered using the type Object
        // But using the reified EVENT type, we can compare the class and ignore unwanted events
        if (event::class != kClass) return

        handleEvent(event)
    }

    abstract fun handleEvent(event: EVENT)
}
