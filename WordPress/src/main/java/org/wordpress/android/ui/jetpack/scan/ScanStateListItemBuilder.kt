package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ButtonAction
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ScanIdleState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ScanScanningState
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@Reusable
class ScanStateListItemBuilder @Inject constructor(
    private val dateProvider: DateProvider,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val resourceProvider: ResourceProvider
) {
    fun mapToScanState(
        model: ScanStateModel,
        site: SiteModel,
        onScanButtonClicked: () -> Unit,
        onFixAllButtonClicked: () -> Unit
    ): ScanState {
        return when (model.state) {
            ScanStateModel.State.IDLE -> {
                model.threats?.let { threats ->
                    mapToThreatsFound(
                        threats,
                        site,
                        onScanButtonClicked,
                        onFixAllButtonClicked
                    )
                } ?: mapToThreatsNotFound(model, onScanButtonClicked)
            }
            ScanStateModel.State.SCANNING -> ScanScanningState()
            ScanStateModel.State.PROVISIONING, ScanStateModel.State.UNAVAILABLE, ScanStateModel.State.UNKNOWN ->
                ScanScanningState() // TODO: ashiagr filter out
        }
    }

    private fun mapToThreatsFound(
        threats: List<ThreatModel>,
        site: SiteModel,
        onScanButtonClicked: () -> Unit,
        onFixAllButtonClicked: () -> Unit
    ): ScanIdleState.ThreatsFound {
        val fixableThreatsFound = threats.any { it.baseThreatModel.fixable != null }
        return ScanIdleState.ThreatsFound(
            scanDescription = buildThreatsFoundDescription(site, threats.size),
            scanAction = buildScanButtonAction(R.string.scan_again, onScanButtonClicked),
            fixAllAction = buildFixAllButtonAction(onFixAllButtonClicked).takeIf { fixableThreatsFound }
        )
    }

    private fun mapToThreatsNotFound(scanStateModel: ScanStateModel, onScanButtonClicked: () -> Unit) =
        ScanIdleState.ThreatsNotFound(
            scanDescription = scanStateModel.mostRecentStatus?.startDate?.time?.let {
                buildLastScanDescription(
                    it
                )
            }
                ?: UiStringRes(R.string.scan_idle_manual_scan_description),
            scanAction = buildScanButtonAction(R.string.scan_now, onScanButtonClicked)
        )

    private fun buildScanButtonAction(@StringRes titleRes: Int, onScanButtonClicked: () -> Unit) = ButtonAction(
        title = UiStringRes(titleRes),
        visibility = true,
        onClicked = onScanButtonClicked
    )

    private fun buildFixAllButtonAction(onFixAllButtonClicked: () -> Unit) = ButtonAction(
        title = UiStringRes(R.string.threats_fix_all),
        visibility = true,
        onClicked = onFixAllButtonClicked
    )

    private fun buildLastScanDescription(timeInMs: Long): UiStringResWithParams {
        val durationInMs = dateProvider.getCurrentDate().time - timeInMs
        val hours = durationInMs / ONE_HOUR
        val minutes = durationInMs / ONE_MINUTE
        val displayDuration = when {
            hours > 0 -> UiStringResWithParams(R.string.scan_in_hours_ago, listOf(UiStringText("${hours.toInt()}")))
            minutes > 0 -> UiStringResWithParams(
                R.string.scan_in_minutes_ago,
                listOf(UiStringText("${minutes.toInt()}"))
            )
            else -> UiStringRes(R.string.scan_in_few_seconds)
        }

        return UiStringResWithParams(
            R.string.scan_idle_last_scan_description,
            listOf(displayDuration, UiStringRes(R.string.scan_idle_manual_scan_description))
        )
    }

    private fun buildThreatsFoundDescription(site: SiteModel, threatsCount: Int) = UiStringText(
        htmlMessageUtils
            .getHtmlMessageFromStringFormatResId(
                R.string.scan_idle_threats_found_description,
                "<b>$threatsCount</b>",
                "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>"
            )
    )

    companion object {
        private const val ONE_MINUTE = 60 * 1000L
        private const val ONE_HOUR = 60 * ONE_MINUTE
    }
}
