package org.wordpress.android.ui.stats.refresh.utils

import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.stats_date_selector.*
import kotlinx.android.synthetic.main.stats_list_fragment.*
import org.wordpress.android.ui.stats.refresh.StatsViewModel.DateSelectorUiModel

fun Fragment.drawDateSelector(dateSelectorUiModel: DateSelectorUiModel?) {
    val dateSelectorVisibility = if (dateSelectorUiModel?.isVisible == true) View.VISIBLE else View.GONE
    if (date_selection_toolbar.visibility != dateSelectorVisibility) {
        date_selection_toolbar.visibility = dateSelectorVisibility
    }
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
