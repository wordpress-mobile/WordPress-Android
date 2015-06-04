package org.wordpress.android.ui.stats;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;


public class StatsInsightsAllTimeFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsAllTimeFragment.class.getSimpleName();


    void customizeUIWithResults() {
        // Another check that the data is available
        if (isDataEmpty(0) || !(mDatamodels[0] instanceof InsightsAllTimeModel)) {
            showErrorUI(null);
            return;
        }

        InsightsAllTimeModel data = (InsightsAllTimeModel) mDatamodels[0];

        mResultContainer.removeAllViews();

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_all_time_item, (ViewGroup) mResultContainer.getRootView(), false);

        TextView postsTextView = (TextView) ll.findViewById(R.id.stats_all_time_posts);
        TextView viewsTextView = (TextView) ll.findViewById(R.id.stats_all_time_views);
        TextView visitorsTextView = (TextView) ll.findViewById(R.id.stats_all_time_visitors);
        TextView besteverTextView = (TextView) ll.findViewById(R.id.stats_all_time_bestever);
        TextView besteverDateTextView = (TextView) ll.findViewById(R.id.stats_all_time_bestever_date);


        postsTextView.setText(FormatUtils.formatDecimal(data.getPosts()));
        viewsTextView.setText(FormatUtils.formatDecimal(data.getViews()));
        visitorsTextView.setText(FormatUtils.formatDecimal(data.getVisitors()));

        besteverTextView.setText(FormatUtils.formatDecimal(data.getViewsBestDayTotal()));
        besteverDateTextView.setText(
                StatsUtils.parseDate(data.getViewsBestDay(), StatsConstants.STATS_INPUT_DATE_FORMAT, "MMMM dd, yyyy")
        );

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
