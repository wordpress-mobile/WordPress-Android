package org.wordpress.android.ui.stats.refresh.lists.detail

import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Year
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle.LIGHT
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.DateFormatSymbols
import javax.inject.Inject

class PostYearsMapper
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun mapYears(
        shownYears: List<Year>,
        expandedYearUiState: ExpandedYearUiState,
        onUiState: (ExpandedYearUiState) -> Unit
    ): MutableList<BlockListItem> {
        val yearList = mutableListOf<BlockListItem>()
        shownYears.forEachIndexed { index, year ->
            if (year.months.isNotEmpty()) {
                val isExpanded = year.year == expandedYearUiState.expandedYear
                yearList.add(
                        ExpandableItem(
                                mapYear(year, index, shownYears), isExpanded = isExpanded
                        ) { changedExpandedState ->
                            onUiState(expandedYearUiState.copy(expandedYear = if (changedExpandedState) year.year else null))
                        })
                if (isExpanded) {
                    yearList.addAll(year.months.sortedByDescending { it.month }.map { month ->
                        ListItemWithIcon(
                                text = DateFormatSymbols(localeManagerWrapper.getLocale()).shortMonths[month.month - 1],
                                value = month.count.toFormattedString(locale = localeManagerWrapper.getLocale()),
                                textStyle = LIGHT,
                                showDivider = false
                        )
                    })
                    yearList.add(Divider)
                }
            } else {
                yearList.add(
                        mapYear(year, index, shownYears)
                )
            }
        }
        return yearList
    }

    private fun mapYear(
        year: Year,
        index: Int,
        shownYears: List<Year>
    ): ListItemWithIcon {
        return ListItemWithIcon(
                text = year.year.toString(),
                value = year.value.toFormattedString(locale = localeManagerWrapper.getLocale()),
                showDivider = index < shownYears.size - 1
        )
    }

    data class ExpandedYearUiState(val expandedYear: Int? = null)
}
