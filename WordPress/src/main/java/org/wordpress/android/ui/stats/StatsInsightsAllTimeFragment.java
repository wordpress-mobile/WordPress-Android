package org.wordpress.android.ui.stats;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.models.InsightsPopularModel;
import org.wordpress.android.ui.stats.service.StatsService;


public class StatsInsightsAllTimeFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsAllTimeFragment.class.getSimpleName();


    void customizeUIWithResults() {
        mResultContainer.removeAllViews();

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_all_time_item, (ViewGroup) mResultContainer.getRootView(), false);

        TextView stats_all_time_posts = (TextView) ll.findViewById(R.id.stats_all_time_posts);
        TextView stats_all_time_views = (TextView) ll.findViewById(R.id.stats_all_time_views);
        TextView stats_all_time_visitors = (TextView) ll.findViewById(R.id.stats_all_time_visitors);
        TextView stats_all_time_bestever = (TextView) ll.findViewById(R.id.stats_all_time_bestever);

        InsightsAllTimeModel data = (InsightsAllTimeModel) mDatamodels[0];

        stats_all_time_posts.setText(String.valueOf(data.getPosts()));
        stats_all_time_views.setText(String.valueOf(data.getViews()));
        stats_all_time_visitors.setText(String.valueOf(data.getVisitors()));
        stats_all_time_bestever.setText(data.getViews_best_day() + " " + data.getViews_best_day_total());


        mResultContainer.addView(ll);
    }


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

}
