package org.wordpress.android.ui.stats;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;

/**
 * Fragment for visitors and views stats. Has three pages, for DAY, WEEK and MONTH stats.
 * A summary of the blog's stats are also shown on each page.
 */
public class StatsVisitorsAndViewsFragment extends StatsAbsViewFragment implements RadioGroup.OnCheckedChangeListener {
    private static final String[] TITLES = new String [] {StatsBarChartUnit.DAY.getLabel(),
                                                           StatsBarChartUnit.WEEK.getLabel(),
                                                           StatsBarChartUnit.MONTH.getLabel()};

    private TextView mVisitorsToday;
    private TextView mVisitorsTodayLabel;
    private TextView mViewsToday;
    private TextView mViewsBestEver;
    private TextView mViewsAllTime;
    private TextView mCommentsAllTime;
    private RadioGroup mRadioGroup;

    private static final String CHILD_TAG = "CHILD_TAG";

    private int mSelectedButtonIndex = 0;

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

        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);
        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);

        for (int i = 0; i < TITLES.length; i++) {
            RadioButton rb = (RadioButton) LayoutInflater.from(getActivity()).inflate(
                    R.layout.stats_radio_button,
                    null,
                    false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            int dp8 = DisplayUtils.dpToPx(view.getContext(), 8);
            params.setMargins(0, 0, dp8, 0);
            rb.setMinimumWidth(DisplayUtils.dpToPx(view.getContext(), 80));
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(TITLES[i]);
            mRadioGroup.addView(rb);

            if (i == mSelectedButtonIndex) {
                rb.setChecked(true);
            }
        }
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);    }

    @Override
    public void onResume() {
        super.onResume();

        mRadioGroup.setOnCheckedChangeListener(this);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_SUMMARY_UPDATED));

        refreshSummary();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mSelectedButtonIndex = group.indexOfChild(group.findViewById(checkedId));
        loadBarChartFragmentForIndex(mSelectedButtonIndex);
    }

    private void loadBarChartFragmentForIndex(int index) {
        String childTag = CHILD_TAG + ":" + this.getClass().getSimpleName() + ":" + index;
        final StatsBarChartUnit unit;
        switch (index) {
            case 1:
                unit = StatsBarChartUnit.WEEK;
                break;
            case 2:
                unit = StatsBarChartUnit.MONTH;
                break;
            default:
                unit = StatsBarChartUnit.DAY;
        }

        StatsBarGraphFragment statsBarGraphFragment = StatsBarGraphFragment.newInstance(unit, getLocalTableBlogID());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.stats_fade_in, R.anim.stats_fade_out);
        ft.replace(R.id.stats_bar_chart_fragment_container, statsBarGraphFragment, childTag);
        ft.commit();
    }

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

    @Override
    protected String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    /*
     * receives broadcast when summary data has been updated
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());
            if (action.equals(StatsService.ACTION_STATS_SUMMARY_UPDATED)) {
                Serializable serial = intent.getSerializableExtra(StatsService.STATS_SUMMARY_UPDATED_EXTRA);
                if (serial instanceof StatsSummary) {
                    refreshSummary((StatsSummary) serial);
                }
            }
        }
    };
}
