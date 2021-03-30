package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import org.wordpress.android.databinding.StatsListFragmentBinding
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel

fun StatsListFragmentBinding.drawDateSelector(dateSelectorUiModel: DateSelectorUiModel?) {
    val dateSelectorVisibility = if (dateSelectorUiModel?.isVisible == true) View.VISIBLE else View.GONE
    if (dateSelectionToolbar.visibility != dateSelectorVisibility) {
        dateSelectionToolbar.visibility = dateSelectorVisibility
    }
    with(dateSelector) {
        selectedDateTextView.text = dateSelectorUiModel?.date ?: ""
        val timeZone = dateSelectorUiModel?.timeZone
        if (currentSiteTimeZone.visibility == View.GONE && timeZone != null) {
            currentSiteTimeZone.visibility = View.VISIBLE
            currentSiteTimeZone.text = timeZone
        } else if (currentSiteTimeZone.visibility == View.VISIBLE && timeZone == null) {
            currentSiteTimeZone.visibility = View.GONE
        }
        val enablePreviousButton = dateSelectorUiModel?.enableSelectPrevious == true
        if (previousDateButton.isEnabled != enablePreviousButton) {
            previousDateButton.isEnabled = enablePreviousButton
        }
        val enableNextButton = dateSelectorUiModel?.enableSelectNext == true
        if (nextDateButton.isEnabled != enableNextButton) {
            nextDateButton.isEnabled = enableNextButton
        }
    }
}
