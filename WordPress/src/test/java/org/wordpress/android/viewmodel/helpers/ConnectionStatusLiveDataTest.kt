package org.wordpress.android.viewmodel.helpers

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE

@ExperimentalCoroutinesApi
class ConnectionStatusLiveDataTest : BaseUnitTest() {
    private val source = MutableLiveData<Boolean>()

    @Test
    fun `it does not emit the state the source already holds when observation starts`() {
        source.value = true

        val emitted = observe()

        assertThat(emitted).isEmpty()
    }

    @Test
    fun `it emits when the connected state changes`() {
        source.value = true
        val emitted = observe()

        source.value = false

        assertThat(emitted).containsExactly(UNAVAILABLE)
    }

    @Test
    fun `it does not emit when the source repeats the same state`() {
        source.value = true
        val emitted = observe()

        repeat(3) { source.value = true }

        assertThat(emitted).isEmpty()
    }

    @Test
    fun `it emits every change in both directions`() {
        source.value = true
        val emitted = observe()

        source.value = false
        source.value = true

        assertThat(emitted).containsExactly(UNAVAILABLE, AVAILABLE)
    }

    @Test
    fun `it emits the first state when the source has none at creation`() {
        val emitted = observe()

        source.value = true

        assertThat(emitted).containsExactly(AVAILABLE)
    }

    private fun observe(): List<ConnectionStatus> {
        val emitted = mutableListOf<ConnectionStatus>()
        ConnectionStatusLiveData(source).observeForever { emitted.add(it) }
        return emitted
    }
}
