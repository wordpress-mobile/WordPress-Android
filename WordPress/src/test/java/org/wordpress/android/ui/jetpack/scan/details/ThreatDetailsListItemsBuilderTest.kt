package org.wordpress.android.ui.jetpack.scan.details

import android.text.SpannedString
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
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

@InternalCoroutinesApi
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
        builder = ThreatDetailsListItemsBuilder(htmlMessageUtils, threatItemBuilder, dateFormatWrapper)

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
        whenever(dateFormat.format(ThreatTestData.genericThreatModel.baseThreatModel.fixedOn))
            .thenReturn(TEST_FIXED_ON_DATE)
    }

    /* BASIC DETAILS */

    @Test
    fun `given fixed state threat, when items are built, then basic list items are built correctly`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))
        val expectedThreatDetailHeaderState = ThreatDetailHeaderState(
            icon = threatItemBuilder.buildThreatItemIcon(threatModel),
            iconBackground = threatItemBuilder.buildThreatItemIconBackground(threatModel),
            header = UiStringRes(R.string.threat_status_fixed),
            description = UiStringText(TEST_FIXED_ON_DATE)
        )
        val expectedFoundHeader = HeaderState(text = UiStringRes(R.string.threat_found_header))

        val expectedFoundSubHeader = DescriptionState(UiStringText(TEST_FOUND_ON_DATE))

        val expectedThreatItemHeader = HeaderState(
            text = threatItemBuilder.buildThreatItemHeader(threatModel),
            textColorRes = R.attr.colorOnSurface
        )
        val expectedThreatItemSubHeader = DescriptionState(
            threatItemBuilder.buildThreatItemDescription(threatModel)
        )
        val expectedProblemHeader = HeaderState(UiStringRes(R.string.threat_problem_header))
        val expectedProblemDescription = DescriptionState(
            UiStringText(threatModel.baseThreatModel.description)
        )

        // Act
        val threatItems = buildThreatDetailsListItems(threatModel)

        // Assert
        assertThat(threatItems).size().isEqualTo(7)
        assertThat(threatItems).containsSequence(
            expectedThreatDetailHeaderState,
            expectedFoundHeader,
            expectedFoundSubHeader,
            expectedThreatItemHeader,
            expectedThreatItemSubHeader,
            expectedProblemHeader,
            expectedProblemDescription
        )
    }

    /* TECHNICAL DETAILS - FILE THREAT */

    @Test
    fun `given file threat, when items are built, then technical details exist in correct sequence`() {
        // Arrange
        val fileThreatModelWithFileName = ThreatTestData.fileThreatModel

        // Act
        val threatItems = buildThreatDetailsListItems(fileThreatModelWithFileName)

        // Assert
        assertThat(threatItems.map { it::class.java }).containsSequence(
            HeaderState::class.java,
            DescriptionState::class.java,
            ThreatFileNameState::class.java,
            ThreatContextLinesItemState::class.java
        )
    }

    @Test
    fun `given file threat, when items are built, then technical details header exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        // Assert
        assertThat(threatItems).contains(technicalDetailsHeaderItem)
    }

    @Test
    fun `given file threat with file name, when items are built, then file name description exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        // Assert
        assertThat(threatItems).contains(fileNameDescriptionItem)
    }

    @Test
    fun `given file threat with file name, when items are built, then file name exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        // Assert
        assertThat(threatItems).contains(ThreatFileNameState(UiStringText(TEST_FILE_PATH)))
    }

    @Test
    fun `given file threat, when items are built, then context lines exist`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.fileThreatModel)

        // Assert
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
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        // Assert
        assertThat(threatItems.map { it::class.java }).containsSequence(
            HeaderState::class.java,
            DescriptionState::class.java,
            ThreatFileNameState::class.java,
            DescriptionState::class.java
        )
    }

    @Test
    fun `given core file modif threat, when items are built, then technical details header exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        // Assert
        assertThat(threatItems).contains(technicalDetailsHeaderItem)
    }

    @Test
    fun `given core file modif threat with file name, when items are built, then file name description exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        // Assert
        assertThat(threatItems).contains(fileNameDescriptionItem)
    }

    @Test
    fun `given core file modif threat with file name, when items are built, then file name exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        // Assert
        assertThat(threatItems).contains(fileNameDescriptionItem)
    }

    @Test
    fun `given core file modif threat, when items are built, then diff item exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        // Assert
        assertThat(threatItems).contains(
            DescriptionState(UiStringText(ThreatTestData.coreFileModificationThreatModel.diff))
        )
    }

    /* FIXABLE THREAT */

    @Test
    fun `given fixable threat, when items are built, then fix details items are built correctly`() {
        // Arrange
        val fixableThreat = GenericThreatModel(
            ThreatTestData.baseThreatModel.copy(
                fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null),
                status = ThreatStatus.CURRENT
            )
        )
        val expectedFixableHeaderItem = HeaderState(UiStringRes(R.string.threat_fix_current_fixable_header))
        val expectedFixableDescriptionItem = DescriptionState(UiStringRes(R.string.threat_fix_fixable_edit))

        // Act
        val threatItems = buildThreatDetailsListItems(fixableThreat)

        // Assert
        assertThat(threatItems).containsSubsequence(
            expectedFixableHeaderItem,
            expectedFixableDescriptionItem
        )
    }

    @Test
    fun `given fixable threat, when items are built, then action buttons are built correctly`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.fixableThreatInCurrentStatus,
            onFixThreatButtonClicked = onFixThreatButtonClicked,
            onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).size().isEqualTo(2)
        assertThat(buttonItems).contains(
            fixThreatButtonItem,
            ignoreThreatButtonItem
        )
    }

    /* NON FIXABLE THREAT */

    @Test
    fun `given non fixable threat, when items are built, then fix details items are built correctly`() {
        // Arrange
        val notFixableThreat = GenericThreatModel(
            ThreatTestData.baseThreatModel.copy(
                fixable = null,
                status = ThreatStatus.CURRENT
            )
        )
        val expectedNotFixableHeaderItem = HeaderState(UiStringRes(R.string.threat_fix_current_not_fixable_header))
        val expectedNotFixableDescriptionStringResId = R.string.threat_fix_current_not_fixable_description

        // Act
        val threatItems = buildThreatDetailsListItems(notFixableThreat)

        // Assert
        assertThat(threatItems).contains(expectedNotFixableHeaderItem)
        verify(htmlMessageUtils).getHtmlMessageFromStringFormatResId(expectedNotFixableDescriptionStringResId)
    }

    @Test
    fun `given non fixable threat, when items are built, then action buttons are built correctly`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.notFixableThreatInCurrentStatus,
            onGetFreeEstimateButtonClicked = onGetFreeEstimateButtonClicked,
            onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).size().isEqualTo(2)
        assertThat(buttonItems).contains(
            getFreeEstimateButtonItem,
            ignoreThreatButtonItem
        )
    }

    /* FIXED THREAT */

    @Test
    fun `given threat status = fixed, when items are built, then Fixed header is added as main header`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))

        // Act
        val threatItems = buildThreatDetailsListItems(threatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>().size).isEqualTo(1)
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].header)
            .isEqualTo(UiStringRes(R.string.threat_status_fixed))
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].description)
            .isEqualTo(UiStringText(TEST_FIXED_ON_DATE))
    }

    @Test
    fun `given threat status = fixed, when items are built, then Found header is added below main header`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))

        // Act
        val threatItems = buildThreatDetailsListItems(threatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<HeaderState>()[0].text)
            .isEqualTo(UiStringRes(R.string.threat_found_header))
        assertThat(threatItems.filterIsInstance<DescriptionState>()[0].text)
            .isEqualTo(UiStringText(TEST_FOUND_ON_DATE))
    }

    /* IGNORED THREAT */

    @Test
    fun `given threat status = ignored, when items are built, then Found header is added as main header`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = IGNORED))

        // Act
        val threatItems = buildThreatDetailsListItems(threatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>().size).isEqualTo(1)
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].header)
            .isEqualTo(UiStringRes(R.string.threat_found_header))
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].description)
            .isEqualTo(UiStringText(TEST_FOUND_ON_DATE))
    }

    /* CURRENT THREAT */

    @Test
    fun `given threat status = current, when items are built, then Found header is added as main header`() {
        // Arrange
        val threatModel = GenericThreatModel(
            ThreatTestData.genericThreatModel.baseThreatModel.copy(status = ThreatStatus.CURRENT)
        )

        // Act
        val threatItems = buildThreatDetailsListItems(threatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>().size).isEqualTo(1)
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].header)
            .isEqualTo(UiStringRes(R.string.threat_found_header))
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].description)
            .isEqualTo(UiStringText(TEST_FOUND_ON_DATE))
    }

    private fun buildThreatDetailsListItems(
        model: ThreatModel,
        onFixThreatButtonClicked: () -> Unit = mock(),
        onGetFreeEstimateButtonClicked: () -> Unit = mock(),
        onIgnoreThreatButtonClicked: () -> Unit = mock()
    ) = builder.buildThreatDetailsListItems(
        threatModel = model,
        onFixThreatButtonClicked = onFixThreatButtonClicked,
        onGetFreeEstimateButtonClicked = onGetFreeEstimateButtonClicked,
        onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
    )
}
