package org.wordpress.android.ui.jetpack.scan.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.OnScanHistoryFetched
import org.wordpress.android.test
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ScanHistoryViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var scanStore: ScanStore
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private val site: SiteModel = SiteModel()

    private lateinit var viewModel: ScanHistoryViewModel

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryViewModel(scanStore, networkUtilsWrapper, TEST_DISPATCHER)
    }

    @Test
    fun `Threats loaded, when the user opens the screen`() = test {
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(OnScanHistoryFetched(1L, mock()))
        whenever(scanStore.getScanHistoryForSite(anyOrNull())).thenReturn(listOf(mock()))

        viewModel.start(site)

        assertThat(viewModel.threats.value!!.size).isEqualTo(1)
    }
}
