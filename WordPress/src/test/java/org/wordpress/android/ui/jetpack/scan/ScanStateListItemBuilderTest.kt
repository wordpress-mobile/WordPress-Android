package org.wordpress.android.ui.jetpack.scan

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.viewmodel.ResourceProvider

// private const val DUMMY_CURRENT_TIME = 10000000L
// private const val ONE_MINUTE = 60 * 1000L
// private const val ONE_HOUR = 60 * ONE_MINUTE

@InternalCoroutinesApi
class ScanStateListItemBuilderTest : BaseUnitTest() {
    private lateinit var builder: ScanStateListItemBuilder

//    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var dateProvider: DateProvider
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var resourceProvider: ResourceProvider

//    private val baseThreatModel = BaseThreatModel(
//        id = 1L,
//        signature = "",
//        description = "",
//        status = ThreatModel.ThreatStatus.CURRENT,
//        firstDetected = Date(0)
//    )
//    private val threat = ThreatModel.GenericThreatModel(baseThreatModel)
//    private val threats = listOf(threat)
//    private val scanStateModelWithNoThreats = ScanStateModel(state = ScanStateModel.State.IDLE, hasCloud = true)
//    private val scanStateModelWithThreats = scanStateModelWithNoThreats.copy(threats = threats)

    @Before
    fun setUp() {
        builder = ScanStateListItemBuilder(dateProvider, htmlMessageUtils, resourceProvider)
//        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any())).thenReturn(SpannedString(""))
//        whenever(site.name).thenReturn((""))
//        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
    }

    @Test
    fun dummyTest() {
    }

    /*@Test
    fun `returns ThreatsFound item for ScanStateModel IDLE state with threats`() {
        // Act
        val scanState = mapToScanState(scanStateModelWithThreats)
        // Assert
        assertThat(scanState).isInstanceOf(ScanIdleState.ThreatsFound::class.java)
    }

    @Test
    fun `returns ThreatsNotFound item for ScanStateModel IDLE state with no threats`() {
        // Act
        val scanState = mapToScanState(scanStateModelWithNoThreats)
        // Assert
        assertThat(scanState).isInstanceOf(ScanIdleState.ThreatsNotFound::class.java)
    }

    @Test
    fun `returns ScanScanningState item for ScanStateModel SCANNING state`() {
        // Given
        val modelWithScanningState = ScanStateModel(state = ScanStateModel.State.SCANNING, hasCloud = true)
        // Act
        val scanState = mapToScanState(modelWithScanningState)
        // Assert
        assertThat(scanState).isInstanceOf(ScanScanningState::class.java)
    }

    @Test
    fun `builds scan again button for ThreatsFound`() {
        // Act
        val scanState = mapToScanState(scanStateModelWithThreats)
        // Assert
        with(scanState as ScanIdleState.ThreatsFound) {
            assertThat(scanAction.visibility).isTrue
            assertThat((scanAction.title as UiStringRes).stringRes).isEqualTo(R.string.scan_again)
        }
    }

    @Test
    fun `builds scan now button for ThreatsNotFound`() {
        // Act
        val scanState = mapToScanState(scanStateModelWithNoThreats)
        // Assert
        with((scanState as ScanIdleState.ThreatsNotFound).scanAction) {
            assertThat(visibility).isTrue
            assertThat((title as UiStringRes).stringRes).isEqualTo(R.string.scan_now)
        }
    }

    @Test
    fun `builds fix all button for ThreatsFound with fixable threats`() {
        // Given
        val threats = listOf(threat.copy(baseThreatModel = baseThreatModel.copy(fixable = mock())))
        val scanStateModelWithFixableThreats = scanStateModelWithThreats.copy(threats = threats)
        // Act
        val threatsFoundState = mapToScanState(scanStateModelWithFixableThreats)
        // Assert
        assertThat(threatsFoundState.fixAllAction?.visibility).isTrue
    }

    @Test
    fun `does not builds fix all button for ThreatsFound with no fixable threats`() {
        // Given
        val threats = listOf(threat.copy(baseThreatModel = baseThreatModel.copy(fixable = null)))
        val scanStateModelWithNoFixableThreats = scanStateModelWithThreats.copy(threats = threats)
        // Act
        val threatsFoundState = mapToScanState(scanStateModelWithNoFixableThreats)
        // Assert
        assertThat(threatsFoundState.fixAllAction).isNull()
    }

    @Test
    fun `builds last scan done seconds ago description for ThreatsNotFound`() {
        // Given
        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
        val scanStateModelWithNoThreats = scanStateModelWithNoThreats.copy(
            mostRecentStatus = ScanProgressStatus(startDate = Date(DUMMY_CURRENT_TIME - 10))
        )
        // Act
        val threatsNotFoundState = mapToScanState(scanStateModelWithNoThreats)
        // Assert
        val scanDescription = threatsNotFoundState.scanDescription as UiStringResWithParams
        assertThat(scanDescription.stringRes).isEqualTo(R.string.scan_idle_last_scan_description)
        assertThat(scanDescription.params.size).isEqualTo(2)
        assertThat((scanDescription.params[0] as UiStringRes).stringRes).isEqualTo(R.string.scan_in_few_seconds)
        assertThat((scanDescription.params[1] as UiStringRes).stringRes)
            .isEqualTo(R.string.scan_idle_manual_scan_description)
    }

    @Test
    fun `builds last scan done hours ago description with hours ago substring for ThreatsNotFound`() {
        // Given
        val scanStateModelWithNoThreats = scanStateModelWithNoThreats.copy(
            mostRecentStatus = ScanProgressStatus(startDate = Date(DUMMY_CURRENT_TIME - ONE_HOUR))
        )
        // Act
        val threatsNotFoundState = mapToScanState(scanStateModelWithNoThreats)
        // Assert
        val scanDescription = threatsNotFoundState.scanDescription as UiStringResWithParams
        assertThat(scanDescription.stringRes).isEqualTo(R.string.scan_idle_last_scan_description)
        assertThat((scanDescription.params[0] as UiStringResWithParams).stringRes).isEqualTo(R.string.scan_in_hours_ago)
    }

    @Test
    fun `builds last scan done minutes ago description with minutes ago substring for ThreatsNotFound`() {
        // Given
        val scanStateModelWithNoThreats = scanStateModelWithNoThreats.copy(
            mostRecentStatus = ScanProgressStatus(startDate = Date(DUMMY_CURRENT_TIME - ONE_MINUTE))
        )
        // Act
        val threatsNotFoundState = mapToScanState(scanStateModelWithNoThreats)
        // Assert
        val scanDescription = threatsNotFoundState.scanDescription as UiStringResWithParams
        assertThat(scanDescription.stringRes).isEqualTo(R.string.scan_idle_last_scan_description)
        assertThat((scanDescription.params[0] as UiStringResWithParams).stringRes)
            .isEqualTo(R.string.scan_in_minutes_ago)
    }

    @Test
    fun `builds threats found description for ThreatsFound`() {
        // Act
        mapToScanState(scanStateModelWithThreats)
        // Assert
        verify(htmlMessageUtils).getHtmlMessageFromStringFormatResId(
            R.string.scan_idle_threats_found_description,
            "<b>${threats.size}</b>",
            "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>"
        )
    }

    private fun mapToScanState(
        model: ScanStateModel
    ) = builder.buildScanStateListItems(
        model = model,
        site = site,
        onScanButtonClicked = mock(),
        onFixAllButtonClicked = mock()
    )*/
}
