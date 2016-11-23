package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * Timeframes for the stats pages.
 */
public enum StatsTimeframe {
    INSIGHTS(R.string.stats_insights),
    DAY(R.string.stats_timeframe_days),
    WEEK(R.string.stats_timeframe_weeks),
    MONTH(R.string.stats_timeframe_months),
    YEAR(R.string.stats_timeframe_years),
    ;

    private final int mLabelResId;

    StatsTimeframe(int labelResId) {
        mLabelResId = labelResId;
    }

    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }

    public String getLabelForRestCall() {
        switch (this) {
            case WEEK:
                return "week";
            case MONTH:
                return "month";
            case YEAR:
                return "year";
            default:
                return "day";
        }
    }
}
