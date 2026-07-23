package org.wordpress.android.networking

import android.net.Network
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class NetworkConnectionMonitorTest : BaseUnitTest() {
    private lateinit var monitor: NetworkConnectionMonitor
    private val emissions = mutableListOf<Boolean>()

    @Before
    fun setUp() {
        monitor = NetworkConnectionMonitor()
        monitor.isConnected.observeForever { emissions.add(it) }
    }

    @Test
    fun `emits connected when the first network becomes available`() {
        monitor.onNetworkAvailable(mock())

        assertThat(monitor.isConnected.value).isTrue
        assertThat(emissions).containsExactly(true)
    }

    @Test
    fun `emits disconnected when the only network is lost`() {
        val network = mock<Network>()
        monitor.onNetworkAvailable(network)
        monitor.onNetworkLost(network)

        assertThat(monitor.isConnected.value).isFalse
        assertThat(emissions).containsExactly(true, false)
    }

    @Test
    fun `does not report a disconnection during a network handover`() {
        val wifi = mock<Network>()
        val cellular = mock<Network>()

        monitor.onNetworkAvailable(wifi)
        // the replacement network arrives before the old one is torn down
        monitor.onNetworkAvailable(cellular)
        monitor.onNetworkLost(wifi)

        assertThat(monitor.isConnected.value).isTrue
        // only the initial connected emission; the handover is not a change
        assertThat(emissions).containsExactly(true)
    }

    @Test
    fun `does not emit again while the connected state is unchanged`() {
        monitor.onNetworkAvailable(mock())
        monitor.onNetworkAvailable(mock())

        assertThat(emissions).containsExactly(true)
    }

    @Test
    fun `emits again when reconnecting after a full disconnect`() {
        val first = mock<Network>()
        monitor.onNetworkAvailable(first)
        monitor.onNetworkLost(first)
        monitor.onNetworkAvailable(mock())

        assertThat(monitor.isConnected.value).isTrue
        assertThat(emissions).containsExactly(true, false, true)
    }
}
