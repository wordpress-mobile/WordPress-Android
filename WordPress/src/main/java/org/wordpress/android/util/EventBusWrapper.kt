package org.wordpress.android.util

import dagger.Reusable
import de.greenrobot.event.EventBus
import javax.inject.Inject

/**
 * Provides an interface for [de.greenrobot.eventbus.EventBus] which can be mocked and used in unit tests.
 */
@Reusable
class EventBusWrapper @Inject constructor() {
    fun register(subscriber: Any) {
        EventBus.getDefault().register(subscriber)
    }

    fun unregister(subscriber: Any) {
        EventBus.getDefault().unregister(subscriber)
    }
}
