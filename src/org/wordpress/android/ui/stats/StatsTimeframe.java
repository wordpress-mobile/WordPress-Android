
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
    ALL_TIME(R.string.stats_timeframe_all_time),
    DAYS(R.string.stats_timeframe_days),
    WEEKS(R.string.stats_timeframe_weeks),
    MONTHS(R.string.stats_timeframe_months),
    SUMMARY(R.string.stats_summary),
    ;

    private int mLabelResId;

    private StatsTimeframe(int labelResId) {
        mLabelResId = labelResId;
    }

    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }
    
    public static String[] toStringArray(StatsTimeframe[] timeframes) {
        String[] titles = new String[timeframes.length];
        
        for (int i = 0; i < timeframes.length; i++) {
            titles[i] = timeframes[i].getLabel();
        }
        
        return titles;
    }

}
