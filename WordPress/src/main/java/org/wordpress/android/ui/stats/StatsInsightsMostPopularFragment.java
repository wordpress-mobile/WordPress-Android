package org.wordpress.android.ui.stats;

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


    void customizeUIWithResults() {
        mResultContainer.removeAllViews();

        // Another check that the data is available
        if (isDataEmpty(0) || !(mDatamodels[0] instanceof InsightsPopularModel)) {
            showErrorUI(null);
            return;
        }

        InsightsPopularModel data = (InsightsPopularModel) mDatamodels[0];

        LinearLayout ll = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.stats_insights_most_popular_item, (ViewGroup) mResultContainer.getRootView(), false);

        int dayOfTheWeek = data.getHighestDayOfWeek();

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
                        getString(R.string.stats_insights_most_popular_percent_weekly_views),
                        data.getHighestDayPercent().intValue()
                )
        );

        TextView mostPopularHourTextView = (TextView) ll.findViewById(R.id.stats_most_popular_hour);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
        c.set(Calendar.HOUR_OF_DAY, data.getHighestHour());
        c.set(Calendar.MINUTE, 0);
        mostPopularHourTextView.setText(timeFormat.format(c.getTime()));
        final TextView mostPopularHourPercentTextView = (TextView) ll.findViewById(R.id.stats_most_popular_hour_percent);
        mostPopularHourPercentTextView.setText(
                String.format(
                        getString(R.string.stats_insights_most_popular_percent_daily_views),
                        data.getHighestHourPercent().intValue()
                )
        );

        mResultContainer.addView(ll);
    }

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
