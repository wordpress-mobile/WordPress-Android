package org.wordpress.android.ui.jetpack.scan

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
class ThreatItemBuilderTest : BaseUnitTest() {
    private lateinit var builder: ThreatItemBuilder

    @Before
    fun setUp() {
        builder = ThreatItemBuilder()
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
        // Arrange
        val expectedSubHeader = UiStringText("")
        // Act
        val threatItem = buildThreatItem(ThreatTestData.databaseThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
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

    private fun buildThreatItem(threatModel: ThreatModel, onThreatItemClicked: ((Long) -> Unit) = mock()) =
        builder.buildThreatItem(
            threatModel = threatModel,
            onThreatItemClicked = onThreatItemClicked
        )
}
