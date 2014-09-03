package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

/**
 * Fragment for comments stats. Has three pages, for Most Commented, for Top Commenters, and for Comments Summary
 */
public class StatsCommentsFragment extends StatsAbsPagedViewFragment {
    private static final Uri STATS_MOST_COMMENTED_URI = StatsContentProvider.STATS_MOST_COMMENTED_URI;
    private static final Uri STATS_TOP_COMMENTERS_URI = StatsContentProvider.STATS_TOP_COMMENTERS_URI;

    public static final String TAG = StatsCommentsFragment.class.getSimpleName();

    private static final String[] TITLES = new String[] { "Top Recent Commenters", "Most Commented", "Summary" };

    private static final int TOP_COMMENTERS = 0;
    private static final int MOST_COMMENTED = 1;

    @Override
    protected Fragment getFragment(int position) {
        if (position == 0) {
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_TOP_COMMENTERS_URI,
                    R.string.stats_entry_top_commenter, R.string.stats_totals_comments, R.string.stats_empty_comments, getLocalTableBlogID());
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, TOP_COMMENTERS));
            fragment.setCallback(this);
            return fragment;
        } else if (position == 1) {
            int entryLabelResId = R.string.stats_entry_most_commented;
            int totalsLabelResId = R.string.stats_totals_comments;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_MOST_COMMENTED_URI,
                    R.string.stats_entry_most_commented, R.string.stats_totals_comments, R.string.stats_empty_comments, getLocalTableBlogID());
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, MOST_COMMENTED));
            fragment.setCallback(this);
            return fragment;
        } else {
            return new CommentsSummaryFragment();
        }
    }

    public class CustomCursorAdapter extends CursorAdapter {
        private final LayoutInflater inflater;
        private final int mType;

        public CustomCursorAdapter(Context context, Cursor c, int type) {
            super(context, c, true);
            mType = type;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            View view = inflater.inflate(R.layout.stats_list_cell, root, false);
            view.setTag(new StatsViewHolder(view));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            final String entry;
            final int total;
            if (mType == TOP_COMMENTERS) {
                entry = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.NAME));
                total = cursor.getInt(cursor.getColumnIndex(StatsTopCommentersTable.Columns.COMMENTS));
            } else {
                entry = cursor.getString(cursor.getColumnIndex(StatsMostCommentedTable.Columns.POST));
                total = cursor.getInt(cursor.getColumnIndex(StatsMostCommentedTable.Columns.COMMENTS));
            }

            holder.entryTextView.setText(entry);
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // image
            if (mType == TOP_COMMENTERS) {
                String imageUrl = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.IMAGE_URL));
                holder.networkImageView.setVisibility(View.VISIBLE);
                holder.showNetworkImage(imageUrl);
            } else {
                holder.networkImageView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_comments);
    }

    @Override
    protected String[] getTabTitles() {
        return TITLES;
    }

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_comments;
    }

    /** Fragment used for summary view **/
    public static class CommentsSummaryFragment extends Fragment {
        private TextView mPerMonthText;
        private TextView mTotalText;
        private TextView mActiveDayText;
        private TextView mActiveTimeText;
        private TextView mMostCommentedText;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.stats_comments_summary, container, false);

            mPerMonthText = (TextView) view.findViewById(R.id.stats_comments_summary_per_month_count);
            mTotalText = (TextView) view.findViewById(R.id.stats_comments_summary_total_count);
            mActiveDayText = (TextView) view.findViewById(R.id.stats_comments_summary_most_active_day_text);
            mActiveTimeText = (TextView) view.findViewById(R.id.stats_comments_summary_most_active_time_text);
            mMostCommentedText = (TextView) view.findViewById(R.id.stats_comments_summary_most_commented_text);

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshStatsFromFile();
        }

        private void refreshStatsFromFile() {
            if (WordPress.getCurrentBlog() == null)
                return;

            final String blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());
            new AsyncTask<Void, Void, StatsSummary>() {
                @Override
                protected StatsSummary doInBackground(Void... params) {
                    //StatsRestHelper.getStatsSummary(blogId);
                    return StatsUtils.getSummary(blogId);
                }

                protected void onPostExecute(final StatsSummary result) {
                    if (getActivity() == null)
                        return;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshStats(result);
                        }
                    });
                }
            }.execute();
        }

        private void refreshStats(StatsSummary stats) {
            int perMonth = 0;
            int total = 0;
            String activeDay = "";
            String activeTime = "";
            String activePost = "";
            String activePostUrl = "";

            if (stats != null) {
                perMonth = stats.getCommentsPerMonth();
                total = stats.getCommentsAllTime();
                activeDay = stats.getCommentsMostActiveRecentDay();
                activeTime = stats.getCommentsMostActiveTime();
//                activePost = result.getRecentMostActivePost(); // TODO
//                activePostUrl = result.getRecentMostActivePostUrl(); // TODO
            }


            mPerMonthText.setText(FormatUtils.formatDecimal(perMonth));
            mTotalText.setText(FormatUtils.formatDecimal(total));
            mActiveDayText.setText(activeDay);
            mActiveTimeText.setText(activeTime);

           // StatUtils.setEntryTextOrLink(mMostCommentedText, activePostUrl, activePost);
        }


    }
}
