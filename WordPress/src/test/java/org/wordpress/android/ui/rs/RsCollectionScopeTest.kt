package org.wordpress.android.ui.rs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class RsCollectionScopeTest : BaseUnitTest(StandardTestDispatcher()) {
    @Test
    fun `reset cancels work launched before it`() = test {
        val scope = RsCollectionScope(testScope())
        val job = scope.launch { CompletableDeferred<Unit>().await() }
        advanceUntilIdle()

        scope.reset()

        assertThat(job.isCancelled).isTrue
    }

    @Test
    fun `work launched after a reset still runs`() = test {
        val scope = RsCollectionScope(testScope())
        scope.reset()
        var ran = false

        scope.launch { ran = true }
        advanceUntilIdle()

        assertThat(ran).isTrue
    }
}
