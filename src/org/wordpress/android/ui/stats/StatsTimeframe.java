
package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum StatsTimeframe {

    TODAY(0, R.string.stats_timeframe_today),
    YESTERDAY(1, R.string.stats_timeframe_yesterday),
    WEEK(7, R.string.stats_timeframe_week),
    MONTH(30, R.string.stats_timeframe_month),
    QUARTER(90, R.string.stats_timeframe_quarter),
    YEAR(365, R.string.stats_timeframe_year),
    ALL_TIME(-1, R.string.stats_timeframe_all_time), ;

    private int mTimeframe;
    private int mLabelResId;

    private StatsTimeframe(int timeframe, int labelResId) {
        mTimeframe = timeframe;
        mLabelResId = labelResId;
    }

    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }

    public int toInt() {
        return mTimeframe;
    }

}
