package org.wordpress.android.ui.rs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Where an rs list runs the work that belongs to its current set of collections - init, observers,
 * refresh, paging and the per-row metrics - so [reset] can cancel all of it at once when those
 * collections are torn down. Without that, an init or refresh that outlives a filter or search
 * change could install a collection nothing closes, or write stale state into the rebuilt tabs.
 *
 * A child of [parent], so it is also torn down with the view model. One stable object rather than
 * a scope that gets swapped, so collaborators can hold it for the view model's lifetime.
 */
internal class RsCollectionScope(private val parent: CoroutineScope) {
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
