package org.wordpress.android.ui.jetpack.scan.builders

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.CURRENT
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.TEST_FILE_NAME
import org.wordpress.android.ui.jetpack.scan.TEST_SIGNATURE
import org.wordpress.android.ui.jetpack.scan.TEST_VULNERABLE_THREAT_SLUG
import org.wordpress.android.ui.jetpack.scan.TEST_VULNERABLE_THREAT_VERSION
import org.wordpress.android.ui.jetpack.scan.ThreatTestData
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
        whenever(dateFormat.format(anyOrNull()))
                .thenReturn(TEST_FIXED_ON_DATE)
    }

    @Test
    fun `builds threat sub header correctly for fixed threat`() {
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = FIXED))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.subHeader).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_sub_header_status_fixed_on,
                        listOf(UiStringText(TEST_FIXED_ON_DATE))
                )
        )
    }

    @Test
    fun `builds threat sub header correctly for ignored threat`() {
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = IGNORED))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.subHeader).isEqualTo(UiStringRes(R.string.threat_item_sub_header_status_ignored))
    }

    @Test
    fun `builds threat header correctly for CoreFileModificationThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItem.header).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_header_infected_core_file,
                        listOf(UiStringText(TEST_FILE_NAME))
                )
        )
    }

    @Test
    fun `builds threat sub header correctly for CoreFileModificationThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.coreFileModificationThreatModel)

        assertThat(threatItem.subHeader).isEqualTo(UiStringRes(R.string.threat_item_sub_header_core_file))
    }

    @Test
    fun `builds threat header correctly for DatabaseThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.databaseThreatModel)

        assertThat(threatItem.header).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_header_database_threat,
                        listOf(UiStringText("${ThreatTestData.rows.size}"))
                )
        )
    }

    @Test
    fun `builds threat sub header correctly for DatabaseThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.databaseThreatModel)

        assertThat(threatItem.subHeader).isNull()
    }

    @Test
    fun `builds threat header correctly for FileThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.fileThreatModel)

        assertThat(threatItem.header).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_header_file_malicious_code_pattern,
                        listOf(UiStringText(TEST_FILE_NAME))
                )
        )
    }

    @Test
    fun `builds threat sub header correctly for FileThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.fileThreatModel)

        assertThat(threatItem.subHeader).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_sub_header_file_signature,
                        listOf(UiStringText(TEST_SIGNATURE))
                )
        )
    }

    @Test
    fun `builds threat header correctly for VulnerableExtensionThreatModel of Plugin type`() {
        val threatItem = buildThreatItem(ThreatTestData.vulnerableExtensionThreatModel)

        assertThat(threatItem.header).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_header_vulnerable_plugin,
                        listOf(
                                UiStringText(TEST_VULNERABLE_THREAT_SLUG),
                                UiStringText(
                                        TEST_VULNERABLE_THREAT_VERSION
                                )
                        )
                )
        )
    }

    @Test
    fun `builds threat sub header correctly for VulnerableExtensionThreatModel of Plugin type`() {
        val threatItem = buildThreatItem(ThreatTestData.vulnerableExtensionThreatModel)

        assertThat(threatItem.subHeader).isEqualTo(UiStringRes(R.string.threat_item_sub_header_vulnerable_plugin))
    }

    @Test
    fun `builds threat header correctly for VulnerableExtensionThreatModel of Theme type`() {
        val threatItem = buildThreatItem(
                ThreatTestData.vulnerableExtensionThreatModel.copy(
                        extension = ThreatTestData.extension.copy(type = ExtensionType.THEME)
                )
        )

        assertThat(threatItem.header).isEqualTo(
                UiStringResWithParams(
                        R.string.threat_item_header_vulnerable_theme,
                        listOf(
                                UiStringText(TEST_VULNERABLE_THREAT_SLUG),
                                UiStringText(
                                        TEST_VULNERABLE_THREAT_VERSION
                                )
                        )
                )
        )
    }

    @Test
    fun `builds threat sub header correctly for VulnerableExtensionThreatModel of Theme type`() {
        val threatItem = buildThreatItem(
                ThreatTestData.vulnerableExtensionThreatModel.copy(
                        extension = ThreatTestData.extension.copy(type = ExtensionType.THEME)
                )
        )

        assertThat(threatItem.subHeader).isEqualTo(UiStringRes(R.string.threat_item_sub_header_vulnerable_theme))
    }

    @Test
    fun `builds threat header correctly for GenericThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.genericThreatModel)

        assertThat(threatItem.header).isEqualTo(UiStringRes(R.string.threat_item_header_threat_found))
    }

    @Test
    fun `builds threat sub header correctly for GenericThreatModel`() {
        val threatItem = buildThreatItem(ThreatTestData.genericThreatModel)

        assertThat(threatItem.subHeader).isEqualTo(UiStringRes(R.string.threat_item_sub_header_misc_vulnerability))
    }

    @Test
    fun `displays loading for threat in fixing state`() {
        val threatItem = buildThreatItem(threatModel = ThreatTestData.genericThreatModel, isFixing = true)

        assertThat(threatItem.isLoadingVisible).isTrue
    }

    @Test
    fun `displays icon for threat not in fixing state`() {
        val threatItem = buildThreatItem(threatModel = ThreatTestData.genericThreatModel, isFixing = false)

        assertThat(threatItem.isIconVisible).isTrue
    }

    @Test
    fun `onThreatItemClicked listener is correctly assigned to ThreatItem's onClick`() = test {
        val onThreatItemClicked: (Long) -> Unit = mock()
        val threatItem = buildThreatItem(ThreatTestData.genericThreatModel, onThreatItemClicked)

        threatItem.onClick.invoke()

        verify(onThreatItemClicked).invoke(ThreatTestData.genericThreatModel.baseThreatModel.id)
    }

    @Test
    fun `Shield icon and green background is used for fixed threat`() {
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = FIXED))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.icon).isEqualTo(R.drawable.ic_shield_tick_white)
        assertThat(threatItem.iconBackground).isEqualTo(R.drawable.bg_oval_success_50)
    }

    @Test
    fun `Notice icon and grey background is used for ignored threat`() {
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = IGNORED))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.icon).isEqualTo(R.drawable.ic_notice_outline_white_24dp)
        assertThat(threatItem.iconBackground).isEqualTo(R.drawable.bg_oval_neutral_30)
    }

    @Test
    fun `Notice icon and red background is used for current threat`() {
        val threatModel = GenericThreatModel(ThreatTestData.genericThreatModel.baseThreatModel.copy(status = CURRENT))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.icon).isEqualTo(R.drawable.ic_notice_outline_white_24dp)
        assertThat(threatItem.iconBackground).isEqualTo(R.drawable.bg_oval_error_50)
    }

    @Test
    fun `Subheader for fixed threat has success color`() {
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = FIXED))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.subHeaderColor).isEqualTo(R.attr.wpColorSuccess)
    }

    @Test
    fun `sub header for current threat has onSurface color`() {
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = CURRENT))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.subHeaderColor).isEqualTo(R.attr.colorOnSurface)
    }

    @Test
    fun `sub header for ignored threat has onSurface color`() {
        val threatModel = GenericThreatModel(ThreatTestData.baseThreatModel.copy(status = IGNORED))

        val threatItem = buildThreatItem(threatModel)

        assertThat(threatItem.subHeaderColor).isEqualTo(R.attr.colorOnSurface)
    }

    private fun buildThreatItem(
        threatModel: ThreatModel,
        onThreatItemClicked: ((Long) -> Unit) = mock(),
        isFixing: Boolean = false
    ) = builder.buildThreatItem(
            threatModel = threatModel,
            isFixing = isFixing,
            onThreatItemClicked = onThreatItemClicked
    )
}
