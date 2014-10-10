package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.SummaryModel;
import org.wordpress.android.ui.stats.model.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;


public class StatsVisitorsAndViewsFragment extends StatsAbstractFragment {

    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();

    private TextView mVisitorsToday;
    private TextView mVisitorsTodayLabel;
    private TextView mViewsToday;
    private TextView mViewsBestEver;
    private TextView mViewsAllTime;
    private TextView mCommentsAllTime;

    private TextView summaryTextTest;
    private TextView graphTextTest;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
        setRetainInstance(true);

        TextView titleTextView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleTextView.setText(getTitle().toUpperCase(Locale.getDefault()));

        mVisitorsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_visitors_count);
        mVisitorsTodayLabel = (TextView) view.findViewById(R.id.stats_visitors_and_views_header_label);
        mViewsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_views_count);
        mViewsBestEver = (TextView) view.findViewById(R.id.stats_visitors_and_views_best_ever_views_count);
        mViewsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_view_count);
        mCommentsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_comment_count);

        summaryTextTest = (TextView) view.findViewById(R.id.textViewTest2);
        graphTextTest = (TextView) view.findViewById(R.id.textViewTest);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_ONE_STAT_UPDATED));

        //refreshSummary();
    }


/*

    private void refreshSummary() {
        if (WordPress.getBlog(getLocalTableBlogID()) == null) {
            return;
        }

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                String blogId = WordPress.getBlog(getLocalTableBlogID()).getDotComBlogId();
                if (TextUtils.isEmpty(blogId)) {
                    blogId = "0";
                }
                final StatsSummary summary = StatsUtils.getSummary(blogId);
                handler.post(new Runnable() {
                    public void run() {
                        refreshSummary(summary);
                    }
                });
            }
        } .start();
    }

    private void refreshSummary(final StatsSummary stats) {
        if (getActivity() == null) {
            return;
        }

        String timezone = StatsUtils.getBlogTimezone(WordPress.getBlog(getLocalTableBlogID()));
        long currentDate = timezone != null ? StatsUtils.getCurrentDateMsTZ(timezone) : StatsUtils.getCurrentDateMs();

        if (stats != null
                && stats.getDay() != null
                && StatsUtils.toMs(stats.getDay()) != currentDate
                ) {
            mVisitorsTodayLabel.setText(StatsUtils.parseDate(stats.getDay(), "yyyy-MM-dd", "MMM d"));
        } else {
            // set the default "Today" label
            mVisitorsTodayLabel.setText(R.string.stats_visitors_and_views_header_today);
        }

        if (stats == null) {
            mVisitorsToday.setText("0");
            mViewsToday.setText("0");
            mViewsBestEver.setText("0");
            mViewsAllTime.setText("0");
            mCommentsAllTime.setText("0");
        } else {
            mVisitorsToday.setText(FormatUtils.formatDecimal(stats.getVisitorsToday()));
            mViewsToday.setText(FormatUtils.formatDecimal(stats.getViewsToday()));
            mViewsBestEver.setText(FormatUtils.formatDecimal(stats.getViewsBestDayTotal()));
            mViewsAllTime.setText(FormatUtils.formatDecimal(stats.getViewsAllTime()));
            mCommentsAllTime.setText(FormatUtils.formatDecimal(stats.getCommentsAllTime()));
        }
    }
*/
    @Override
    protected String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    /*
     * receives broadcast when data has been updated
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (!action.equals(StatsService.ACTION_ONE_STAT_UPDATED) || !intent.hasExtra(StatsService.EXTRA_UPDATED_SECTION)) {
                return;
            }

            StatsService.StatsSectionEnum sectionToUpdate = (StatsService.StatsSectionEnum) intent.getSerializableExtra(StatsService.EXTRA_UPDATED_SECTION);
            if (sectionToUpdate != StatsService.StatsSectionEnum.SUMMARY && sectionToUpdate != StatsService.StatsSectionEnum.VISITS) {
                return;
            }

            Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_UPDATED_DATA);
            if ( dataObj == null || dataObj instanceof VolleyError) {
                //TODO: show the error on the section ???
                return;
            }
            //TODO: check period and blogID
            final String blogId = StatsUtils.getBlogId(getLocalTableBlogID());

            if (sectionToUpdate == StatsService.StatsSectionEnum.SUMMARY) {
                summaryTextTest.setText(""+  ((SummaryModel)dataObj).getVisitors());
            }

            if (sectionToUpdate == StatsService.StatsSectionEnum.VISITS) {
                graphTextTest.setText("" + org.apache.commons.lang.StringUtils.abbreviate(((VisitsModel)dataObj).getData(), 100));
            }
            return;
        }
    };
}
