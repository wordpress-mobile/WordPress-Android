package org.wordpress.android.ui.stats;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.models.StatsMostCommented;
import org.wordpress.android.models.StatsTopCommenter;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsCommentsFragment extends StatsAbsListViewFragment implements TabListener {

    private static final Uri STATS_MOST_COMMENTED_URI = StatsContentProvider.STATS_MOST_COMMENTED_URI;
    private static final Uri STATS_TOP_COMMENTERS_URI = StatsContentProvider.STATS_TOP_COMMENTERS_URI;

    private static final String[] TITLES = new String[] { "Top Recent Commenters", "Most Commented", "Summary" };
    
    private static final int TOP_COMMENTERS = 0;
    private static final int MOST_COMMENTED = 1;
    
    @Override
    public FragmentStatePagerAdapter getAdapter() {
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
        if (position == 0) {
            int entryLabelResId = R.string.stats_entry_top_commenter;
            int totalsLabelResId = R.string.stats_totals_comments;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_TOP_COMMENTERS_URI, entryLabelResId, totalsLabelResId);
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, TOP_COMMENTERS));
            return fragment;
        } else if (position == 1) {
            int entryLabelResId = R.string.stats_entry_most_commented;
            int totalsLabelResId = R.string.stats_totals_comments;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_MOST_COMMENTED_URI, entryLabelResId, totalsLabelResId);
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

            
            // totals
            int total;
            if (mType == TOP_COMMENTERS)
                total = cursor.getInt(cursor.getColumnIndex(StatsTopCommentersTable.Columns.COMMENTS));
            else 
                total = cursor.getInt(cursor.getColumnIndex(StatsMostCommentedTable.Columns.COMMENTS));
            
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");
            
            // image 
            String imageUrl;
            if (mType == TOP_COMMENTERS) {
                imageUrl = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.IMAGE_URL));
                
                NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageUrl(imageUrl, WordPress.imageLoader);
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

    public static class CommentsSummaryFragment extends SherlockFragment {
        
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
        
    }

    @Override
    public String[] getTabTitles() {
        return TITLES;
    }

    @Override
    public void refresh(final int position) {
        final String blogId = getCurrentBlogId();
        if (getCurrentBlogId() == null)
            return;

        if (position == MOST_COMMENTED) {
            WordPress.restClient.getStatsMostCommented(blogId, 
                    new Listener() {
                        
                        @Override
                        public void onResponse(JSONObject response) {
                            new ParseJsonTask().execute(blogId, response, position);
                        }
                    }, 
                    new ErrorListener() {
                        
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("WordPress Stats", StatsCommentsFragment.class.getSimpleName() + ": " + error.toString());
                        }
                    });
        } else if (position == TOP_COMMENTERS) {
            WordPress.restClient.getStatsTopCommenters(blogId, 
                    new Listener() {
                        
                        @Override
                        public void onResponse(JSONObject response) {
                            new ParseJsonTask().execute(blogId, response, position);
                        }
                    }, 
                    new ErrorListener() {
                        
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("WordPress Stats", StatsCommentsFragment.class.getSimpleName() + ": " + error.toString());
                        }
                    });
        }
    }
    
    private static class ParseJsonTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            int position = (Integer) params[2];
            
            if (response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    
                    if (position == TOP_COMMENTERS) {
                        parseTopCommenters(blogId, results);
                    } else if (position == MOST_COMMENTED) {
                        parseMostCommented(blogId, results);            
                    }
                    
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            return null;
        }

        private void parseTopCommenters(String blogId, JSONArray results) throws JSONException {

            Context context = WordPress.getContext();
            
            for (int i = 0; i < results.length(); i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsTopCommenter stat = new StatsTopCommenter(blogId, result);
                ContentValues values = StatsTopCommentersTable.getContentValues(stat);
                context.getContentResolver().insert(STATS_TOP_COMMENTERS_URI, values);
            }
        }
        
        private void parseMostCommented(String blogId, JSONArray results) throws JSONException {

            Context context = WordPress.getContext();
            
            for (int i = 0; i < results.length(); i++ ) {
                JSONObject result = results.getJSONObject(i);
                StatsMostCommented stat = new StatsMostCommented(blogId, result);
                ContentValues values = StatsMostCommentedTable.getContentValues(stat);
                context.getContentResolver().insert(STATS_MOST_COMMENTED_URI, values);
            }
        }
    }
    
    
}
