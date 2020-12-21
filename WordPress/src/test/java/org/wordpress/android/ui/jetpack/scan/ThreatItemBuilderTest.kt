package org.wordpress.android.ui.jetpack.scan

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.BaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel.Row
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.CURRENT
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType.THEME
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.util.Date

private const val FILE_PATH = "/var/www/html/jp-scan-daily/wp-admin/index.php"
private const val FILE_NAME = "index.php"
private const val VULNERABLE_THREAT_SLUG = "test slug"
private const val VULNERABLE_THREAT_VERSION = "test version"
private const val TEST_SIGNATURE = "test signature"

@InternalCoroutinesApi
class ThreatItemBuilderTest : BaseUnitTest() {
    private lateinit var builder: ThreatItemBuilder

    private val baseThreatModel = BaseThreatModel(
        id = 1L,
        signature = TEST_SIGNATURE,
        description = "",
        status = CURRENT,
        firstDetected = Date(0)
    )
    private val extension = Extension(
        type = Extension.ExtensionType.PLUGIN,
        slug = VULNERABLE_THREAT_SLUG,
        name = "",
        version = VULNERABLE_THREAT_VERSION,
        isPremium = false
    )
    private val rows = listOf(Row(id = 1, rowNumber = 1))
    private val coreFileModificationThreatModel = CoreFileModificationThreatModel(
        baseThreatModel = baseThreatModel,
        fileName = FILE_PATH,
        diff = ""
    )
    private val databaseThreatModel = DatabaseThreatModel(
        baseThreatModel = baseThreatModel,
        rows = rows
    )
    private val fileThreatModel = FileThreatModel(
        baseThreatModel = baseThreatModel,
        fileName = FILE_NAME,
        context = ThreatContext(emptyList())
    )
    private val vulnerableExtensionThreatModel = VulnerableExtensionThreatModel(
        baseThreatModel = baseThreatModel,
        extension = extension
    )
    private val genericThreatModel = GenericThreatModel(
        baseThreatModel = baseThreatModel
    )

    @Before
    fun setUp() {
        builder = ThreatItemBuilder()
    }

    @Test
    fun `builds threat header correctly for CoreFileModificationThreatModel`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_infected_core_file,
            listOf(UiStringText(FILE_NAME))
        )
        // Act
        val threatItem = buildThreatItem(coreFileModificationThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for CoreFileModificationThreatModel`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_core_file)
        // Act
        val threatItem = buildThreatItem(coreFileModificationThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for DatabaseThreatModel`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_database_threat,
            listOf(UiStringText("${rows.size}"))
        )
        // Act
        val threatItem = buildThreatItem(databaseThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly for DatabaseThreatModel`() {
        // Arrange
        val expectedSubHeader = UiStringText("")
        // Act
        val threatItem = buildThreatItem(databaseThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for FileThreatModel`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_file_malicious_code_pattern,
            listOf(UiStringText(FILE_NAME))
        )
        // Act
        val threatItem = buildThreatItem(fileThreatModel)
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
        val threatItem = buildThreatItem(fileThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for VulnerableExtensionThreatModel of Plugin type`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_vulnerable_plugin,
            listOf(UiStringText(VULNERABLE_THREAT_SLUG), UiStringText(VULNERABLE_THREAT_VERSION))
        )
        // Act
        val threatItem = buildThreatItem(vulnerableExtensionThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly VulnerableExtensionThreatModel of Plugin type`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_vulnerable_plugin)
        // Act
        val threatItem = buildThreatItem(vulnerableExtensionThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for VulnerableExtensionThreatModel of Theme type`() {
        // Arrange
        val expectedHeader = UiStringResWithParams(
            R.string.threat_item_header_vulnerable_theme,
            listOf(UiStringText(VULNERABLE_THREAT_SLUG), UiStringText(VULNERABLE_THREAT_VERSION))
        )
        // Act
        val threatItem = buildThreatItem(vulnerableExtensionThreatModel.copy(extension = extension.copy(type = THEME)))
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly VulnerableExtensionThreatModel of Theme type`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_vulnerable_theme)
        // Act
        val threatItem = buildThreatItem(vulnerableExtensionThreatModel.copy(extension = extension.copy(type = THEME)))
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `builds threat header correctly for GenericThreatModel`() {
        // Arrange
        val expectedHeader = UiStringRes(R.string.threat_item_header_threat_found)
        // Act
        val threatItem = buildThreatItem(genericThreatModel)
        // Assert
        assertThat(threatItem.header).isEqualTo(expectedHeader)
    }

    @Test
    fun `builds threat sub header correctly Plugin GenericThreatModel`() {
        // Arrange
        val expectedSubHeader = UiStringRes(R.string.threat_item_sub_header_misc_vulnerability)
        // Act
        val threatItem = buildThreatItem(genericThreatModel)
        // Assert
        assertThat(threatItem.subHeader).isEqualTo(expectedSubHeader)
    }

    @Test
    fun `onThreatItemClicked listener is correctly assigned to ThreatItem's onClick`() = test {
        // Arrange
        val onThreatItemClicked: (ThreatModel) -> Unit = mock()
        val threatItem = buildThreatItem(genericThreatModel, onThreatItemClicked)
        // Act
        threatItem.onClick.invoke()
        // Assert
        verify(onThreatItemClicked).invoke(genericThreatModel)
    }

    private fun buildThreatItem(threatModel: ThreatModel, onThreatItemClicked: ((ThreatModel) -> Unit) = mock()) =
        builder.buildThreatItem(
            threatModel = threatModel,
            onThreatItemClicked = onThreatItemClicked
        )
}
