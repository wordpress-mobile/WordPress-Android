package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPLinkMovementMethod;

import java.io.Serializable;
import java.util.Locale;

/**
 * Fragment for summary stats. Only a single page.
 */
public class StatsTotalsFollowersAndSharesFragment extends StatsAbsViewFragment {
    public static final String TAG = StatsTotalsFollowersAndSharesFragment.class.getSimpleName();

    private TextView mPostsCountView;
    private TextView mCategoriesCountView;
    private TextView mTagsCountView;
    private TextView mFollowersCountView;
    private TextView mCommentsCountView;
    private TextView mSharesCountView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_totals_followers_shares, container, false);

        final TextView titleView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleView.setText(getTitle().toUpperCase(Locale.getDefault()));

        mPostsCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_posts_count);
        mCategoriesCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_categories_count);
        mTagsCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_tags_count);
        mFollowersCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_followers_count);
        mCommentsCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_comments_count);
        mSharesCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_shares_count);

        TextView followersHeader = (TextView) view.findViewById(R.id.stats_totals_followers_shares_header_followers);
        if (followersHeader != null) {
            String headerText = getString(R.string.stats_totals_followers_shares_header_followers) +
                    " <font color=\"#9E9E9E\">(" +
                    String.format(getString(R.string.stats_totals_followers_shares_header_includes_publicize),
                    "</font><font color=\"#21759B\"><a href=\"http://support.wordpress.com/publicize/\">") +
                    "</font><font color=\"#9E9E9E\">)</font>";
            followersHeader.setText(Html.fromHtml(headerText));
            followersHeader.setMovementMethod(WPLinkMovementMethod.getInstance());
        }

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
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_SUMMARY_UPDATED));

        refreshSummary();
    }

    private void refreshSummary() {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                if (WordPress.getCurrentBlog() == null)
                    return;

                String blogId = WordPress.getCurrentBlog().getDotComBlogId();
                if (TextUtils.isEmpty(blogId))
                    blogId = "0";

                final StatsSummary stats = StatsUtils.getSummary(blogId);

                handler.post(new Runnable() {
                    public void run() {
                        refreshSummary(stats);
                    }
                });
            }
        }.start();
    }

    private void refreshSummary(final StatsSummary stats) {
        if (getActivity() == null)
            return;

        if (stats == null){
            mPostsCountView.setText("0");
            mCategoriesCountView.setText("0");
            mTagsCountView.setText("0");
            mFollowersCountView.setText("0");
            mCommentsCountView.setText("0");
            mSharesCountView.setText("0");
        } else {
            mPostsCountView.setText(FormatUtils.formatDecimal(stats.getPosts()));
            mCategoriesCountView.setText(FormatUtils.formatDecimal(stats.getCategories()));
            mTagsCountView.setText(FormatUtils.formatDecimal(stats.getTags()));
            mFollowersCountView.setText(FormatUtils.formatDecimal(stats.getFollowersBlog()));
            mCommentsCountView.setText(FormatUtils.formatDecimal(stats.getFollowersComments()));
            mSharesCountView.setText(FormatUtils.formatDecimal(stats.getShares()));
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_totals_followers_and_shares);
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
