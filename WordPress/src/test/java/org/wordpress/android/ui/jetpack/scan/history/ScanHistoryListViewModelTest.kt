package org.wordpress.android.ui.jetpack.scan.history

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatDateItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemLoadingSkeletonState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ThreatTestData.genericThreatModel
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel.ScanHistoryUiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel.ScanHistoryUiState.EmptyUiState.EmptyHistory
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.ALL
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.ScanTracker
import org.wordpress.android.util.extensions.toFormattedDateString
import java.util.Calendar

private const val ON_ITEM_CLICKED_PARAM_POSITION = 1

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ScanHistoryListViewModelTest : BaseUnitTest() {
    @Mock private lateinit var scanThreatItemBuilder: ThreatItemBuilder
    @Mock private lateinit var scanHistoryViewModel: ScanHistoryViewModel
    @Mock private lateinit var scanTracker: ScanTracker
    private val captor = argumentCaptor<ThreatModel>()

    private lateinit var viewModel: ScanHistoryListViewModel

    private val site: SiteModel = SiteModel()

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryListViewModel(
                scanThreatItemBuilder,
                scanTracker,
                TEST_DISPATCHER
        )
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
            ThreatItemState(1L, true, mock(), mock(), mock(), 0, 0, 0) {
                it.getArgument<(Long) -> Unit>(ON_ITEM_CLICKED_PARAM_POSITION)(1L)
            }
        }
    }

    @Test
    fun `Loading skeletons shown, when the user opens the screen`() {
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData())

        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        assertThat((viewModel.uiState.value as ContentUiState).items).allMatch { it is ThreatItemLoadingSkeletonState }
    }

    @Test
    fun `Threat ui state items shown, when the data is available`() {
        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        assertThat((viewModel.uiState.value as ContentUiState).items).allMatch {
            it is ThreatDateItemState || it is ThreatItemState
        }
    }

    @Test
    fun `Threat date is shown as first item, when data is available`() {
        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        assertThat((viewModel.uiState.value as ContentUiState).items.first())
                .isInstanceOf(ThreatDateItemState::class.java)
    }

    @Test
    fun `Threat date is shown again, when the date is different`() {
        // assign
        val calendar = Calendar.getInstance()
        val threats = listOf(
                GenericThreatModel(
                        genericThreatModel.baseThreatModel.copy(
                                firstDetected = calendar.time,
                                status = ThreatStatus.FIXED
                        )
                ),

                GenericThreatModel(
                        genericThreatModel.baseThreatModel.copy(
                                firstDetected = calendar.apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }.time,
                                status = ThreatStatus.FIXED
                        )
                ),
                GenericThreatModel(
                        genericThreatModel.baseThreatModel.copy(
                                firstDetected = calendar.time,
                                status = ThreatStatus.FIXED
                        )
                ),
                GenericThreatModel(
                        genericThreatModel.baseThreatModel.copy(
                                firstDetected = calendar.apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }.time,
                                status = ThreatStatus.FIXED
                        )
                ),
                GenericThreatModel(
                        genericThreatModel.baseThreatModel.copy(
                                firstDetected = calendar.apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }.time,
                                status = ThreatStatus.FIXED
                        )
                )
        )
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData(threats))

        whenever(scanThreatItemBuilder.buildThreatItem(anyOrNull(), anyOrNull(), anyBoolean())).thenAnswer {
            val threat = it.arguments[0] as GenericThreatModel
            val date = threat.baseThreatModel.firstDetected.toFormattedDateString()
            ThreatItemState(1L, true, UiStringText(date), mock(), mock(), 0, 0, 0) {
                it.getArgument<(Long) -> Unit>(ON_ITEM_CLICKED_PARAM_POSITION)(1L)
            }
        }

        // act
        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        // asset
        assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        val dateItemStates = (viewModel.uiState.value as ContentUiState)
                .items
                .filterIndexed { index, _ ->
                    index == 0 || index == 2 || index == 5 || index == 7
                }
        assertThat(dateItemStates)
                .allMatch { it is ThreatDateItemState }
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
        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        verify(scanThreatItemBuilder, times(4)).buildThreatItem(captor.capture(), anyOrNull(), anyBoolean())
        assertThat(captor.allValues).allMatch {
            it.baseThreatModel.status == ThreatStatus.FIXED || it.baseThreatModel.status == ThreatStatus.IGNORED
        }
    }

    @Test
    fun `Empty screen shown, when history is empty`() {
        whenever(scanHistoryViewModel.threats).thenReturn(MutableLiveData(listOf()))

        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())

        assertThat(viewModel.uiState.value).isInstanceOf(EmptyHistory::class.java)
    }

    @Test
    fun `Threat detail screen opened, when the user clicks on threat list item`() {
        viewModel.start(ALL, site, scanHistoryViewModel)
        viewModel.uiState.observeForever(mock())
        viewModel.navigation.observeForever(mock())

        // first item is date header
        ((viewModel.uiState.value!! as ContentUiState).items[1] as ThreatItemState).onClick.invoke()

        assertThat(viewModel.navigation.value!!.peekContent()).isInstanceOf(ShowThreatDetails::class.java)
    }
}
