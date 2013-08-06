
package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum StatsTimeframe {

    TODAY(R.string.stats_timeframe_today),
    YESTERDAY(R.string.stats_timeframe_yesterday),
    WEEK(R.string.stats_timeframe_week),
    MONTH(R.string.stats_timeframe_month),
    QUARTER(R.string.stats_timeframe_quarter),
    YEAR(R.string.stats_timeframe_year),
    ALL_TIME(R.string.stats_timeframe_all_time), ;

    private int mLabelResId;

    private StatsTimeframe(int labelResId) {
        mLabelResId = labelResId;
    }

    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }

}
