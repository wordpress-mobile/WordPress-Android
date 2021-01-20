package org.wordpress.android.ui.jetpack.scan.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
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
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ScanHistoryListViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var scanThreatItemBuilder: ThreatItemBuilder
    @Mock private lateinit var scanHistoryViewModel: ScanHistoryViewModel

    private lateinit var viewModel: ScanHistoryListViewModel

    private val site: SiteModel = SiteModel()

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryListViewModel(scanThreatItemBuilder, TEST_DISPATCHER)
        whenever(scanThreatItemBuilder.buildThreatItem(anyOrNull(), anyOrNull())).thenReturn(mock())
    }

    @Test
    fun `Threat ui state items shown, when the data is available`() {
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData(listOf(mock(), mock())))

        viewModel.start(site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isNotEmpty
    }
}
