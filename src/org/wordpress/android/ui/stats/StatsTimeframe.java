package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * Timeframes for the stats pages. 
 */
public enum StatsTimeframe {

    TODAY(R.string.stats_timeframe_today),
    YESTERDAY(R.string.stats_timeframe_yesterday),
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
