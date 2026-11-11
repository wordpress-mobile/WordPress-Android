package org.wordpress.android.ui.accounts.applicationpassword

import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the creation source for the application password flow.
 * Since the web authorization goes through Chrome Custom Tabs,
 * we cannot pass extras through the intent. This singleton
 * bridges the source from the caller to the ViewModel that
 * handles the callback.
 */
object ApplicationPasswordCreationTracker {
    const val SOURCE_LOGIN = "login"
    const val SOURCE_AUTO_MIGRATION = "auto_migration"
    const val SOURCE_MIGRATION = "migration"
    const val SOURCE_REAUTH = "reauth"

    private const val DEFAULT_SOURCE = SOURCE_LOGIN

    private val pendingCreationSource =
        AtomicReference(DEFAULT_SOURCE)

    fun setPendingCreationSource(source: String) {
        pendingCreationSource.set(source)
    }

    fun consumePendingCreationSource(): String =
        pendingCreationSource.getAndSet(DEFAULT_SOURCE)
}
