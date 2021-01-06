package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.ui.jetpack.scan.builders.ScanStateListItemsBuilder

// private const val SCAN_STATE_MODEL_PARAM_POSITION = 0

@InternalCoroutinesApi
class ScanViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStatusService: ScanStatusService
    @Mock private lateinit var scanStateItemsBuilder: ScanStateListItemsBuilder
    private val scanState = MutableLiveData<ScanStateModel>()

    private lateinit var viewModel: ScanViewModel
    /*private val scanStateModel = ScanStateModel(state = ScanStateModel.State.IDLE, hasCloud = true)

    private val threat = GenericThreatModel(
        baseThreatModel = BaseThreatModel(
            id = 1L,
            signature = "",
            description = "",
            status = ThreatStatus.CURRENT,
            firstDetected = Date(0)
        )
    )*/

    @Before
    fun setUp() {
        viewModel = ScanViewModel(scanStatusService, scanStateItemsBuilder)
//        whenever(scanStatusService.scanState).thenReturn(scanState)
    }

    @Test
    fun dummyTest() {
    }

    /*@Test
    fun `if no threats found, then on start, Content includes correct list items`() {
        // Given
        val scanStateModelWithNoThreats = scanStateModel.copy(threats = null)
        val uiStates = init(scanStateModelWithNoThreats).uiStates
        // Act
        viewModel.start(site)
        // Assert
        with((uiStates.last() as Content)) {
            assertEquals(items.size, 1)
            assertThat(items.first()).isInstanceOf(ScanState::class.java)
        }
    }

    @Test
    fun `if threats found for ScanStateModel IDLE state, then on start, Content includes correct list items`() {
        // Given
        val scanStateModelWithThreats = scanStateModel.copy(threats = listOf(threat))
        val uiStates = init(scanStateModelWithThreats).uiStates
        // Act
        viewModel.start(site)
        // Assert
        with((uiStates.last() as Content)) {
            assertEquals(items.size, 3)
            assertThat(items[0]).isInstanceOf(ScanState::class.java)
            assertThat(items[1]).isInstanceOf(ThreatsHeaderItemState::class.java)
            assertThat(items[2]).isInstanceOf(ThreatItemState::class.java)
        }
    }

    @Test
    fun `if threats found for ScanStateModel SCANNING state, then on start, Content includes correct list items`() {
        // Given
        val scanStateModelWithThreats = scanStateModel.copy(
            state = ScanStateModel.State.SCANNING,
            threats = listOf(threat)
        )
        val uiStates = init(scanStateModelWithThreats).uiStates
        // Act
        viewModel.start(site)
        // Assert
        with((uiStates.last() as Content)) {
            assertEquals(items.size, 1)
            assertThat(items.first()).isInstanceOf(ScanState::class.java)
        }
    }

    private fun init(scanStateModel: ScanStateModel): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        whenever(scanStatusService.start(site)).thenAnswer {
            scanState.postValue(scanStateModel)
        }
        whenever(scanStateItemBuilder.mapToScanState(any(), any(), any(), any())).thenAnswer {
            val model = it.getArgument(SCAN_STATE_MODEL_PARAM_POSITION) as ScanStateModel
            mapToDummyScanStateItem(model)
        }
        return Observers(uiStates)
    }

    private fun mapToDummyScanStateItem(model: ScanStateModel) = if (model.state == IDLE) {
        model.threats?.let {
            ThreatsFound(
                scanDescription = mock(),
                scanAction = ButtonAction(mock(), mock())
            )
        } ?: ThreatsNotFound(
            scanDescription = mock(),
            scanAction = ButtonAction(mock(), mock())
        )
    } else {
        ScanScanningState()
    }

    private data class Observers(val uiStates: List<UiState>)
    */
}
