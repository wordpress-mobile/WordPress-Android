package org.wordpress.android.ui.jetpack.scan.builders

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.CoreFileModificationThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.VulnerableExtensionThreatModel.Extension.ExtensionType
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

@Reusable
class ThreatItemBuilder @Inject constructor() {
    fun buildThreatItem(threatModel: ThreatModel, onThreatItemClicked: (threatId: Long) -> Unit) =
        ThreatItemState(
            threatId = threatModel.baseThreatModel.id,
            header = buildThreatItemHeader(threatModel),
            subHeader = buildThreatItemSubHeader(threatModel),
            onClick = { onThreatItemClicked(threatModel.baseThreatModel.id) }
        )

    fun buildThreatItemHeader(threatModel: ThreatModel) = when (threatModel) {
        is CoreFileModificationThreatModel -> UiStringResWithParams(
            R.string.threat_item_header_infected_core_file,
            listOf(UiStringText(getDisplayFileName(threatModel.fileName)))
        )

        is DatabaseThreatModel -> UiStringResWithParams(
            R.string.threat_item_header_database_threat,
            listOf(UiStringText("${threatModel.rows?.size ?: 0}"))
        )

        is FileThreatModel -> UiStringResWithParams(
            R.string.threat_item_header_file_malicious_code_pattern,
            listOf(UiStringText(getDisplayFileName(threatModel.fileName)))
        )

        is VulnerableExtensionThreatModel -> {
            val slug = threatModel.extension.slug ?: ""
            val version = threatModel.extension.version ?: ""
            when (threatModel.extension.type) {
                ExtensionType.PLUGIN -> UiStringResWithParams(
                    R.string.threat_item_header_vulnerable_plugin,
                    listOf(UiStringText(slug), UiStringText(version))
                )
                ExtensionType.THEME -> UiStringResWithParams(
                    R.string.threat_item_header_vulnerable_theme,
                    listOf(UiStringText(slug), UiStringText(version))
                )
                ExtensionType.UNKNOWN -> throw IllegalArgumentException(
                    "$UNKNOWN_VULNERABLE_EXTENSION_TYPE in ${this::class.java.simpleName}"
                )
            }
        }

        is GenericThreatModel -> UiStringRes(R.string.threat_item_header_threat_found)
    }

    fun buildThreatItemSubHeader(threatModel: ThreatModel) = when (threatModel) {
        is CoreFileModificationThreatModel -> UiStringRes(R.string.threat_item_sub_header_core_file)

        is DatabaseThreatModel -> UiStringText("")

        is FileThreatModel -> UiStringResWithParams(
            R.string.threat_item_sub_header_file_signature,
            listOf(UiStringText(threatModel.baseThreatModel.signature))
        )

        is VulnerableExtensionThreatModel -> {
            when (threatModel.extension.type) {
                ExtensionType.PLUGIN -> UiStringRes(R.string.threat_item_sub_header_vulnerable_plugin)
                ExtensionType.THEME -> UiStringRes(R.string.threat_item_sub_header_vulnerable_theme)
                ExtensionType.UNKNOWN -> throw IllegalArgumentException(
                    "$UNKNOWN_VULNERABLE_EXTENSION_TYPE in ${this::class.java.simpleName}"
                )
            }
        }

        is GenericThreatModel -> UiStringRes(R.string.threat_item_sub_header_misc_vulnerability)
    }

    /**
     * Uses regex to remove the whole path except of the file name
     * e.g. "/var/www/html/jp-scan-daily/wp-admin/index.php" returns "index.php".
     * */
    private fun getDisplayFileName(fileName: String?) = fileName?.replace(".*/".toRegex(), "") ?: ""

    companion object {
        private const val UNKNOWN_VULNERABLE_EXTENSION_TYPE = "Unexpected vulnerable extension threat type"
    }
}
