package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;


public class StatsInsightsAllTimeFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsAllTimeFragment.class.getSimpleName();

    private InsightsAllTimeModel mInsightsAllTimeModel;

    @Override
    protected boolean hasDataAvailable() {
        return mInsightsAllTimeModel != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mInsightsAllTimeModel);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mInsightsAllTimeModel = (InsightsAllTimeModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.InsightsAllTimeUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mInsightsAllTimeModel = event.mInsightsAllTimeModel;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mInsightsAllTimeModel = null;
        showErrorUI(event.mError);
    }


    protected void updateUI() {
        super.updateUI();

        if (!isAdded() || !hasDataAvailable()) {
            return;
        }

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_all_time_item, (ViewGroup) mResultContainer.getRootView(), false);

        TextView postsTextView = (TextView) ll.findViewById(R.id.stats_all_time_posts);
        TextView viewsTextView = (TextView) ll.findViewById(R.id.stats_all_time_views);
        TextView visitorsTextView = (TextView) ll.findViewById(R.id.stats_all_time_visitors);
        TextView besteverTextView = (TextView) ll.findViewById(R.id.stats_all_time_bestever);
        TextView besteverDateTextView = (TextView) ll.findViewById(R.id.stats_all_time_bestever_date);


        postsTextView.setText(FormatUtils.formatDecimal(mInsightsAllTimeModel.getPosts()));
        viewsTextView.setText(FormatUtils.formatDecimal(mInsightsAllTimeModel.getViews()));
        visitorsTextView.setText(FormatUtils.formatDecimal(mInsightsAllTimeModel.getVisitors()));

        besteverTextView.setText(FormatUtils.formatDecimal(mInsightsAllTimeModel.getViewsBestDayTotal()));
        besteverDateTextView.setText(
                StatsUtils.parseDate(mInsightsAllTimeModel.getViewsBestDay(), StatsConstants.STATS_INPUT_DATE_FORMAT, "MMMM dd, yyyy")
        );

        mResultContainer.addView(ll);
    }


    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.INSIGHTS_ALL_TIME
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_all_time);
    }

}
