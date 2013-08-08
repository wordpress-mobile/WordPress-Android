package org.wordpress.android.ui.stats;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsTopPostsAndPagesTable;
import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsTopPostsAndPagesFragment extends StatsAbsListViewFragment  implements TabListener {
    
    private static final Uri STATS_TOP_POSTS_AND_PAGES_URI = StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI;
    private static final String[] TITLES = new String[] { StatsTimeframe.TODAY.getLabel(), StatsTimeframe.YESTERDAY.getLabel() };

    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

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
        int entryLabelResId = R.string.stats_entry_posts_and_pages;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_top_posts;
        StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_TOP_POSTS_AND_PAGES_URI, entryLabelResId, totalsLabelResId, emptyLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        return fragment;
    }
    
    public class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsTopPostsAndPagesTable.Columns.TITLE));
            String url = cursor.getString(cursor.getColumnIndex(StatsTopPostsAndPagesTable.Columns.URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsTopPostsAndPagesTable.Columns.VIEWS));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            if (url != null && url.length() > 0) {
                Spanned link = Html.fromHtml("<a href=\"" + url + "\">" + entry + "</a>");
                entryTextView.setText(link);
                entryTextView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                entryTextView.setText(entry);
            }
            
            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");
            
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
        }

    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
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
        
        WordPress.restClient.getStatsTopPosts(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseJsonTask().execute(blogId, response, position);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("WordPress Stats", StatsTopPostsAndPagesFragment.class.getSimpleName() + ": " + error.toString());
                    }
                });
    }
    
    private class ParseJsonTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            // int position = (Integer) params[2];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");

                    int count = results.length();
                    for (int i = 0; i < count; i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTopPostsAndPages stat = new StatsTopPostsAndPages(blogId, result);
                        ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                        context.getApplicationContext().getContentResolver().insert(STATS_TOP_POSTS_AND_PAGES_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            return null;
        }        
    }

}
