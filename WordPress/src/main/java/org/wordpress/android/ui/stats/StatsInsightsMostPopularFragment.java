package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.InsightsPopularModel;
import org.wordpress.android.ui.stats.service.StatsService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class StatsInsightsMostPopularFragment extends StatsAbstractInsightsFragment {
    public static final String TAG = StatsInsightsMostPopularFragment.class.getSimpleName();

    private InsightsPopularModel mInsightsPopularModel;

    @Override
    protected boolean hasDataAvailable() {
        return mInsightsPopularModel != null;
    }
    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mInsightsPopularModel);
        }
    }
    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
            mInsightsPopularModel = (InsightsPopularModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.InsightsPopularUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mInsightsPopularModel = event.mInsightsPopularModel;
        updateUI();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mInsightsPopularModel = null;
        showErrorUI(event.mError);
    }

    protected void updateUI() {
        super.updateUI();

        if (!isAdded() || !hasDataAvailable()) {
            return;
        }

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_most_popular_item, (ViewGroup) mResultContainer.getRootView(), false);

        int dayOfTheWeek = mInsightsPopularModel.getHighestDayOfWeek();

        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTimeInMillis(System.currentTimeMillis());
        switch (dayOfTheWeek) {
            case 0:
                c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                break;
            case 1:
                c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                break;
            case 2:
                c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                break;
            case 3:
                c.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                break;
            case 4:
                c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                break;
            case 5:
                c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                break;
            case 6:
                c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                break;
        }

        DateFormat formatter = new SimpleDateFormat("EEEE");
        final TextView mostPopularDayTextView = (TextView) ll.findViewById(R.id.stats_most_popular_day);
        mostPopularDayTextView.setText(formatter.format(c.getTime()));
        final TextView mostPopularDayPercentTextView = (TextView) ll.findViewById(R.id.stats_most_popular_day_percent);
        mostPopularDayPercentTextView.setText(
                String.format(
                        getString(R.string.stats_insights_most_popular_percent_views),
                        roundToInteger(mInsightsPopularModel.getHighestDayPercent())
                )
        );

        TextView mostPopularHourTextView = (TextView) ll.findViewById(R.id.stats_most_popular_hour);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
        c.set(Calendar.HOUR_OF_DAY, mInsightsPopularModel.getHighestHour());
        c.set(Calendar.MINUTE, 0);
        mostPopularHourTextView.setText(timeFormat.format(c.getTime()));
        final TextView mostPopularHourPercentTextView = (TextView) ll.findViewById(R.id.stats_most_popular_hour_percent);
        mostPopularHourPercentTextView.setText(
                String.format(
                        getString(R.string.stats_insights_most_popular_percent_views),
                        roundToInteger(mInsightsPopularModel.getHighestHourPercent())
                )
        );

        mResultContainer.addView(ll);
    }

    /*
     * Round a double to the closest integer
     *
     * If the decimal part is less than 0.5, the integer part stays the same,
     * and truncation gives the right result.
     * If the decimal part is more that 0.5, the integer part increments,
     * and again truncation gives what we want.
     *
     */
    private int roundToInteger(double inputValue) {
        return (int) Math.floor(inputValue + 0.5);
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.INSIGHTS_POPULAR
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_insights_popular);
    }
}
