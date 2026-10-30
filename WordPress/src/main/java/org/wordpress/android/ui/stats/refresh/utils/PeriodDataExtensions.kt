package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.fluxc.model.stats.time.PeriodData

/**
 * Trims leading periods that contain no data (both views and visitors are null or zero)
 * from a stats date range, so charts start at the first period with actual data instead
 * of showing empty years/days since the site was created.
 *
 * See https://github.com/wordpress-mobile/WordPress-Android/issues/21190
 */
fun List<PeriodData>.trimLeadingEmptyPeriods(): List<PeriodData> {
    val firstIndexWithData = indexOfFirst { it.hasAnyData() }
    return if (firstIndexWithData <= 0) {
        this
    } else {
        drop(firstIndexWithData)
    }
}

private fun PeriodData.hasAnyData(): Boolean = views > 0L || visitors > 0L