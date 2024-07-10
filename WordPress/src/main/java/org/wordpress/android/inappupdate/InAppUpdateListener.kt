package org.wordpress.android.inappupdate

/**
 * Abstract class for handling callbacks related to in-app update events.
 *
 * Each method provides a default implementation that does nothing, allowing
 * implementers to only override the necessary methods without implementing
 * all callback methods.
 */
abstract class InAppUpdateListener {
    open fun onAppUpdateStarted(type: Int) {
        // Default empty implementation
    }

    open fun onAppUpdateDownloaded() {
        // Default empty implementation
    }

    open fun onAppUpdateInstalled() {
        // Default empty implementation
    }

    open fun onAppUpdateFailed() {
        // Default empty implementation
    }

    open fun onAppUpdateCancelled() {
        // Default empty implementation
    }

    open fun onAppUpdatePending() {
        // Default empty implementation
    }
}
