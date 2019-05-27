package org.wordpress.android.util

import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

/**
 * Provides an interface for [org.greenrobot.eventbus.EventBus] which can be mocked and used in unit tests.
 */
class EventBusAdapter @Inject constructor() {
    fun register(subscriber: Any) {
        EventBus.getDefault().register(subscriber)
    }

    fun unregister(subscriber: Any) {
        EventBus.getDefault().unregister(subscriber)
    }
}
