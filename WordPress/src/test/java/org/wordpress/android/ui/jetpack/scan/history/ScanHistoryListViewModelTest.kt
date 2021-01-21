package org.wordpress.android.ui.jetpack.scan.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.ThreatTestData.genericThreatModel
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ScanHistoryListViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var scanThreatItemBuilder: ThreatItemBuilder
    @Mock private lateinit var scanHistoryViewModel: ScanHistoryViewModel
    private val captor = argumentCaptor<ThreatModel>()

    private lateinit var viewModel: ScanHistoryListViewModel

    private val site: SiteModel = SiteModel()

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryListViewModel(scanThreatItemBuilder, TEST_DISPATCHER)
        val threats = listOf(
                GenericThreatModel(genericThreatModel.baseThreatModel.copy(status = ThreatStatus.FIXED)),
                GenericThreatModel(genericThreatModel.baseThreatModel.copy(status = ThreatStatus.UNKNOWN)),
                GenericThreatModel(genericThreatModel.baseThreatModel.copy(status = ThreatStatus.FIXED)),
                GenericThreatModel(genericThreatModel.baseThreatModel.copy(status = ThreatStatus.IGNORED)),
                GenericThreatModel(genericThreatModel.baseThreatModel.copy(status = ThreatStatus.FIXED)),
                GenericThreatModel(genericThreatModel.baseThreatModel.copy(status = ThreatStatus.CURRENT))
        )
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData(threats))
    }

    @Test
    fun `Threat ui state items shown, when the data is available`() {
        viewModel.start(ScanHistoryTabType.ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isNotEmpty
    }

    @Test
    fun `Only fixed threats are shown, when fixed tab is selected`() {
        viewModel.start(ScanHistoryTabType.FIXED, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(3)).buildThreatItem(captor.capture(), anyOrNull())
        assertThat(captor.allValues).allMatch { it.baseThreatModel.status == ThreatStatus.FIXED }
    }

    @Test
    fun `Only ignored threats are shown, when ignore tab is selected`() {
        viewModel.start(ScanHistoryTabType.IGNORED, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(1)).buildThreatItem(captor.capture(), anyOrNull())
        assertThat(captor.allValues).allMatch { it.baseThreatModel.status == ThreatStatus.IGNORED }
    }

    @Test
    fun `Only fixed and ignored threats are shown, when all tab is selected`() {
        viewModel.start(ScanHistoryTabType.ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(4)).buildThreatItem(captor.capture(), anyOrNull())
        assertThat(captor.allValues).allMatch {
            it.baseThreatModel.status == ThreatStatus.FIXED || it.baseThreatModel.status == ThreatStatus.IGNORED
        }
    }
}
