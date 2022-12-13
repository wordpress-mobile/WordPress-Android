package org.wordpress.android.ui.jetpack.scan.details

import android.text.SpannedString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.scan.TEST_FILE_PATH
import org.wordpress.android.ui.jetpack.scan.ThreatTestData
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatDetailHeaderState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatFileNameState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateFormatWrapper
import java.text.DateFormat

private const val TEST_THREAT_ITEM_HEADER = "Threat found"
private const val TEST_THREAT_ITEM_SUB_HEADER = "Miscellaneous vulnerability"
private const val TEST_FOUND_ON_DATE = "1 January, 2020"
private const val TEST_FIXED_ON_DATE = "2 January, 2020"
private const val TEST_SITE_ID = 1L
private const val SERVER_CREDS_MSG_WITH_CLICKABLE_LINK =
        "<a href=\"${Constants.URL_JETPACK_SETTINGS}/$TEST_SITE_ID}\">Enter your server credentials&lt</a> " +
                "to fix threat."

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class ThreatDetailsListItemsBuilderTest : BaseUnitTest() {
    @Mock
    private lateinit var htmlMessageUtils: HtmlMessageUtils

    @Mock
    private lateinit var threatItemBuilder: ThreatItemBuilder

    @Mock
    private lateinit var dateFormatWrapper: DateFormatWrapper

    @Mock
    private lateinit var dateFormat: DateFormat

    private lateinit var builder: ThreatDetailsListItemsBuilder

    private val technicalDetailsHeaderItem = HeaderState(UiStringRes(R.string.threat_technical_details_header))
    private val fileNameDescriptionItem = DescriptionState(UiStringRes(R.string.threat_file_description))

    private val onFixThreatButtonClicked: () -> Unit = mock()
    private val onGetFreeEstimateButtonClicked: () -> Unit = mock()
    private val onIgnoreThreatButtonClicked: () -> Unit = mock()

    private val fixThreatButtonItem = ActionButtonState(
            text = UiStringRes(R.string.threat_fix),
            onClick = onFixThreatButtonClicked,
            contentDescription = UiStringRes(R.string.threat_fix)
    )
    private val getFreeEstimateButtonItem = ActionButtonState(
            text = UiStringRes(R.string.threat_get_free_estimate),
            onClick = onGetFreeEstimateButtonClicked,
            contentDescription = UiStringRes(R.string.threat_get_free_estimate)
    )
    private val ignoreThreatButtonItem = ActionButtonState(
            text = UiStringRes(R.string.threat_ignore),
            onClick = onIgnoreThreatButtonClicked,
            contentDescription = UiStringRes(R.string.threat_ignore),
            isSecondary = true
    )

    @Before
    fun setUp() {
        builder = ThreatDetailsListItemsBuilder(
                htmlMessageUtils,
                threatItemBuilder,
                dateFormatWrapper
        )

        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(any())).thenReturn(
                SpannedString("")
        )
        with(threatItemBuilder) {
            whenever(buildThreatItemHeader(any())).thenReturn(UiStringText(TEST_THREAT_ITEM_HEADER))
            whenever(buildThreatItemDescription(any())).thenReturn(UiStringText(TEST_THREAT_ITEM_SUB_HEADER))
        }
        whenever(dateFormatWrapper.getLongDateFormat()).thenReturn(dateFormat)
        whenever(dateFormat.format(ThreatTestData.genericThreatModel.baseThreatModel.firstDetected))
                .thenReturn(TEST_FOUND_ON_DATE)
        whenever(dateFormat.format(ThreatTestData.genericThreatModel.baseThreatModel.fixedOn!!))
                .thenReturn(TEST_FIXED_ON_DATE)
    }

    /* BASIC DETAILS */

    @Test
    fun `given any threat model, when items are built, then icon is added from threat item builder`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].icon).isEqualTo(
                threatItemBuilder.buildThreatItemIcon(ThreatTestData.genericThreatModel)
        )
    }

    @Test
    fun `given any threat model, when items are built, then icon background is added from threat item builder`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].iconBackground).isEqualTo(
                threatItemBuilder.buildThreatItemIconBackground(ThreatTestData.genericThreatModel)
        )
    }

    @Test
    fun `given any threat model, when items are built, then threat item header is added from threat item builder`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        assertThat(threatItems.filterIsInstance<HeaderState>()).contains(
                HeaderState(threatItemBuilder.buildThreatItemHeader(ThreatTestData.genericThreatModel))
        )
    }

    @Test
    fun `given any threat model, when items are built, then threat item description added from threat item builder`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        assertThat(threatItems.filterIsInstance<DescriptionState>()).contains(
                DescriptionState(threatItemBuilder.buildThreatItemDescription(ThreatTestData.genericThreatModel))
        )
    }

    @Test
    fun `given any threat model, when items are built, then problem details are added`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        assertThat(threatItems.filterIsInstance<HeaderState>()).contains(
                HeaderState(UiStringRes(R.string.threat_problem_header))
        )
        assertThat(threatItems.filterIsInstance<DescriptionState>()).contains(
                DescriptionState(UiStringText(ThreatTestData.genericThreatModel.baseThreatModel.description))
        )
    }

    /* TECHNICAL DETAILS - FILE THREAT */

    @Test
    fun `given file threat with file name, when items are built, then technical details exist in correct sequence`() {
        val fileThreatModelWithFileName = ThreatTestData.fileThreatModel

        val threatItems = buildThreatDetailsListItems(fileThreatModelWithFileName)

        assertThat(threatItems.map { it::class.java }).containsSequence(
                HeaderState::class.java,
                DescriptionState::class.java,
                ThreatFileNameState::class.java,
                ThreatContextLinesItemState::class.java
        )
    }

    @Test
    fun `given file threat, when items are built, then technical details header exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        assertThat(threatItems).contains(technicalDetailsHeaderItem)
    }

    @Test
    fun `given file threat with file name, when items are built, then file name description exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        assertThat(threatItems).contains(fileNameDescriptionItem)
    }

    @Test
    fun `given file threat without file name, when items are built, then file name description does not exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel.copy(fileName = null))

        assertThat(threatItems).doesNotContain(fileNameDescriptionItem)
    }

    @Test
    fun `given file threat without file nam, when items are built, then file name does not exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel.copy(fileName = null))

        assertThat(threatItems).doesNotContain(ThreatFileNameState(UiStringText(TEST_FILE_PATH)))
    }

    @Test
    fun `given file threat, when items are built, then context lines exist`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        assertThat(threatItems).contains(
                ThreatContextLinesItemState(
                        listOf(
                                ThreatContextLinesItemState.ThreatContextLineItemState(
                                        line = ThreatTestData.contextLine,
                                        lineNumberBackgroundColorRes = R.color.context_line_highlighted_row_background,
                                        contentBackgroundColorRes = R.color.context_line_highlighted_row_background,
                                        highlightedBackgroundColorRes = R.color.red_60,
                                        highlightedTextColorRes = R.color.white
                                )
                        )
                )
        )
    }

    /* TECHNICAL DETAILS - CORE FILE MODIFICATION THREAT */

    @Test
    fun `given core file modif threat, when items are built, then technical details exist in correct sequence`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItems.map { it::class.java }).containsSequence(
                HeaderState::class.java,
                DescriptionState::class.java,
                ThreatFileNameState::class.java,
                DescriptionState::class.java
        )
    }

    @Test
    fun `given core file modif threat, when items are built, then technical details header exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItems).contains(technicalDetailsHeaderItem)
    }

    @Test
    fun `given core file modif threat with file name, when items are built, then file name description exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItems).contains(fileNameDescriptionItem)
    }

    @Test
    fun `given core file modif threat with file name, when items are built, then file name exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItems).contains(fileNameDescriptionItem)
    }

    @Test
    fun `given core file modif threat, when items are built, then diff item exists`() {
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItems).contains(
                DescriptionState(UiStringText(ThreatTestData.coreFileModificationThreatModel.diff))
        )
    }

    /* FIXABLE THREAT */

    @Test
    fun `given fixable threat not in fixed status, when items are built, then fix header exists`() {
        val fixableThreat = GenericThreatModel(
                ThreatTestData.baseThreatModel.copy(
                        fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null)
                )
        )

        val threatItems = buildThreatDetailsListItems(fixableThreat)

        assertThat(threatItems).contains(HeaderState(UiStringRes(R.string.threat_fix_current_fixable_header)))
    }

    @Test
    fun `given current fixable threat with server creds, when items are built, then fix threat btn is enabled`() {
        val items = buildThreatDetailsListItems(ThreatTestData.fixableThreatInCurrentStatus)

        assertThat(
                items.filterIsInstance(ActionButtonState::class.java)
                        .firstOrNull { it.text == UiStringRes(R.string.threat_fix) }?.isEnabled
        ).isTrue
    }

    @Test
    fun `given current fixable threat without server creds, when items are built, then fix threat btn is disabled`() {
        val items = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus,
                scanStateHasValidCredentials = false
        )

        assertThat(
                items.filterIsInstance(ActionButtonState::class.java)
                        .firstOrNull { it.text == UiStringRes(R.string.threat_fix) }?.isEnabled
        ).isFalse
    }

    @Test
    fun `given current fixable threat with server creds, when items are built, then server creds msg doesn't exist`() {
        val items = buildThreatDetailsListItems(ThreatTestData.fixableThreatInCurrentStatus)

        assertThat(items.filterIsInstance(DescriptionState::class.java)
                .firstOrNull { it.text == UiStringRes(R.string.threat_fix_enter_server_creds_msg_singular) })
                .isNull()
    }

    @Test
    fun `given current fixable threat without server creds, when items are built, then server creds msg exists`() {
        val items = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus,
                scanStateHasValidCredentials = false
        )

        assertThat(items.filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) })
                .isNotNull
    }

    @Test
    fun `given ignored fixable threat with server creds, when items are built, then server creds msg doesn't exist`() {
        val items = buildThreatDetailsListItems(ThreatTestData.fixableThreatInIgnoredStatus)

        assertThat(items.filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) })
                .isNull()
    }

    @Test
    fun `given server creds msg exists, when items are built, then primary colored plus button exists`() =
            test {
                val scanStateItems = buildThreatDetailsListItems(
                        model = ThreatTestData.fixableThreatInCurrentStatus,
                        scanStateHasValidCredentials = false
                )

                val serverCredsMsg = scanStateItems.filterIsInstance(FootnoteState::class.java)
                        .first { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) }
                with(serverCredsMsg) {
                    assertThat(iconResId).isEqualTo(R.drawable.ic_plus_white_24dp)
                    assertThat(iconColorResId).isEqualTo(R.color.colorPrimary)
                }
            }

    @Test
    fun `given ignored fixable threat without server creds, when items are built, then server creds msg exists`() {
        val items = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInIgnoredStatus,
                scanStateHasValidCredentials = false
        )

        assertThat(items.filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) })
                .isNotNull
    }

    @Test
    fun `given fixed threat with server creds, when items are built, then server creds msg doesn't exist`() {
        val items = buildThreatDetailsListItems(ThreatTestData.fixableThreatInFixedStatus)

        assertThat(items.filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) })
                .isNull()
    }

    @Test
    fun `given fixed threat without server creds, when items are built, then server creds msg doesn't exist`() {
        val items = buildThreatDetailsListItems(ThreatTestData.fixableThreatInFixedStatus)

        assertThat(items.filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) })
                .isNull()
    }

    @Test
    fun `given fixable threat in fixed status, when items are built, then fix header does not exists`() {
        val fixableThreat = GenericThreatModel(
                ThreatTestData.baseThreatModel.copy(
                        fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null),
                        status = FIXED
                )
        )

        val threatItems = buildThreatDetailsListItems(fixableThreat)

        assertThat(threatItems).doesNotContain(HeaderState(UiStringRes(R.string.threat_fix_current_fixable_header)))
    }

    @Test
    fun `given fixable threat not in fixed status, when items are built, then fix description exists`() {
        val fixableThreat = GenericThreatModel(
                ThreatTestData.baseThreatModel.copy(
                        fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null)
                )
        )

        val threatItems = buildThreatDetailsListItems(fixableThreat)

        assertThat(threatItems).contains(DescriptionState(UiStringRes(R.string.threat_fix_fixable_edit)))
    }

    @Test
    fun `given fixable threat in fixed status, when items are built, then fix description does not exists`() {
        val fixableThreat = GenericThreatModel(
                ThreatTestData.baseThreatModel.copy(
                        fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null),
                        status = FIXED
                )
        )

        val threatItems = buildThreatDetailsListItems(fixableThreat)

        assertThat(threatItems).doesNotContain(DescriptionState(UiStringRes(R.string.threat_fix_fixable_edit)))
    }

    @Test
    fun `given fixable threat in current status, when items are built, then fix threat button exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus,
                onFixThreatButtonClicked = onFixThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(fixThreatButtonItem)
    }

    @Test
    fun `given fixable threat in ignored status, when items are built, then fix threat button exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus.copy(
                        baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel
                                .copy(status = IGNORED)
                ),
                onFixThreatButtonClicked = onFixThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(fixThreatButtonItem)
    }

    @Test
    fun `given fixable threat in fixed status, when items are built, then fix threat button does not exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus.copy(
                        baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel
                                .copy(status = FIXED)
                ),
                onFixThreatButtonClicked = onFixThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).doesNotContain(fixThreatButtonItem)
    }

    @Test
    fun `given fixable threat in current status, when items are built, then ignore threat button exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus,
                onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(ignoreThreatButtonItem)
    }

    @Test
    fun `given fixable threat in fixed status, when items are built, then ignore threat button does not exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus.copy(
                        baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel
                                .copy(status = FIXED)
                ),
                onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).doesNotContain(ignoreThreatButtonItem)
    }

    @Test
    fun `given fixable threat in ignored status, when items are built, then ignore threat button does not exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.fixableThreatInCurrentStatus.copy(
                        baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel
                                .copy(status = IGNORED)
                ),
                onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).doesNotContain(ignoreThreatButtonItem)
    }

    /* NON FIXABLE THREAT */

    @Test
    fun `given non fixable threat, when items are built, then not fixable header exists`() {
        val notFixableThreat = GenericThreatModel(ThreatTestData.baseThreatModel.copy(fixable = null))

        val threatItems = buildThreatDetailsListItems(notFixableThreat)

        assertThat(threatItems).contains(HeaderState(UiStringRes(R.string.threat_fix_current_not_fixable_header)))
    }

    @Test
    fun `given non fixable threat, when items are built, then not fixable description exists`() {
        val notFixableThreat = GenericThreatModel(ThreatTestData.baseThreatModel.copy(fixable = null))

        buildThreatDetailsListItems(notFixableThreat)

        verify(htmlMessageUtils)
                .getHtmlMessageFromStringFormatResId(R.string.threat_fix_current_not_fixable_description)
    }

    @Test
    fun `given non fixable threat in current status, when items are built, then get free estimates button exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.notFixableThreatInCurrentStatus,
                onGetFreeEstimateButtonClicked = onGetFreeEstimateButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(getFreeEstimateButtonItem)
    }

    @Test
    fun `given non fixable threat in current status, when items are built, then ignore threat button exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.notFixableThreatInCurrentStatus,
                onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(ignoreThreatButtonItem)
    }

    @Test
    fun `given non fixable threat in ignored status, when items are built, then ignore threat btn does not exists`() {
        val threatItems = buildThreatDetailsListItems(
                model = ThreatTestData.notFixableThreatInCurrentStatus.copy(
                        baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel
                                .copy(status = IGNORED)
                ),
                onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).doesNotContain(ignoreThreatButtonItem)
    }

    @Test
    fun `given non fixable threat, when items are built, then server creds msg doesn't exist`() {
        val items = buildThreatDetailsListItems(ThreatTestData.notFixableThreatInCurrentStatus)

        assertThat(items.filterIsInstance(DescriptionState::class.java)
                .firstOrNull { it.text == UiStringRes(R.string.threat_fix_enter_server_creds_msg_singular) })
                .isNull()
    }

    /* FIXED THREAT */

    @Test
    fun `given threat status = fixed, when items are built, then Fixed header is added as main header`() {
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))

        val threatItems = buildThreatDetailsListItems(threatModel)

        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>().size).isEqualTo(1)
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].header)
                .isEqualTo(UiStringRes(R.string.threat_status_fixed))
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].description)
                .isEqualTo(UiStringText(TEST_FIXED_ON_DATE))
    }

    @Test
    fun `given threat status = fixed, when items are built, then Found header is added below main header`() {
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))

        val threatItems = buildThreatDetailsListItems(threatModel)

        assertThat(threatItems.filterIsInstance<HeaderState>()[0].text)
                .isEqualTo(UiStringRes(R.string.threat_found_header))
        assertThat(threatItems.filterIsInstance<DescriptionState>()[0].text)
                .isEqualTo(UiStringText(TEST_FOUND_ON_DATE))
    }

    /* IGNORED THREAT */

    @Test
    fun `given threat status = ignored, when items are built, then Found header is added as main header`() {
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = IGNORED))

        val threatItems = buildThreatDetailsListItems(threatModel)

        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>().size).isEqualTo(1)
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].header)
                .isEqualTo(UiStringRes(R.string.threat_found_header))
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].description)
                .isEqualTo(UiStringText(TEST_FOUND_ON_DATE))
    }

    /* CURRENT THREAT */

    @Test
    fun `given threat status = current, when items are built, then Found header is added as main header`() {
        val threatModel = GenericThreatModel(
                ThreatTestData.genericThreatModel.baseThreatModel.copy(status = ThreatStatus.CURRENT)
        )

        val threatItems = buildThreatDetailsListItems(threatModel)

        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>().size).isEqualTo(1)
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].header)
                .isEqualTo(UiStringRes(R.string.threat_found_header))
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].description)
                .isEqualTo(UiStringText(TEST_FOUND_ON_DATE))
    }

    private fun buildThreatDetailsListItems(
        model: ThreatModel,
        scanStateHasValidCredentials: Boolean = true,
        onFixThreatButtonClicked: () -> Unit = mock(),
        onGetFreeEstimateButtonClicked: () -> Unit = mock(),
        onIgnoreThreatButtonClicked: () -> Unit = mock(),
        onEnterServerCredsIconClicked: () -> Unit = mock()
    ): List<JetpackListItemState> {
        if (!scanStateHasValidCredentials) {
            whenever(
                    htmlMessageUtils
                            .getHtmlMessageFromStringFormatResId(
                                    R.string.threat_fix_enter_server_creds_msg_singular,
                                    "${Constants.URL_JETPACK_SETTINGS}/$TEST_SITE_ID"
                            )
            ).thenReturn(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK)
        }

        return builder.buildThreatDetailsListItems(
                threatModel = model,
                scanStateHasValidCredentials = scanStateHasValidCredentials,
                siteId = TEST_SITE_ID,
                onFixThreatButtonClicked = onFixThreatButtonClicked,
                onGetFreeEstimateButtonClicked = onGetFreeEstimateButtonClicked,
                onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked,
                onEnterServerCredsIconClicked = onEnterServerCredsIconClicked
        )
    }
}
