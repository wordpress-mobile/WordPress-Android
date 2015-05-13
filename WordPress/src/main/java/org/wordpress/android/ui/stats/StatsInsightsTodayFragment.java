package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.service.StatsService;


public class StatsInsightsTodayFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsTodayFragment.class.getSimpleName();


    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.INSIGHTS_TODAY
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_today);
    }

}
