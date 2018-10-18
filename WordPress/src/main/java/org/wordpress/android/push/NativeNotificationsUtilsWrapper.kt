package org.wordpress.android.push

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around NativeNotificationsUtilsWrapper.
 *
 * NativeNotificationsUtilsWrapper interface is consisted of static methods, which make the client code difficult to
 * test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Singleton
class NativeNotificationsUtilsWrapper @Inject constructor() {
    fun extrasContainValid2FaToken(intent: Intent) = NativeNotificationsUtils.extrasContainValid2FaToken(intent)

    fun retrieve2FATokenFromIntentExtras(intent: Intent): String =
            NativeNotificationsUtils.retrieve2FATokenFromIntentExtras(intent)
}
