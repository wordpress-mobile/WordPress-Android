package org.wordpress.android.ui.jetpack.scan.builders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.threat.BaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState.ClickableTextInfo
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ThreatTestData
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemsBuilder
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

// TODO ashiagr add missing tests
@InternalCoroutinesApi
class ScanStateListItemsBuilderTest : BaseUnitTest() {
    private lateinit var builder: ScanStateListItemsBuilder

    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var dateProvider: DateProvider
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var threatItemBuilder: ThreatItemBuilder
    @Mock private lateinit var threatDetailsListItemsBuilder: ThreatDetailsListItemsBuilder
    @Mock private lateinit var scanStore: ScanStore
    @Mock private lateinit var onHelpClickedMock: () -> Unit

    private val baseThreatModel = BaseThreatModel(
        id = 1L,
        signature = "",
        description = "",
        status = ThreatStatus.CURRENT,
        firstDetected = Date(0)
    )
    private val threat = ThreatModel.GenericThreatModel(baseThreatModel)
    private val threats = listOf(threat)
    private val scanStateModelWithNoThreats = ScanStateModel(state = State.IDLE)
    private val scanStateModelWithThreats = scanStateModelWithNoThreats.copy(threats = threats)

    @Before
    fun setUp() {
        builder = ScanStateListItemsBuilder(
            dateProvider,
            htmlMessageUtils,
            resourceProvider,
            threatItemBuilder,
            threatDetailsListItemsBuilder,
            scanStore
        )
    }

    /* FIXING THREATS STATE */

    @Test
    fun `builds shield warning icon with error color for fixing threats state`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(IconState::class.java).first()).isEqualTo(
            IconState(
                icon = R.drawable.ic_shield_warning_white,
                colorResId = R.color.error,
                sizeResId = R.dimen.scan_icon_size,
                marginResId = R.dimen.scan_icon_margin,
                contentDescription = UiStringRes(R.string.scan_state_icon)
            )
        )
    }

    @Test
    fun `builds header for fixing threat state`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_fixing_threat_title))
        )
    }

    @Test
    fun `builds fixing threat description for fixing threat state`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_fixing_threat_description))
        )
    }

    @Test
    fun `builds progress bar for fixing threats state`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(ProgressState::class.java).first()).isEqualTo(
            ProgressState(isIndeterminate = true, isVisible = true)
        )
    }

    @Test
    fun `builds threat items for fixing threats state`() = test {
        val threatModel = ThreatTestData.fixableThreatInCurrentStatus
        val threatItemState = createDummyThreatItemState(threatModel)
        whenever(threatItemBuilder.buildThreatItem(threatModel)).thenReturn(threatItemState)
        whenever(threatDetailsListItemsBuilder.buildFixableThreatDescription(any())).thenReturn(mock())
        whenever(scanStore.getThreatModelByThreatId(any())).thenReturn(threatModel)

        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(ThreatItemState::class.java)).isNotEmpty
    }

    @Test
    fun `builds fixable description as sub header for fixing threats state threat item`() = test {
        val threatModel = ThreatTestData.fixableThreatInCurrentStatus
        val threatItemState = createDummyThreatItemState(threatModel)
        val subHeader = DescriptionState(UiStringText("sub header"))
        whenever(threatItemBuilder.buildThreatItem(threatModel)).thenReturn(threatItemState)
        whenever(threatDetailsListItemsBuilder.buildFixableThreatDescription(any())).thenReturn(subHeader)
        whenever(scanStore.getThreatModelByThreatId(any())).thenReturn(threatModel)

        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threatModel.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(ThreatItemState::class.java).first().subHeader)
            .isEqualTo(subHeader.text)
    }

    /* IDLE - THREATS FOUND STATE */

    @Test
    fun `builds clickable text info in description for scan state model with threats`() = test {
        val clickableText = "clickable text"
        val descriptionWithClickableText = "description with $clickableText"
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any()))
            .thenReturn(descriptionWithClickableText)
        whenever(resourceProvider.getString(R.string.scan_here_to_help)).thenReturn(clickableText)

        val scanStateItems = buildScanStateItems(scanStateModelWithThreats)

        val descriptionState = scanStateItems.filterIsInstance(DescriptionState::class.java).first()
        assertThat(descriptionState.clickableTextsInfo?.first()).isEqualTo(ClickableTextInfo(17, 31, onHelpClickedMock))
    }

    /* PROVISIONING STATE */

    @Test
    fun `builds shield icon with green color for provisioning scan state model`() = test {
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
    fun `builds preparing to scan header for provisioning scan state model`() = test {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_preparing_to_scan_title))
        )
    }

    @Test
    fun `builds provisioning description for provisioning scan state model`() = test {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_provisioning_description))
        )
    }

    /* INVALID STATES */

    @Test
    fun `builds empty list for unknown scan state model`() = test {
        val scanStateModelInUnknownState = scanStateModelWithNoThreats.copy(state = State.UNKNOWN)

        val scanStateItems = buildScanStateItems(scanStateModelInUnknownState)

        assertThat(scanStateItems).isEmpty()
    }

    @Test
    fun `builds empty list for unavailable scan state model`() = test {
        val scanStateModelInUnAvailableState = scanStateModelWithNoThreats.copy(state = State.UNAVAILABLE)

        val scanStateItems = buildScanStateItems(scanStateModelInUnAvailableState)

        assertThat(scanStateItems).isEmpty()
    }

    /* SCANNING STATE */

    @Test
    fun `builds initial scanning description for scanning scan state model with no initial recent scan`() = test {
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
    fun `builds scanning description for scanning scan state model with past recent scan`() = test {
        val scanStateModelInScanningInitialState = scanStateModelWithNoThreats.copy(
            state = State.SCANNING,
            mostRecentStatus = ScanProgressStatus(isInitial = false, startDate = Date(0))
        )

        val scanStateItems = buildScanStateItems(scanStateModelInScanningInitialState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_scanning_description))
        )
    }

    private suspend fun buildScanStateItems(
        model: ScanStateModel,
        fixingThreatIds: List<Long> = emptyList()
    ) = builder.buildScanStateListItems(
        model = model,
        site = site,
        fixingThreatIds = fixingThreatIds,
        onScanButtonClicked = mock(),
        onFixAllButtonClicked = mock(),
        onThreatItemClicked = mock(),
        onHelpClicked = onHelpClickedMock
    )

    private fun createDummyThreatItemState(threatModel: ThreatModel) = ThreatItemState(
        threatId = threatModel.baseThreatModel.id,
        header = UiStringText(""),
        subHeader = UiStringText(""),
        subHeaderColor = 0,
        icon = 0,
        iconBackground = 0,
        onClick = {}
    )
}
