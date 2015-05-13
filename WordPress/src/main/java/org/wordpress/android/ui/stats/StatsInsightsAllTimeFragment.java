package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.service.StatsService;


public class StatsInsightsAllTimeFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsAllTimeFragment.class.getSimpleName();

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.INSIGHTS_ALL_TIME
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_all_time);
    }

    /*
    @Override
    protected int getInsightsFragmentID() {
        return R.layout.stats_insights_generic_fragment;
    }
    */

}
