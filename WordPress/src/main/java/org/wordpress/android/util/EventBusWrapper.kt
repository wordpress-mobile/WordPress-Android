package org.wordpress.android.util

import dagger.Reusable
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

/**
 * Provides an interface for [org.greenrobot.eventbus.EventBus] which can be mocked and used in unit tests.
 */
@Reusable
class EventBusWrapper @Inject constructor() {
    fun register(subscriber: Any) {
        EventBus.getDefault().register(subscriber)
    }

    fun unregister(subscriber: Any) {
        EventBus.getDefault().unregister(subscriber)
    }

    fun removeStickyEvent(event: Any) {
        EventBus.getDefault().removeStickyEvent(event)
    }

    fun isRegistered(subscriber: Any): Boolean {
        return EventBus.getDefault().isRegistered(subscriber)
    }

    fun postSticky(event: Any) {
        EventBus.getDefault().postSticky(event)
    }

    fun post(event: Any) {
        EventBus.getDefault().post(event)
    }
}
