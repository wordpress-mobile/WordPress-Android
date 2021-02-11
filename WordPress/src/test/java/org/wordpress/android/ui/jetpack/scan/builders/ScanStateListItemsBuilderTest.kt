package org.wordpress.android.ui.jetpack.scan.builders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
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
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
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
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

private const val DUMMY_CURRENT_TIME = 10000000L
private const val ONE_MINUTE = 60 * 1000L
private const val ONE_HOUR = 60 * ONE_MINUTE

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
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any())).thenReturn("")
        whenever(resourceProvider.getString(R.string.scan_here_to_help)).thenReturn("")
        whenever(site.name).thenReturn((""))
        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
    }

    /* FIXING THREATS STATE */

    @Test
    fun `given fixing threats state, when items are built, items contain shield warning icon with error color`() =
        test {
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
    fun `given fixing threats state, when items are built, items contain fixing threats header`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_fixing_threats_title))
        )
    }

    @Test
    fun `given fixing threats state, when items are built, items contain fixing threats description`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_fixing_threats_description))
        )
    }

    @Test
    fun `given fixing threats state, when items are built, items contain indeterminate progress bar`() = test {
        val scanStateItems = buildScanStateItems(
            model = scanStateModelWithThreats,
            fixingThreatIds = listOf(threat.baseThreatModel.id)
        )

        assertThat(scanStateItems.filterIsInstance(ProgressState::class.java).first()).isEqualTo(
            ProgressState(isIndeterminate = true, isVisible = true)
        )
    }

    @Test
    fun `given fixing threats state, when items are built, items contain fixing threats`() = test {
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
    fun `given fixing threats state, when items are built, items contain threats with fixable description subheader`() =
        test {
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
    fun `given idle state with threats, when items are built, items contain shield warning icon with error color`() =
        test {
            val scanStateItems = buildScanStateItems(scanStateModelWithThreats)

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
    fun `given idle state with threats, when items are built, items contain header for threats found`() = test {
        val scanStateItems = buildScanStateItems(model = scanStateModelWithThreats)

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_idle_threats_found_title))
        )
    }

    @Test
    fun `given idle state with threats, when items are built, items contain threats found description`() = test {
        buildScanStateItems(scanStateModelWithThreats)

        verify(htmlMessageUtils).getHtmlMessageFromStringFormatResId(
            R.string.scan_idle_with_threats_description,
            "<b>${threats.size}</b>",
            "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>",
            resourceProvider.getString(R.string.scan_here_to_help)
        )
    }

    @Test
    fun `given idle state with threats, when items are built, items contain scan again button`() = test {
        val scanStateItems = buildScanStateItems(scanStateModelWithThreats)

        assertThat(scanStateItems.filterIsInstance(ActionButtonState::class.java).map { it.text }).contains(
            UiStringRes(R.string.scan_again)
        )
    }

    @Test
    fun `given idle state with fixable threats, when items are built, items contain fix all threats button`() = test {
        val threats = listOf(threat.copy(baseThreatModel = baseThreatModel.copy(fixable = mock())))
        val scanStateModelWithFixableThreats = scanStateModelWithThreats.copy(threats = threats)

        val scanStateItems = buildScanStateItems(scanStateModelWithFixableThreats)

        assertThat(scanStateItems.filterIsInstance(ActionButtonState::class.java).map { it.text }).contains(
            UiStringRes(R.string.threats_fix_all)
        )
    }

    @Test
    fun `given idle state with not fixable threats, when items are built, items contain fix all threats button`() =
        test {
            val threats = listOf(threat.copy(baseThreatModel = baseThreatModel.copy(fixable = null)))
            val scanStateModelWithNoFixableThreats = scanStateModelWithThreats.copy(threats = threats)

            val scanStateItems = buildScanStateItems(scanStateModelWithNoFixableThreats)

            assertThat(
                scanStateItems.filterIsInstance(ActionButtonState::class.java)
                    .map { it.text }
            ).doesNotContain(
                UiStringRes(R.string.threats_fix_all)
            )
        }

    @Test
    fun `given idle state with threats, when items are built, items contain clickable help text info in description`() =
        test {
            val clickableText = "clickable help text"
            val descriptionWithClickableText = "description with $clickableText"
            whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any()))
                .thenReturn(descriptionWithClickableText)
            whenever(resourceProvider.getString(R.string.scan_here_to_help)).thenReturn(clickableText)

            val scanStateItems = buildScanStateItems(scanStateModelWithThreats)

            val descriptionState = scanStateItems.filterIsInstance(DescriptionState::class.java).first()
            assertThat(descriptionState.clickableTextsInfo?.first()).isEqualTo(
                ClickableTextInfo(
                    17,
                    36,
                    onHelpClickedMock
                )
            )
        }

    /* IDLE - THREATS NOT FOUND STATE */

    @Test
    fun `given idle no threats state, when items are built, items contain shield tick icon with green color`() =
        test {
            val scanStateItems = buildScanStateItems(scanStateModelWithNoThreats)

            assertThat(scanStateItems.filterIsInstance(IconState::class.java).first()).isEqualTo(
                IconState(
                    icon = R.drawable.ic_shield_tick_white,
                    colorResId = R.color.jetpack_green_40,
                    sizeResId = R.dimen.scan_icon_size,
                    marginResId = R.dimen.scan_icon_margin,
                    contentDescription = UiStringRes(R.string.scan_state_icon)
                )
            )
        }

    @Test
    fun `given idle no threats state, when items are built, items contain header for no threats found`() = test {
        val scanStateItems = buildScanStateItems(model = scanStateModelWithNoThreats)

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_idle_no_threats_found_title))
        )
    }

    @Test
    fun `given idle no threats state, when items are built, items contain scan now button `() = test {
        val scanStateItems = buildScanStateItems(scanStateModelWithNoThreats)

        assertThat(scanStateItems.filterIsInstance(ActionButtonState::class.java).map { it.text }).contains(
            UiStringRes(R.string.scan_now)
        )
    }

    @Test
    fun `given idle no threats state with last scan secs ago, when items are built, items contain few secs in descr`() =
        test {
            whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
            val scanStateModelWithMostRecentStartDate = scanStateModelWithNoThreats.copy(
                mostRecentStatus = ScanProgressStatus(startDate = Date(DUMMY_CURRENT_TIME - 10))
            )
            val scanStateItems = buildScanStateItems(scanStateModelWithMostRecentStartDate)

            assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).map { it.text }).contains(
                UiStringResWithParams(
                    R.string.scan_idle_last_scan_description,
                    listOf(
                        UiStringRes(R.string.scan_in_few_seconds),
                        UiStringRes(R.string.scan_idle_manual_scan_description)
                    )
                )
            )
        }

    @Test
    fun `given idle no threats state with last scan hrs ago, when items are built, items contain hrs ago in descr`() =
        test {
            val scanStateModelWithMostRecentStartDate = scanStateModelWithNoThreats.copy(
                mostRecentStatus = ScanProgressStatus(startDate = Date(DUMMY_CURRENT_TIME - ONE_HOUR))
            )

            val scanStateItems = buildScanStateItems(scanStateModelWithMostRecentStartDate)

            assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).map { it.text }).contains(
                UiStringResWithParams(
                    R.string.scan_idle_last_scan_description,
                    listOf(
                        UiStringResWithParams(R.string.scan_in_hours_ago, listOf(UiStringText("1"))),
                        UiStringRes(R.string.scan_idle_manual_scan_description)
                    )
                )
            )
        }

    @Test
    fun `given idle no threats state with last scan mins ago, when items are built, items contain mins ago in descr`() =
        test {
            val scanStateModelWithMostRecentStartDate = scanStateModelWithNoThreats.copy(
                mostRecentStatus = ScanProgressStatus(startDate = Date(DUMMY_CURRENT_TIME - ONE_MINUTE))
            )

            val scanStateItems = buildScanStateItems(scanStateModelWithMostRecentStartDate)

            assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).map { it.text }).contains(
                UiStringResWithParams(
                    R.string.scan_idle_last_scan_description,
                    listOf(
                        UiStringResWithParams(
                            R.string.scan_in_minutes_ago,
                            listOf(UiStringText("1"))
                        ),
                        UiStringRes(R.string.scan_idle_manual_scan_description)
                    )
                )
            )
        }

    /* SCANNING STATE */

    @Test
    fun `given scanning state, when items are built, items contain shield icon with light green color`() =
        test {
            val scanStateModelInScanningState = scanStateModelWithNoThreats.copy(state = State.SCANNING)

            val scanStateItems = buildScanStateItems(scanStateModelInScanningState)

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
    fun `given scanning state with zero progress, when items are built, items contain preparing for scan header`() =
        test {
            val scanStateModelInScanningState = scanStateModelWithNoThreats.copy(
                state = State.SCANNING,
                currentStatus = ScanProgressStatus(progress = 0, startDate = Date(0))
            )

            val scanStateItems = buildScanStateItems(scanStateModelInScanningState)

            assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
                HeaderState(UiStringRes(R.string.scan_preparing_to_scan_title))
            )
        }

    @Test
    fun `given scanning state with non zero progress, when items are built, items contain scanning files header`() =
        test {
            val scanStateModelInScanningState = scanStateModelWithNoThreats.copy(
                state = State.SCANNING,
                currentStatus = ScanProgressStatus(progress = 10, startDate = Date(0))
            )

            val scanStateItems = buildScanStateItems(scanStateModelInScanningState)

            assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
                HeaderState(UiStringRes(R.string.scan_scanning_title))
            )
        }

    @Test
    fun `given initial scanning state, when items are built, items contain initial scanning descr`() =
        test {
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
    fun `given non initial scanning state, when items are built, items contain scanning description`() = test {
        val scanStateModelInScanningNonInitialState = scanStateModelWithNoThreats.copy(
            state = State.SCANNING,
            mostRecentStatus = ScanProgressStatus(isInitial = false, startDate = Date(0))
        )

        val scanStateItems = buildScanStateItems(scanStateModelInScanningNonInitialState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_scanning_description))
        )
    }

    @Test
    fun `given scanning state, when items are built, items contain progress bar with progress values`() = test {
        val progress = 10
        val scanStateModelInScanningState = scanStateModelWithNoThreats.copy(
            state = State.SCANNING,
            currentStatus = ScanProgressStatus(progress = progress, startDate = Date(0))
        )

        val scanStateItems = buildScanStateItems(scanStateModelInScanningState)

        assertThat(scanStateItems.filterIsInstance(ProgressState::class.java).first()).isEqualTo(
            ProgressState(
                progress = progress,
                progressLabel = UiStringResWithParams(
                    R.string.scan_progress_label,
                    listOf(UiStringText(progress.toString()))
                )
            )
        )
    }

    /* PROVISIONING STATE */

    @Test
    fun `given provisioning state, when items are built, items contain shield icon with light green color`() = test {
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
    fun `given provisioning state, when items are built, items contain preparing to scan header`() = test {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(HeaderState::class.java).first()).isEqualTo(
            HeaderState(UiStringRes(R.string.scan_preparing_to_scan_title))
        )
    }

    @Test
    fun `given provisioning state, when items are built, items contain provisioning description`() = test {
        val scanStateModelInProvisioningState = scanStateModelWithNoThreats.copy(state = State.PROVISIONING)

        val scanStateItems = buildScanStateItems(scanStateModelInProvisioningState)

        assertThat(scanStateItems.filterIsInstance(DescriptionState::class.java).first()).isEqualTo(
            DescriptionState(UiStringRes(R.string.scan_provisioning_description))
        )
    }

    /* INVALID STATES */

    @Test
    fun `given unknown state, when items are built, items list is empty`() = test {
        val scanStateModelInUnknownState = scanStateModelWithNoThreats.copy(state = State.UNKNOWN)

        val scanStateItems = buildScanStateItems(scanStateModelInUnknownState)

        assertThat(scanStateItems).isEmpty()
    }

    @Test
    fun `given unavailable state, when items are built, items list is empty`() = test {
        val scanStateModelInUnAvailableState = scanStateModelWithNoThreats.copy(state = State.UNAVAILABLE)

        val scanStateItems = buildScanStateItems(scanStateModelInUnAvailableState)

        assertThat(scanStateItems).isEmpty()
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
