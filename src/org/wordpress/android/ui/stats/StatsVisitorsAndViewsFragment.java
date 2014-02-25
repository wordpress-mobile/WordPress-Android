package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.Utils;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Fragment for visitors and views stats. Has three pages, for DAY, WEEK and MONTH stats.
 * A summary of the blog's stats are also shown on each page.
 */ 
public class StatsVisitorsAndViewsFragment extends StatsAbsViewFragment implements RadioGroup.OnCheckedChangeListener {
    private static final String[] TITLES = new String [] { StatsBarChartUnit.DAY.getLabel(),
                                                           StatsBarChartUnit.WEEK.getLabel(),
                                                           StatsBarChartUnit.MONTH.getLabel() };

    private TextView mVisitorsToday;
    private TextView mViewsToday;
    private TextView mViewsBestEver;
    private TextView mViewsAllTime;
    private TextView mCommentsAllTime;

    private static final String CHILD_TAG = "CHILD_TAG";

    private int mSelectedButtonIndex = 0;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(StatUtils.STATS_SUMMARY_UPDATED)) {
                StatsSummary summary = (StatsSummary) intent.getSerializableExtra(StatUtils.STATS_SUMMARY_UPDATED_EXTRA);
                refreshViews(summary);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);

        TextView titleTextView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleTextView.setText(getTitle());

        mVisitorsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_visitors_count);
        mViewsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_views_count);
        mViewsBestEver = (TextView) view.findViewById(R.id.stats_visitors_and_views_best_ever_views_count);
        mViewsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_view_count);
        mCommentsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_comment_count);

        RadioGroup mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);
        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);

        for (int i = 0; i < TITLES.length; i++) {
            RadioButton rb = (RadioButton) LayoutInflater.from(getActivity()).inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            int dp8 = (int) Utils.dpToPx(8);
            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth((int) Utils.dpToPx(80));
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(TITLES[i]);
            mRadioGroup.addView(rb);

            if (i == mSelectedButtonIndex)
                rb.setChecked(true);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatUtils.STATS_SUMMARY_UPDATED));

        refreshSummary();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mSelectedButtonIndex = group.indexOfChild(group.findViewById(checkedId));
        loadBarChartFragmentForIndex(mSelectedButtonIndex);
    }

    private void loadBarChartFragmentForIndex(int index) {
        if (getChildFragmentManager().findFragmentByTag(CHILD_TAG + ":" + index) == null) {
            StatsBarChartUnit unit = StatsBarChartUnit.DAY;
            if (index == 1) {
                unit = StatsBarChartUnit.WEEK;
            } else if (index == 2) {
                unit = StatsBarChartUnit.MONTH;
            }
            StatsBarGraphFragment statsBarGraphFragment = StatsBarGraphFragment.newInstance(unit);
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.stats_fade_in, R.anim.stats_fade_out);
            ft.replace(R.id.stats_bar_chart_fragment_container, statsBarGraphFragment, CHILD_TAG + ":" + index);
            ft.commit();
        }
    }

    private void refreshSummary() {
        if (WordPress.getCurrentBlog() == null)
            return;

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                String blogId = WordPress.getCurrentBlog().getDotComBlogId();
                if (TextUtils.isEmpty(blogId))
                    blogId = "0";
                final StatsSummary summary = StatUtils.getSummary(blogId);
                handler.post(new Runnable() {
                    public void run() {
                        if (getActivity() != null)
                            refreshViews(summary);
                    }
                });
            }
        }.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    private void refreshViews(final StatsSummary stats) {
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                int visitorsToday = (stats != null ? stats.getVisitorsToday() : 0);
                int viewsToday = (stats != null ? stats.getViewsToday() : 0);
                int visitorsBestEver = (stats != null ? stats.getViewsBestDayTotal() : 0);
                int viewsAllTime = (stats != null ? stats.getViewsAllTime() : 0);
                int commentsAllTime = (stats != null ? stats.getCommentsAllTime() : 0);

                DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
                final String fmtVisitorsToday = formatter.format(visitorsToday);
                final String fmtViewsToday = formatter.format(viewsToday);
                final String fmtVisitorsBestEver = formatter.format(visitorsBestEver);
                final String fmtViewsAllTime = formatter.format(viewsAllTime);
                final String fmtCommentsAllTime = formatter.format(commentsAllTime);

                handler.post(new Runnable() {
                    public void run() {
                        if (getActivity() == null)
                            return;
                        mVisitorsToday.setText(fmtVisitorsToday);
                        mViewsToday.setText(fmtViewsToday);
                        mViewsBestEver.setText(fmtVisitorsBestEver);
                        mViewsAllTime.setText(fmtViewsAllTime);
                        mCommentsAllTime.setText(fmtCommentsAllTime);
                    }
                });
            }
        }.start();
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }
}