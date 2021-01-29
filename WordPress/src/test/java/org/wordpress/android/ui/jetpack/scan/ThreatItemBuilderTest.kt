package org.wordpress.android.ui.jetpack.scan

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
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.CURRENT
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateFormatWrapper
import java.text.DateFormat

private const val TEST_FIXED_ON_DATE = "2 January, 2020"

@InternalCoroutinesApi
class ThreatItemBuilderTest : BaseUnitTest() {
    private lateinit var builder: ThreatItemBuilder

    @Mock private lateinit var dateFormatWrapper: DateFormatWrapper
    @Mock private lateinit var dateFormat: DateFormat

    @Before
    fun setUp() {
        builder = ThreatItemBuilder(dateFormatWrapper)
        whenever(dateFormatWrapper.getLongDateFormat()).thenReturn(dateFormat)
        whenever(dateFormat.format(ThreatTestData.genericThreatModel.baseThreatModel.fixedOn))
            .thenReturn(TEST_FIXED_ON_DATE)
    }

    @Test
    fun `builds threat sub header correctly for fixed threat`() {
        // Arrange
        val expectedSubHeader = UiStringResWithParams(
            R.string.threat_item_sub_header_status_fixed_on,
            listOf(UiStringText(TEST_FIXED_ON_DATE))
        )
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = FIXED))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat sub header correctly for ignored threat`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_status_ignored)
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = IGNORED))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for CoreFileModificationThreatModel`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_infected_core_file,
            listOf(UiStringText(TEST_FILE_NAME))
        )
        // Act
        val threatItem = buildThreatItem(ThreatTestData.coreFileModificationThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for CoreFileModificationThreatModel`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_core_file)
        // Act
        val threatItem = buildThreatItem(ThreatTestData.coreFileModificationThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for DatabaseThreatModel`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_database_threat,
            listOf(UiStringText("${ThreatTestData.rows.size}"))
        )
        // Act
        val threatItem = buildThreatItem(ThreatTestData.databaseThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for DatabaseThreatModel`() {
        // Act
        val threatItem = buildThreatItem(ThreatTestData.databaseThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isNull()
    }

    @Test
    fun `builds threat header correctly for FileThreatModel`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_file_malicious_code_pattern,
            listOf(UiStringText(TEST_FILE_NAME))
        )
        // Act
        val threatItem = buildThreatItem(ThreatTestData.fileThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for FileThreatModel`() {
        // Arrange
        val expectedSubHeader = UiStringResWithParams(
            R.string.threat_item_sub_header_file_signature,
            listOf(UiStringText(TEST_SIGNATURE))
        )
        // Act
        val threatItem = buildThreatItem(ThreatTestData.fileThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for VulnerableExtensionThreatModel of Plugin type`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_vulnerable_plugin,
            listOf(UiStringText(TEST_VULNERABLE_THREAT_SLUG), UiStringText(TEST_VULNERABLE_THREAT_VERSION))
        )
        // Act
        val threatItem = buildThreatItem(ThreatTestData.vulnerableExtensionThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for VulnerableExtensionThreatModel of Plugin type`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_vulnerable_plugin)
        // Act
        val threatItem = buildThreatItem(ThreatTestData.vulnerableExtensionThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for VulnerableExtensionThreatModel of Theme type`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_vulnerable_theme,
            listOf(UiStringText(TEST_VULNERABLE_THREAT_SLUG), UiStringText(TEST_VULNERABLE_THREAT_VERSION))
        )
        // Act
        val threatItem = buildThreatItem(
            ThreatTestData.vulnerableExtensionThreatModel.copy(
                extension = ThreatTestData.extension.copy(type = ExtensionType.THEME)
            )
        )
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for VulnerableExtensionThreatModel of Theme type`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_vulnerable_theme)
        // Act
        val threatItem = buildThreatItem(
            ThreatTestData.vulnerableExtensionThreatModel.copy(
                extension = ThreatTestData.extension.copy(type = ExtensionType.THEME)
            )
        )
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for GenericThreatModel`() {
        // Arrange
        val expectedHeader = UiStringRes(R.string.threat_item_header_threat_found)
        // Act
        val threatItem = buildThreatItem(ThreatTestData.genericThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for GenericThreatModel`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_misc_vulnerability)
        // Act
        val threatItem = buildThreatItem(ThreatTestData.genericThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `onThreatItemClicked listener is correctly assigned to ThreatItem's onClick`() = test {
        // Arrange
        val onThreatItemClicked: (Long) -> Unit = mock()
        val threatItem = buildThreatItem(ThreatTestData.genericThreatModel, onThreatItemClicked)
        // Act
        threatItem.onClick.invoke()
        // Assert
        verify(onThreatItemClicked).invoke(ThreatTestData.genericThreatModel.baseThreatModel.id)
    }

    @Test
    fun `Shield icon and green background is used for fixed threat`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.icon).isEqualTo(R.drawable.ic_shield_tick_white)
        assertThat(threatItem.iconBackground).isEqualTo(R.drawable.bg_oval_success_50)
    }

    @Test
    fun `Notice icon and grey background is used for ignored threat`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = IGNORED))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.icon).isEqualTo(R.drawable.ic_notice_outline_white_24dp)
        assertThat(threatItem.iconBackground).isEqualTo(R.drawable.bg_oval_neutral_30)
    }

    @Test
    fun `Notice icon and red background is used for current threat`() {
        // Arrange
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = CURRENT))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.icon).isEqualTo(R.drawable.ic_notice_outline_white_24dp)
        assertThat(threatItem.iconBackground).isEqualTo(R.drawable.bg_oval_error_50)
    }

    @Test
    fun `Subheader for fixed threat has success color`() {
        // Arrange
        val expectedSubHeader = R.attr.wpColorSuccess
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = FIXED))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.subHeaderColor).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `sub header for current threat has onSurface color`() {
        // Arrange
        val expectedSubHeader = R.attr.colorOnSurface
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = CURRENT))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.subHeaderColor).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `sub header for ignored threat has onSurface color`() {
        // Arrange
        val expectedSubHeader = R.attr.colorOnSurface
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = IGNORED))
        // Act
        val threatItem = buildThreatItem(threatModel)
        // Assert
        assertThat(threatItem.subHeaderColor).isEqualTo(expectedSubHeader)
    }

    private fun buildThreatItem(threatModel: ThreatModel, onThreatItemClicked: ((Long) -> Unit) = mock()) =
        builder.buildThreatItem(
            threatModel = threatModel,
            onThreatItemClicked = onThreatItemClicked
        )
}
