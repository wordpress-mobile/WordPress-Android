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
    fun `given any threat model, when items are built, then icon is added from threat item builder`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].icon).isEqualTo(
            threatItemBuilder.buildThreatItemIcon(ThreatTestData.genericThreatModel)
        )
    }

    @Test
    fun `given any threat model, when items are built, then icon background is added from threat item builder`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<ThreatDetailHeaderState>()[0].iconBackground).isEqualTo(
            threatItemBuilder.buildThreatItemIconBackground(ThreatTestData.genericThreatModel)
        )
    }

    @Test
    fun `given any threat model, when items are built, then threat item header is added from threat item builder`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<HeaderState>()).contains(
            HeaderState(threatItemBuilder.buildThreatItemHeader(ThreatTestData.genericThreatModel))
        )
    }

    @Test
    fun `given any threat model, when items are built, then threat item description added from threat item builder`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<DescriptionState>()).contains(
            DescriptionState(threatItemBuilder.buildThreatItemDescription(ThreatTestData.genericThreatModel))
        )
    }

    @Test
    fun `given any threat model, when items are built, then problem details are added`() {
        // Act
        val threatItems = buildThreatDetailsListItems(ThreatTestData.genericThreatModel)

        // Assert
        assertThat(threatItems.filterIsInstance<HeaderState>()).contains(
            HeaderState(UiStringRes(R.string.threat_problem_header))
        )
        assertThat(threatItems.filterIsInstance<DescriptionState>()).contains(
            DescriptionState(UiStringText(ThreatTestData.genericThreatModel.baseThreatModel.description))
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
    fun `given fixable threat not in fixed status, when items are built, then fix header exists`() {
        // Arrange
        val fixableThreat = GenericThreatModel(
            ThreatTestData.baseThreatModel.copy(
                fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null)
            )
        )

        // Act
        val threatItems = buildThreatDetailsListItems(fixableThreat)

        // Assert
        assertThat(threatItems).contains(HeaderState(UiStringRes(R.string.threat_fix_current_fixable_header)))
    }

    @Test
    fun `given fixable threat not in fixed status, when items are built, then fix description exists`() {
        // Arrange
        val fixableThreat = GenericThreatModel(
            ThreatTestData.baseThreatModel.copy(
                fixable = Fixable(file = null, fixer = Fixable.FixType.EDIT, target = null)
            )
        )

        // Act
        val threatItems = buildThreatDetailsListItems(fixableThreat)

        // Assert
        assertThat(threatItems).contains(DescriptionState(UiStringRes(R.string.threat_fix_fixable_edit)))
    }

    @Test
    fun `given fixable threat in current status, when items are built, then fix threat button exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.fixableThreatInCurrentStatus,
            onFixThreatButtonClicked = onFixThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(fixThreatButtonItem)
    }

    @Test
    fun `given fixable threat in current status, when items are built, then ignore threat button exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.fixableThreatInCurrentStatus,
            onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(ignoreThreatButtonItem)
    }

    @Test
    fun `given fixable threat in ignored status, when items are built, then ignore threat button exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.fixableThreatInCurrentStatus.copy(
                baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel.copy(status = IGNORED)
            ),
            onFixThreatButtonClicked = onFixThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(fixThreatButtonItem)
    }

    /* NON FIXABLE THREAT */

    @Test
    fun `given non fixable threat, when items are built, then not fixable header exists`() {
        // Arrange
        val notFixableThreat = GenericThreatModel(ThreatTestData.baseThreatModel.copy(fixable = null))

        // Act
        val threatItems = buildThreatDetailsListItems(notFixableThreat)

        // Assert
        assertThat(threatItems).contains(HeaderState(UiStringRes(R.string.threat_fix_current_not_fixable_header)))
    }

    @Test
    fun `given non fixable threat, when items are built, then not fixable description exists`() {
        // Arrange
        val notFixableThreat = GenericThreatModel(ThreatTestData.baseThreatModel.copy(fixable = null))

        // Act
        buildThreatDetailsListItems(notFixableThreat)

        // Assert
        verify(htmlMessageUtils)
            .getHtmlMessageFromStringFormatResId(R.string.threat_fix_current_not_fixable_description)
    }

    @Test
    fun `given non fixable threat in current status, when items are built, then get free estimates button exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.notFixableThreatInCurrentStatus,
            onGetFreeEstimateButtonClicked = onGetFreeEstimateButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(getFreeEstimateButtonItem)
    }

    @Test
    fun `given non fixable threat in current status, when items are built, then ignore threat button exists`() {
        // Act
        val threatItems = buildThreatDetailsListItems(
            model = ThreatTestData.notFixableThreatInCurrentStatus,
            onIgnoreThreatButtonClicked = onIgnoreThreatButtonClicked
        )

        // Assert
        val buttonItems = threatItems.filterIsInstance(ActionButtonState::class.java)
        assertThat(buttonItems).contains(ignoreThreatButtonItem)
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
