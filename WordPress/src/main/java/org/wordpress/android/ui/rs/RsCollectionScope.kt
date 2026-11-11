package org.wordpress.android.ui.rs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Runs the work tied to an rs list's current collections, so [reset] can cancel all of it when they
 * are torn down. A child of [parent], so it also dies with the view model.
 */
internal class RsCollectionScope(private val parent: CoroutineScope) {
    @Volatile
    private var scope = createScope()

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    /** Cancels everything launched so far; later [launch] calls run in a fresh scope. */
    fun reset() {
        scope.cancel()
        scope = createScope()
    }

    private fun createScope() = CoroutineScope(
        parent.coroutineContext + SupervisorJob(parent.coroutineContext.job)
    )
}
