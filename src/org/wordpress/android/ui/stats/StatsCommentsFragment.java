package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.StatsRestHelper;
import org.wordpress.android.util.WPLinkMovementMethod;

import java.text.DecimalFormat;
import java.util.Locale;

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
    protected FragmentStatePagerAdapter getAdapter() {
        return new CustomPagerAdapter(getChildFragmentManager());
    }
    
    private class CustomPagerAdapter extends FragmentStatePagerAdapter {

        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }
        
    }


    @Override
    protected Fragment getFragment(int position) {
        int emptyLabelResId = R.string.stats_empty_comments;
        if (position == 0) {
            int entryLabelResId = R.string.stats_entry_top_commenter;
            int totalsLabelResId = R.string.stats_totals_comments;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_TOP_COMMENTERS_URI, entryLabelResId, totalsLabelResId, emptyLabelResId);
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, TOP_COMMENTERS));
            return fragment;
        } else if (position == 1) {
            int entryLabelResId = R.string.stats_entry_most_commented;
            int totalsLabelResId = R.string.stats_totals_comments;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_MOST_COMMENTED_URI, entryLabelResId, totalsLabelResId, emptyLabelResId);
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, MOST_COMMENTED));
            return fragment;
        } else {
            CommentsSummaryFragment fragment = new CommentsSummaryFragment();
            return fragment;
        }
    }
    
    public class CustomCursorAdapter extends CursorAdapter {

        private int mType;

        public CustomCursorAdapter(Context context, Cursor c, int type) {
            super(context, c, true);
            mType = type;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            // entry
            String entry;
            if (mType == TOP_COMMENTERS)
                entry = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.NAME));
            else 
                entry = cursor.getString(cursor.getColumnIndex(StatsMostCommentedTable.Columns.POST));

            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);


            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
            
            // totals
            int total;
            if (mType == TOP_COMMENTERS)
                total = cursor.getInt(cursor.getColumnIndex(StatsTopCommentersTable.Columns.COMMENTS));
            else 
                total = cursor.getInt(cursor.getColumnIndex(StatsMostCommentedTable.Columns.COMMENTS));
            
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(formatter.format(total));
            
            // image 
            String imageUrl;
            if (mType == TOP_COMMENTERS) {
                imageUrl = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.IMAGE_URL));
                
                view.findViewById(R.id.stats_list_cell_image_frame).setVisibility(View.VISIBLE);
                
                NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
                ImageView errorImageView = (ImageView) view.findViewById(R.id.stats_list_cell_blank_image);
                if (imageUrl != null && imageUrl.length() > 0) {
                    imageView.setErrorImageResId(R.drawable.stats_blank_image);
                    imageView.setDefaultImageResId(R.drawable.stats_blank_image);
                    imageView.setImageUrl(imageUrl, WordPress.imageLoader);
                    imageView.setVisibility(View.VISIBLE);
                    errorImageView.setVisibility(View.GONE);
                } else {
                    imageView.setVisibility(View.GONE);
                    errorImageView.setVisibility(View.VISIBLE);
                }
            }
            
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
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

    /** Fragment used for summary view **/
    public static class CommentsSummaryFragment extends SherlockFragment {
        
        private TextView mPerMonthText;
        private TextView mTotalText;
        private TextView mActiveDayText;
        private TextView mActiveTimeText;
        private TextView mMostCommentedText;
        
        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(StatUtils.STATS_SUMMARY_UPDATED)) {
                    StatsSummary stats = (StatsSummary) intent.getSerializableExtra(StatUtils.STATS_SUMMARY_UPDATED_EXTRA);
                    refreshStats(stats);
                }
            }
        };
        
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
        public void onPause() {
            super.onPause();

            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
            lbm.unregisterReceiver(mReceiver);
        }
        
        @Override
        public void onResume() {
            super.onResume();
            
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
            lbm.registerReceiver(mReceiver, new IntentFilter(StatUtils.STATS_SUMMARY_UPDATED));

            refreshStatsFromFile();
        }

        private void refreshStatsFromFile() {
            if (WordPress.getCurrentBlog() == null)
                return;
            
            final String blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());
            new AsyncTask<Void, Void, StatsSummary>() {

                @Override
                protected StatsSummary doInBackground(Void... params) {
                    StatsRestHelper.getStatsSummary(blogId);
                    return StatUtils.getSummary(blogId);
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
                };
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
    

            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());

            mPerMonthText.setText(formatter.format(perMonth));
            mTotalText.setText(formatter.format(total));
            mActiveDayText.setText(activeDay);
            mActiveTimeText.setText(activeTime);
            
            Spanned link = Html.fromHtml("<a href=\"" + activePostUrl + "\">" + activePost + "</a>");
            mMostCommentedText.setText(link);
            mMostCommentedText.setMovementMethod(WPLinkMovementMethod.getInstance());
        }

        
    }

}
