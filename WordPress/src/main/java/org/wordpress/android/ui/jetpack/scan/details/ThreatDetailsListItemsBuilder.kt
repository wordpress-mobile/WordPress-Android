package org.wordpress.android.ui.jetpack.scan.details

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable.FixType
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatDetailHeaderState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatFileNameState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

@Reusable
class ThreatDetailsListItemsBuilder @Inject constructor(
    private val htmlMessageUtils: HtmlMessageUtils,
    private val threatItemBuilder: ThreatItemBuilder
) {
    fun buildThreatDetailsListItems(
        threatModel: ThreatModel,
        onFixThreatButtonClicked: () -> Unit,
        onGetFreeEstimateButtonClicked: () -> Unit,
        onIgnoreThreatButtonClicked: () -> Unit
    ) = mutableListOf<JetpackListItemState>().apply {
        addAll(buildBasicThreatDetailsListItems(threatModel))
        addAll(buildTechnicalDetailsListItems(threatModel))
        addAll(buildFixDetailsListItems(threatModel))
        addAll(
            buildActionButtons(
                threatModel,
                onFixThreatButtonClicked,
                onGetFreeEstimateButtonClicked,
                onIgnoreThreatButtonClicked
            )
        )
    }.toList()

    private fun buildBasicThreatDetailsListItems(threatModel: ThreatModel) =
        mutableListOf<JetpackListItemState>().apply {
            add(buildThreatDetailHeader(threatModel))
            add(buildThreatHeaderItem(threatModel))
            add(buildThreatSubHeaderItem(threatModel))
            add(buildProblemHeaderItem())
            add(buildProblemDescriptionItem(threatModel))
        }

    private fun buildTechnicalDetailsListItems(threatModel: ThreatModel): List<JetpackListItemState> {
        var fileName: String? = null
        var threatContext: ThreatContext? = null
        var diff: String? = null

        when (threatModel) {
            is ThreatModel.FileThreatModel -> {
                fileName = threatModel.fileName
                threatContext = threatModel.context
            }

            is ThreatModel.CoreFileModificationThreatModel -> {
                fileName = threatModel.fileName
                diff = threatModel.diff
            }

            is ThreatModel.DatabaseThreatModel,
            is ThreatModel.GenericThreatModel,
            is ThreatModel.VulnerableExtensionThreatModel -> { // Do Nothing
            }
        }

        return mutableListOf<JetpackListItemState>().apply {
            fileName?.let {
                add(buildFileNameDescription())
                add(buildFileName(it))
            }
            threatContext?.let { add(buildThreatContextLines(threatContext)) }
            diff?.let { add(buildDiff(diff)) }

            if (isNotEmpty()) {
                add(0, buildTechnicalDetailsHeaderItem())
            }
        }
    }

    private fun buildFixDetailsListItems(threatModel: ThreatModel) = mutableListOf<JetpackListItemState>().apply {
        with(threatModel.baseThreatModel) {
            if (status != ThreatStatus.FIXED) {
                add(buildFixTitleHeader(status, fixable))
                add(buildFixDescription(fixable))
            }
        }
    }

    private fun buildActionButtons(
        threatModel: ThreatModel,
        onFixThreatButtonClicked: () -> Unit,
        onGetFreeEstimateButtonClicked: () -> Unit,
        onIgnoreThreatButtonClicked: () -> Unit
    ) = mutableListOf<JetpackListItemState>().apply {
        with(threatModel.baseThreatModel) {
            val isFixable = fixable != null
            if (status != ThreatStatus.FIXED) {
                if (isFixable) {
                    add(buildFixThreatButtonAction(onFixThreatButtonClicked))
                } else {
                    add(buildGetFreeEstimateButtonAction(onGetFreeEstimateButtonClicked))
                }
            }
            if (status == ThreatStatus.CURRENT) {
                add(buildIgnoreThreatButtonAction(onIgnoreThreatButtonClicked))
            }
        }
    }

    private fun buildThreatDetailHeader(threatModel: ThreatModel) = ThreatDetailHeaderState(
            icon = threatItemBuilder.buildThreatItemIcon(threatModel),
            iconBackground = threatItemBuilder.buildThreatItemIconBackground(threatModel),
            header = threatItemBuilder.buildThreatItemHeader(threatModel),
            description = UiStringText("TEST")
    )

    private fun buildThreatHeaderItem(threatModel: ThreatModel) = HeaderState(
        text = threatItemBuilder.buildThreatItemHeader(threatModel),
        textColorRes = R.attr.colorError
    )

    private fun buildThreatSubHeaderItem(threatModel: ThreatModel) =
        DescriptionState(threatItemBuilder.buildThreatItemSubHeader(threatModel))

    private fun buildProblemHeaderItem() = HeaderState(UiStringRes(R.string.threat_problem_header))

    private fun buildProblemDescriptionItem(threatModel: ThreatModel) =
        DescriptionState(UiStringText(threatModel.baseThreatModel.description))

    private fun buildTechnicalDetailsHeaderItem() = HeaderState(UiStringRes(R.string.threat_technical_details_header))

    private fun buildFileNameDescription() = DescriptionState(UiStringRes(R.string.threat_file_description))

    private fun buildFileName(fileName: String) = ThreatFileNameState(UiStringText(fileName))

    private fun buildThreatContextLines(context: ThreatContext) =
        ThreatContextLinesItemState(lines = context.lines.map { buildThreatContextLine(it) })

    private fun buildThreatContextLine(
        line: ThreatContext.ContextLine
    ): ThreatContextLinesItemState.ThreatContextLineItemState {
        val isHighlighted = line.highlights?.isNotEmpty() == true

        val lineNumberBackgroundColorRes = if (isHighlighted) R.color.pink_5 else R.color.gray_20
        val contentBackgroundColorRes = if (isHighlighted) R.color.pink_5 else R.color.gray_5

        return ThreatContextLinesItemState.ThreatContextLineItemState(
            line = line,
            lineNumberBackgroundColorRes = lineNumberBackgroundColorRes,
            contentBackgroundColorRes = contentBackgroundColorRes,
            highlightedBackgroundColorRes = R.color.red,
            highlightedTextColorRes = R.color.white,
            normalTextColorRes = R.color.black
        )
    }

    private fun buildDiff(diff: String) = DescriptionState(UiStringText(diff)) // TODO: ashiagr custom diff view?

    private fun buildFixTitleHeader(status: ThreatStatus, fixable: Fixable?) = HeaderState(
        UiStringRes(
            when (status) {
                ThreatStatus.FIXED -> R.string.threat_fix_fixed_header

                ThreatStatus.CURRENT ->
                    fixable?.let { R.string.threat_fix_current_fixable_header }
                        ?: R.string.threat_fix_current_not_fixable_header

                ThreatStatus.IGNORED, ThreatStatus.UNKNOWN -> R.string.threat_fix_default_header
            }
        )
    )

    private fun buildFixDescription(fixable: Fixable?) = fixable?.let { buildFixableThreatDescription(it) }
        ?: buildNotFixableThreatDescription()

    fun buildFixableThreatDescription(fixable: Fixable) = DescriptionState(
        when (fixable.fixer) {
            FixType.REPLACE -> UiStringRes(R.string.threat_fix_fixable_replace)

            FixType.DELETE -> UiStringRes(R.string.threat_fix_fixable_delete)

            FixType.UPDATE -> fixable.target?.let {
                UiStringResWithParams(R.string.threat_fix_fixable_update, listOf(UiStringText(it)))
            } ?: UiStringRes(R.string.threat_fix_fixable_default)

            FixType.EDIT -> UiStringRes(R.string.threat_fix_fixable_edit)

            FixType.UNKNOWN -> UiStringRes(R.string.threat_fix_fixable_default)
        }
    )

    private fun buildNotFixableThreatDescription() = DescriptionState(
        UiStringText(
            htmlMessageUtils.getHtmlMessageFromStringFormatResId(R.string.threat_fix_current_not_fixable_description)
        )
    )

    private fun buildFixThreatButtonAction(onFixThreatButtonClicked: () -> Unit) = ActionButtonState(
        text = UiStringRes(R.string.threat_fix),
        onClick = onFixThreatButtonClicked,
        contentDescription = UiStringRes(R.string.threat_fix)
    )

    private fun buildGetFreeEstimateButtonAction(onGetFreeEstimateButtonClicked: () -> Unit) = ActionButtonState(
        text = UiStringRes(R.string.threat_get_free_estimate),
        onClick = onGetFreeEstimateButtonClicked,
        contentDescription = UiStringRes(R.string.threat_get_free_estimate)
    )

    private fun buildIgnoreThreatButtonAction(onIgnoreThreatButtonClicked: () -> Unit) = ActionButtonState(
        text = UiStringRes(R.string.threat_ignore),
        onClick = onIgnoreThreatButtonClicked,
        contentDescription = UiStringRes(R.string.threat_ignore),
        isSecondary = true
    )
}
