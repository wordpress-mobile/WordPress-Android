package org.wordpress.android.ui.jetpack.scan.builders

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState.ClickableTextInfo
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatsHeaderItemState
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@Reusable
class ScanStateListItemsBuilder @Inject constructor(
    private val dateProvider: DateProvider,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val resourceProvider: ResourceProvider,
    private val threatItemBuilder: ThreatItemBuilder
) {
    fun buildScanStateListItems(
        model: ScanStateModel,
        site: SiteModel,
        fixingThreatIds: List<Long>,
        onScanButtonClicked: () -> Unit,
        onFixAllButtonClicked: () -> Unit,
        onThreatItemClicked: (threatId: Long) -> Unit,
        onHelpClicked: () -> Unit
    ): List<JetpackListItemState> {
        return if (fixingThreatIds.isNotEmpty()) {
            buildThreatsFixingStateItems(
                threats = model.threats,
                site = site,
                fixingThreatIds = fixingThreatIds,
                onHelpClicked = onHelpClicked
            )
        } else when (model.state) {
            ScanStateModel.State.IDLE -> {
                model.threats?.takeIf { threats -> threats.isNotEmpty() }?.let { threats ->
                    buildThreatsFoundStateItems(
                        threats,
                        site,
                        onScanButtonClicked,
                        onFixAllButtonClicked,
                        onThreatItemClicked,
                        onHelpClicked
                    )
                } ?: buildThreatsNotFoundStateItems(model, onScanButtonClicked)
            }
            ScanStateModel.State.SCANNING -> buildScanningStateItems(model.mostRecentStatus, model.currentStatus)
            ScanStateModel.State.PROVISIONING -> buildProvisioningStateItems()
            ScanStateModel.State.UNAVAILABLE, ScanStateModel.State.UNKNOWN -> emptyList()
        }
    }

    private fun buildThreatsFixingStateItems(
        threats: List<ThreatModel>?,
        site: SiteModel,
        fixingThreatIds: List<Long>,
        onHelpClicked: () -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()

        val scanIcon = buildScanIcon(R.drawable.ic_shield_warning_white, R.color.error)
        val scanHeader = HeaderState(UiStringRes(R.string.scan_fixing_threats_title))
        // TODO ashiagr replace label
        val scanDescription = buildThreatsFoundDescription(site, fixingThreatIds.size, onHelpClicked)
        val scanProgress = ProgressState(
            isIndeterminate = true,
            isVisible = fixingThreatIds.isNotEmpty()
        )

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)
        items.add(scanProgress)

        // TODO ashiagr fix threats display logic
        threats?.takeIf { it.isNotEmpty() && fixingThreatIds.isNotEmpty() }?.let {
            items.add(ThreatsHeaderItemState())
            items.addAll(
                threats.filter { it.baseThreatModel.id in fixingThreatIds }.map { threat ->
                    threatItemBuilder.buildThreatItem(threat)
                }
            )
        }

        return items
    }

    private fun buildThreatsFoundStateItems(
        threats: List<ThreatModel>,
        site: SiteModel,
        onScanButtonClicked: () -> Unit,
        onFixAllButtonClicked: () -> Unit,
        onThreatItemClicked: (threatId: Long) -> Unit,
        onHelpClicked: () -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()

        val scanIcon = buildScanIcon(R.drawable.ic_shield_warning_white, R.color.error)
        val scanHeader = HeaderState(UiStringRes(R.string.scan_idle_threats_found_title))
        val scanDescription = buildThreatsFoundDescription(site, threats.size, onHelpClicked)
        val scanButton = buildScanButtonAction(titleRes = R.string.scan_again, onClick = onScanButtonClicked)
        val scanProgress = ProgressState(
            progressStateLabel = UiStringRes(R.string.threat_fixing),
            isIndeterminate = true,
            isVisible = false
        )

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)
        items.add(scanProgress)

        val fixableThreats = threats.filter { it.baseThreatModel.fixable != null }
        buildFixAllButtonAction(onFixAllButtonClicked, fixableThreats.size).takeIf { fixableThreats.isNotEmpty() }
            ?.let { items.add(it) }

        items.add(scanButton)

        threats.takeIf { it.isNotEmpty() }?.let {
            items.add(ThreatsHeaderItemState())
            items.addAll(threats.map { threat -> threatItemBuilder.buildThreatItem(threat, onThreatItemClicked) })
        }

        return items
    }

    private fun buildThreatsNotFoundStateItems(
        scanStateModel: ScanStateModel,
        onScanButtonClicked: () -> Unit
    ): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()

        val scanIcon = buildScanIcon(R.drawable.ic_shield_tick_white, R.color.jetpack_green_40)
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

    private fun buildScanningStateItems(
        mostRecentStatus: ScanProgressStatus?,
        currentProgress: ScanProgressStatus?
    ): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()
        // TODO: ashiagr replace icon with stroke, using direct icon (color = null) causing issues with dynamic tinting
        val progress = currentProgress?.progress ?: 0
        val scanIcon = buildScanIcon(R.drawable.ic_shield_white, R.color.jetpack_green_5)
        val scanTitleRes = if (progress == 0) R.string.scan_preparing_to_scan_title else R.string.scan_scanning_title
        val scanHeader = HeaderState(UiStringRes(scanTitleRes))
        val descriptionRes = if (mostRecentStatus?.isInitial == true) {
            R.string.scan_scanning_is_initial_description
        } else {
            R.string.scan_scanning_description
        }
        val scanDescription = DescriptionState(UiStringRes(descriptionRes))
        val scanProgress = ProgressState(
            progress = progress,
            progressLabel = UiStringResWithParams(
                R.string.scan_progress_label,
                listOf(UiStringText(progress.toString()))
            )
        )

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)
        items.add(scanProgress)

        return items
    }

    private fun buildProvisioningStateItems(): List<JetpackListItemState> {
        val items = mutableListOf<JetpackListItemState>()
        val scanIcon = buildScanIcon(R.drawable.ic_shield_white, R.color.jetpack_green_5)
        val scanHeader = HeaderState(UiStringRes(R.string.scan_preparing_to_scan_title))
        val scanDescription = DescriptionState(UiStringRes(R.string.scan_provisioning_description))

        items.add(scanIcon)
        items.add(scanHeader)
        items.add(scanDescription)

        return items
    }

    private fun buildScanIcon(@DrawableRes icon: Int, @ColorRes color: Int?) = IconState(
        icon = icon,
        colorResId = color,
        sizeResId = R.dimen.scan_icon_size,
        marginResId = R.dimen.scan_icon_margin,
        contentDescription = UiStringRes(R.string.scan_state_icon)
    )

    private fun buildScanButtonAction(@StringRes titleRes: Int, onClick: () -> Unit) = ActionButtonState(
        text = UiStringRes(titleRes),
        onClick = onClick,
        contentDescription = UiStringRes(titleRes),
        isSecondary = true
    )

    private fun buildFixAllButtonAction(
        onFixAllButtonClicked: () -> Unit,
        fixableThreatsCount: Int
    ): ActionButtonState {
        val title = UiStringResWithParams(
            R.string.threats_fix_num_of_threats,
            listOf(UiStringText("$fixableThreatsCount"))
        )
        return ActionButtonState(
            text = title,
            onClick = onFixAllButtonClicked,
            contentDescription = title
        )
    }

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

    private fun buildThreatsFoundDescription(
        site: SiteModel,
        threatsCount: Int,
        onHelpClicked: () -> Unit
    ): DescriptionState {
        val descriptionText = htmlMessageUtils
            .getHtmlMessageFromStringFormatResId(
                R.string.scan_idle_threats_found_description,
                "<b>$threatsCount</b>",
                "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>"
            )

        val clickableText = resourceProvider.getString(R.string.scan_here_to_help)
        val startIndex = descriptionText.indexOf(clickableText)
        val endIndex = startIndex + clickableText.length
        val clickableTextInfo = listOf(ClickableTextInfo(startIndex, endIndex, onHelpClicked))

        return DescriptionState(
            text = UiStringText(descriptionText),
            clickableTextsInfo = clickableTextInfo
        )
    }

    companion object {
        private const val ONE_MINUTE = 60 * 1000L
        private const val ONE_HOUR = 60 * ONE_MINUTE
    }
}
