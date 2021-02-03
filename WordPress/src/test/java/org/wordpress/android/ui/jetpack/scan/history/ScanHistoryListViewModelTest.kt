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
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemLoadingSkeletonState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ThreatTestData.genericThreatModel
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel.ScanHistoryUiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel.ScanHistoryUiState.EmptyUiState.EmptyHistory
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.ALL

private const val ON_ITEM_CLICKED_PARAM_POSITION = 1

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
        whenever(scanThreatItemBuilder.buildThreatItem(anyOrNull(), anyOrNull(), anyBoolean())).thenAnswer {
            ThreatItemState(1L, true, mock(), mock(), 0, 0, 0) {
                it.getArgument<(Long) -> Unit>(ON_ITEM_CLICKED_PARAM_POSITION)(1L)
            }
        }
    }

    @Test
    fun `Loading skeletons shown, when the user opens the screen`() {
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData())

        viewModel.start(ScanHistoryTabType.ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        assertThat((viewModel.uiState.value as ContentUiState).items).allMatch { it is ThreatItemLoadingSkeletonState }
    }

    @Test
    fun `Threat ui state items shown, when the data is available`() {
        viewModel.start(ScanHistoryTabType.ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        assertThat((viewModel.uiState.value as ContentUiState).items).allMatch { it is ThreatItemState }
    }

    @Test
    fun `Only fixed threats are shown, when fixed tab is selected`() {
        viewModel.start(ScanHistoryTabType.FIXED, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(3)).buildThreatItem(captor.capture(), anyOrNull(), anyBoolean())
        assertThat(captor.allValues).allMatch { it.baseThreatModel.status == ThreatStatus.FIXED }
    }

    @Test
    fun `Only ignored threats are shown, when ignore tab is selected`() {
        viewModel.start(ScanHistoryTabType.IGNORED, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(1)).buildThreatItem(captor.capture(), anyOrNull(), anyBoolean())
        assertThat(captor.allValues).allMatch { it.baseThreatModel.status == ThreatStatus.IGNORED }
    }

    @Test
    fun `Only fixed and ignored threats are shown, when all tab is selected`() {
        viewModel.start(ScanHistoryTabType.ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(4)).buildThreatItem(captor.capture(), anyOrNull(), anyBoolean())
        assertThat(captor.allValues).allMatch {
            it.baseThreatModel.status == ThreatStatus.FIXED || it.baseThreatModel.status == ThreatStatus.IGNORED
        }
    }

    @Test
    fun `Empty screen shown, when history is empty`() {
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData(listOf()))

        viewModel.start(ScanHistoryTabType.ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(EmptyHistory::class.java)
    }

    @Test
    fun `Threat detail screen opened, when the user clicks on threat list item`() {
        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())
        viewModel.navigation.observeForever(mock())

        ((viewModel.uiState.value!! as ContentUiState).items[0] as ThreatItemState).onClick.invoke()

        assertThat(viewModel.navigation.value!!.peekContent()).isInstanceOf(ShowThreatDetails::class.java)
    }
}
