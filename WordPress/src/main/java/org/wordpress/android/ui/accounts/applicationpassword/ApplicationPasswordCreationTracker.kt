package org.wordpress.android.ui.accounts.applicationpassword

/**
 * Holds the creation source for the application password flow.
 * Since the web authorization goes through Chrome Custom Tabs,
 * we cannot pass extras through the intent. This singleton
 * bridges the source from the caller to the ViewModel that
 * handles the callback.
 */
object ApplicationPasswordCreationTracker {
    private const val DEFAULT_SOURCE = "login"

    var pendingCreationSource: String = DEFAULT_SOURCE
        private set

    fun setPendingCreationSource(source: String) {
        pendingCreationSource = source
    }

    fun consumePendingCreationSource(): String {
        val source = pendingCreationSource
        pendingCreationSource = DEFAULT_SOURCE
        return source
    }
}
