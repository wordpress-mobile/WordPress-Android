@file:Suppress("DEPRECATION")

package org.wordpress.android.viewmodel.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.lifecycle.LiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
class ConnectionStatusLiveDataTest : BaseUnitTest() {
    private lateinit var connectionStatusLiveData: LiveData<ConnectionStatus>
    private lateinit var broadcastReceiver: BroadcastReceiver

    @Before
    fun setUp() {
        val captor = argumentCaptor<BroadcastReceiver>()
        val context = mock<Context> {
            on { registerReceiver(captor.capture(), any()) } doReturn mock()
        }

        connectionStatusLiveData = ConnectionStatusLiveData.Factory(context).create()
        // Start observing to capture the broadcastReceiver
        connectionStatusLiveData.observeForever { }

        broadcastReceiver = captor.firstValue
    }

    @Test
    fun `it emits a value when receiving a network info change`() {
        assertThat(connectionStatusLiveData.value).isNull()

        broadcastReceiver.onReceive(mockedBroadcastReceiverContext(connectedNetwork = false), mock())

        assertThat(connectionStatusLiveData.value).isEqualTo(ConnectionStatus.UNAVAILABLE)
    }

    @Test
    fun `it emits a value when the network availability changes`() {
        // Arrange
        broadcastReceiver.onReceive(mockedBroadcastReceiverContext(connectedNetwork = true), mock())
        assertThat(connectionStatusLiveData.value).isEqualTo(ConnectionStatus.AVAILABLE)

        // Act
        broadcastReceiver.onReceive(mockedBroadcastReceiverContext(connectedNetwork = false), mock())

        // Assert
        assertThat(connectionStatusLiveData.value).isEqualTo(ConnectionStatus.UNAVAILABLE)
    }

    @Test
    fun `it does not emit a value when the network available didn't change`() {
        // Arrange
        var emitCount = 0

        connectionStatusLiveData.observeForever {
            emitCount += 1
        }

        broadcastReceiver.onReceive(mockedBroadcastReceiverContext(connectedNetwork = true), mock())

        // Act
        repeat(3) {
            broadcastReceiver.onReceive(mockedBroadcastReceiverContext(connectedNetwork = true), mock())
        }

        // Assert
        assertThat(emitCount).isEqualTo(1)
        assertThat(connectionStatusLiveData.value).isEqualTo(ConnectionStatus.AVAILABLE)
    }

    @Suppress("DEPRECATION")
    private fun mockedBroadcastReceiverContext(connectedNetwork: Boolean): Context {
        val networkInfo = mock<NetworkInfo> {
            on { isConnected } doReturn connectedNetwork
        }
        val connectivityManager = mock<ConnectivityManager> {
            on { activeNetworkInfo } doReturn networkInfo
        }

        return mock {
            on { getSystemService(any()) } doReturn connectivityManager
        }
    }
}
