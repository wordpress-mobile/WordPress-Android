package org.wordpress.android.ui.jetpack.scan.builders

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.IDLE
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

// private const val DUMMY_CURRENT_TIME = 10000000L
// private const val ONE_MINUTE = 60 * 1000L
// private const val ONE_HOUR = 60 * ONE_MINUTE

// TODO ashiagr tweak existing tests
@InternalCoroutinesApi
class ScanStateListItemsBuilderTest : BaseUnitTest() {
    private lateinit var builder: ScanStateListItemsBuilder

    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var dateProvider: DateProvider
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var threatItemBuilder: ThreatItemBuilder
    @Mock private lateinit var uiHelpers: UiHelpers
    @Mock private lateinit var contextProvider: ContextProvider

//    private val baseThreatModel = BaseThreatModel(
//        id = 1L,
//        signature = "",
//        description = "",
//        status = ThreatModel.ThreatStatus.CURRENT,
//        firstDetected = Date(0)
//    )
//    private val threat = ThreatModel.GenericThreatModel(baseThreatModel)
//    private val threats = listOf(threat)
    private val scanStateModelWithNoThreats = ScanStateModel(state = IDLE)
//    private val scanStateModelWithThreats = scanStateModelWithNoThreats.copy(threats = threats)

    @Before
    fun setUp() {
        builder = ScanStateListItemsBuilder(
            dateProvider,
            htmlMessageUtils,
            resourceProvider,
            threatItemBuilder,
            uiHelpers,
            contextProvider
        )
//        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any())).thenReturn(SpannedString(""))
//        whenever(site.name).thenReturn((""))
//        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
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
    }*/

    @Test
    fun `builds shield icon with green color for provisioning scan state model`() {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(IconState::class.java).first()).isEqualTo(
            IconState(
                icon = R.drawable.ic_shield_white,
                colorResId = R.color.jetpack_green_5,
                sizeResId = R.dimen.scan_icon_size,
                marginResId = R.dimen.scan_icon_margin,
                contentDescription = UiStringRes(R.string.scan_state_icon)
            )
        )
    }

    @Test
    fun `builds preparing to scan header for provisioning scan state model`() {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_preparing_to_scan_title))
        )
    }

    @Test
    fun `builds provisioning description for provisioning scan state model`() {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_provisioning_description))
        )
    }

    @Test
    fun `builds empty list for unknown scan state model`() {
        val scanStateModelInUnknownState = scanStateModelWithNoThreats.copy(state = State.UNKNOWN)

        val scanStateItems = buildScanStateItems(scanStateModelInUnknownState)

        assertThat(scanStateItems).isEmpty()
    }

    @Test
    fun `builds empty list for unavailable scan state model`() {
        val scanStateModelInUnAvailableState = scanStateModelWithNoThreats.copy(state = State.UNAVAILABLE)

        val scanStateItems = buildScanStateItems(scanStateModelInUnAvailableState)

        assertThat(scanStateItems).isEmpty()
    }

    @Test
    fun `builds initial scanning description for scanning scan state model with no initial recent scan`() {
        val scanStateModelInScanningInitialState = scanStateModelWithNoThreats.copy(
            state = State.SCANNING,
            mostRecentStatus = ScanProgressStatus(isInitial = true, startDate = Date(0))
        )

        val scanStateItems = buildScanStateItems(scanStateModelInScanningInitialState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_scanning_is_initial_description))
        )
    }

    @Test
    fun `builds scanning description for scanning scan state model with past recent scan`() {
        val scanStateModelInScanningInitialState = scanStateModelWithNoThreats.copy(
            state = State.SCANNING,
            mostRecentStatus = ScanProgressStatus(isInitial = false, startDate = Date(0))
        )

        val scanStateItems = buildScanStateItems(scanStateModelInScanningInitialState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_scanning_description))
        )
    }

    private fun buildScanStateItems(
        model: ScanStateModel
    ) = builder.buildScanStateListItems(
        model = model,
        site = site,
        onScanButtonClicked = mock(),
        onFixAllButtonClicked = mock(),
        onThreatItemClicked = mock()
    )
}
