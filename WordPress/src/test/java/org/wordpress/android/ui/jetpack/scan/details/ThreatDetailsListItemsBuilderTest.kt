package org.wordpress.android.ui.jetpack.scan.details

import android.text.SpannedString
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.*
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.*
import org.wordpress.android.ui.jetpack.scan.TEST_FILE_PATH
import org.wordpress.android.ui.jetpack.scan.ThreatTestData
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.*
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

    @Test
    fun `builds basic list items correctly for a ThreatModel in Fixed state`() {
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
        Assertions.assertThat(threatItems).size().isEqualTo(7)
        Assertions.assertThat(threatItems).containsSequence(
            expectedThreatDetailHeaderState,
            expectedFoundHeader,
            expectedFoundSubHeader,
            expectedThreatItemHeader,
            expectedThreatItemSubHeader,
            expectedProblemHeader,
            expectedProblemDescription
        )
    }

    @Test
    fun `builds technical details list items correctly for FileThreatModel`() {
        // Arrange
        val fileThreatModelWithFileName = ThreatTestData.fileThreatModel.copy(fileName = TEST_FILE_PATH)

        val expectedTechnicalDetailsHeaderItem = technicalDetailsHeaderItem
        val expectedFileNameDescriptionItem = fileNameDescriptionItem
        val expectedFileNameItem = ThreatFileNameState(UiStringText(TEST_FILE_PATH))
        val expectedContextLinesItem = ThreatContextLinesItemState(
            listOf(
                ThreatContextLinesItemState.ThreatContextLineItemState(
                    line = ThreatTestData.contextLine,
                    lineNumberBackgroundColorRes = R.color.pink_5,
                    contentBackgroundColorRes = R.color.pink_5,
                    highlightedBackgroundColorRes = R.color.red,
                    highlightedTextColorRes = R.color.white,
                    normalTextColorRes = R.color.black
                )
            )
        )

        // Act
        val threatItems = buildThreatDetailsListItems(fileThreatModelWithFileName)

        // Assert
        Assertions.assertThat(threatItems).containsSequence(
            expectedTechnicalDetailsHeaderItem,
            expectedFileNameDescriptionItem,
            expectedFileNameItem,
            expectedContextLinesItem
        )
    }

    @Test
    fun `builds technical details list items correctly for CoreFileModificationThreatModel`() {
        // Arrange
        val expectedTechnicalDetailsHeaderItem = technicalDetailsHeaderItem
        val expectedFileNameDescriptionItem = fileNameDescriptionItem
        val expectedFileNameItem = ThreatFileNameState(
            UiStringText(ThreatTestData.coreFileModificationThreatModel.fileName)
        )
        val expectedDiffItem = DescriptionState(UiStringText(ThreatTestData.coreFileModificationThreatModel.diff))

        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.coreFileModificationThreatModel)

        // Assert
        Assertions.assertThat(threatItems).containsSubsequence(
            expectedTechnicalDetailsHeaderItem,
            expectedFileNameDescriptionItem,
            expectedFileNameItem,
            expectedDiffItem
        )
    }

    @Test
    fun `builds fix details items correctly for a fixable threat`() {
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
        Assertions.assertThat(threatItems).containsSubsequence(
            expectedFixableHeaderItem,
            expectedFixableDescriptionItem
        )
    }

    @Test
    fun `builds fix details items correctly for a non fixable threat`() {
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
        Assertions.assertThat(threatItems).contains(expectedNotFixableHeaderItem)
        verify(htmlMessageUtils).getHtmlMessageFromStringFormatResId(expectedNotFixableDescriptionStringResId)
    }

    @Test
    fun `builds action buttons correctly for a fixable threat`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.fixableThreatInCurrentStatus,
            onFixThreatButtonClicked = onFixThreatButtonClicked,
            onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        Assertions.assertThat(buttonItems).size().isEqualTo(2)
        Assertions.assertThat(buttonItems).contains(
            fixThreatButtonItem,
            ignoreThreatButtonItem
        )
    }

    @Test
    fun `builds action buttons correctly for a non fixable threat`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.notFixableThreatInCurrentStatus,
            onGetFreeEstimateButtonClicked = onGetFreeEstimateButtonClicked,
            onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        Assertions.assertThat(buttonItems).size().isEqualTo(2)
        Assertions.assertThat(buttonItems).contains(
            getFreeEstimateButtonItem,
            ignoreThreatButtonItem
        )
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
