package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatsHeaderItemState
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
    fun buildScanStateListItems(
        model: ScanStateModel,
        site: SiteModel,
        onScanButtonClicked: () -> Unit,
        onFixAllButtonClicked: () -> Unit
    ): List<JetpackListItemState> {
        return when (model.state) {
            ScanStateModel.State.IDLE -> {
                model.threats?.takeIf { threats -> threats.isNotEmpty() }?.let { threats ->
                    buildThreatsFoundStateItems(
                        threats,
                        site,
                        onScanButtonClicked,
                        onFixAllButtonClicked
                    )
                } ?: buildThreatsNotFoundStateItems(model, onScanButtonClicked)
            }
            ScanStateModel.State.SCANNING -> buildScanningStateItems()
            ScanStateModel.State.PROVISIONING, ScanStateModel.State.UNAVAILABLE, ScanStateModel.State.UNKNOWN ->
                buildScanningStateItems() // TODO: ashiagr filter out invalid states
        }
    }

    private fun buildThreatsFoundStateItems(
        threats: List<ThreatModel>,
        site: SiteModel,
        onScanButtonClicked: () -> Unit,
        onFixAllButtonClicked: () -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()

        val scanIcon = buildScanIcon(R.drawable.ic_scan_idle_threats_found)
        val scanHeader = HeaderState(UiStringRes(R.string.scan_idle_threats_found_title))
        val scanDescription = buildThreatsFoundDescription(site, threats.size)
        val scanButton = buildScanButtonAction(titleRes = R.string.scan_again, onClick = onScanButtonClicked)

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)
        items.add(scanButton)

        val fixableThreatsFound = threats.any { it.baseThreatModel.fixable != null }
        buildFixAllButtonAction(onFixAllButtonClicked).takeIf { fixableThreatsFound }?.let { items.add(it) }

        threats.takeIf { it.isNotEmpty() }?.let {
            items.add(ThreatsHeaderItemState())
            items.addAll(threats.map { ThreatItemState(it) })
        }

        return items
    }

    private fun buildThreatsNotFoundStateItems(
        scanStateModel: ScanStateModel,
        onScanButtonClicked: () -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()

        val scanIcon = buildScanIcon(R.drawable.ic_scan_idle_threats_not_found)
        val scanHeader = HeaderState(UiStringRes(R.string.scan_idle_no_threats_found_title))
        val scanDescription = scanStateModel.mostRecentStatus?.startDate?.time?.let {
            buildLastScanDescription(it)
        } ?: DescriptionState(UiStringRes(R.string.scan_idle_manual_scan_description))
        val scanButton = buildScanButtonAction(titleRes = R.string.scan_now, onClick = onScanButtonClicked)

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)
        items.add(scanButton)

        return items
    }

    private fun buildScanningStateItems(): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()

        val scanIcon = buildScanIcon(R.drawable.ic_scan_scanning)
        val scanHeader = HeaderState(UiStringRes(R.string.scan_scanning_title))
        val scanDescription = DescriptionState(UiStringRes(R.string.scan_scanning_description))

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)

        return items
    }

    private fun buildScanIcon(@DrawableRes icon: Int) = IconState(
        icon = icon,
        contentDescription = UiStringRes(R.string.scan_state_icon)
    )

    private fun buildScanButtonAction(@StringRes titleRes: Int, onClick: () -> Unit) = ActionButtonState(
        text = UiStringRes(titleRes),
        onClick = onClick,
        contentDescription = UiStringRes(titleRes),
        isSecondary = true
    )

    private fun buildFixAllButtonAction(onFixAllButtonClicked: () -> Unit) = ActionButtonState(
        text = UiStringRes(R.string.threats_fix_all),
        onClick = onFixAllButtonClicked,
        contentDescription = UiStringRes(R.string.threats_fix_all)
    )

    private fun buildLastScanDescription(timeInMs: Long): DescriptionState {
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

        return DescriptionState(
            UiStringResWithParams(
                R.string.scan_idle_last_scan_description,
                listOf(displayDuration, UiStringRes(R.string.scan_idle_manual_scan_description))
            )
        )
    }

    private fun buildThreatsFoundDescription(site: SiteModel, threatsCount: Int) = DescriptionState(
        UiStringText(
            htmlMessageUtils
                .getHtmlMessageFromStringFormatResId(
                    R.string.scan_idle_threats_found_description,
                    "<b>$threatsCount</b>",
                    "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>"
                )
        )
    )

    companion object {
        private const val ONE_MINUTE = 60 * 1000L
        private const val ONE_HOUR = 60 * ONE_MINUTE
    }
}
