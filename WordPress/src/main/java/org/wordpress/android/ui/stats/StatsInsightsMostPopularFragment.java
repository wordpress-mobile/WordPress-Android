package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.service.StatsService;


public class StatsInsightsMostPopularFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsMostPopularFragment.class.getSimpleName();

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.INSIGHTS_POPULAR
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_popular);
    }
}
