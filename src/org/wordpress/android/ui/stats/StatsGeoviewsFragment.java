package org.wordpress.android.ui.stats;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsClicksTable;
import org.wordpress.android.datasets.StatsGeoviewsTable;
import org.wordpress.android.models.StatsGeoview;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.ui.stats.StatsCursorFragment.StatsCursorFragmentCallback;

public class StatsGeoviewsFragment extends StatsAbsListViewFragment implements TabListener, StatsCursorFragmentCallback {
    
    private static final Uri STATS_GEOVIEWS_URI = StatsContentProvider.STATS_GEOVIEWS_URI;

    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };
    
    public static final String TAG = StatsGeoviewsFragment.class.getSimpleName();
    
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
            return TIMEFRAMES.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TIMEFRAMES[position].getLabel(); 
        }

    }

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_country;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_geoviews;
        StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_GEOVIEWS_URI, TIMEFRAMES[position], entryLabelResId, totalsLabelResId, emptyLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        return fragment;
    }
    
    public static class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsGeoviewsTable.Columns.COUNTRY));
            int total = cursor.getInt(cursor.getColumnIndex(StatsGeoviewsTable.Columns.VIEWS));
            String imageUrl = cursor.getString(cursor.getColumnIndex(StatsClicksTable.Columns.IMAGE_URL));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);
            
            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");
            
            // image
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

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
        }

    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_views_by_country);
    }

    @Override
    public String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    public void refresh(final int position) {
        final String blogId = getCurrentBlogId();
        if (getCurrentBlogId() == null)
            return;
                    
        WordPress.restClient.getStatsGeoviews(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseJsonTask().execute(blogId, response, position);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("WordPress Stats", StatsGeoviewsFragment.class.getSimpleName() + ": " + error.toString());
                    }
                });
    }
    
    private static class ParseJsonTask extends AsyncTask<Object, Void, Void> {

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
                        StatsGeoview stat = new StatsGeoview(blogId, result);
                        ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                        context.getContentResolver().insert(STATS_GEOVIEWS_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            return null;
        }        
    }

    @Override
    public CursorLoader getCursorLoader(Uri uri, StatsTimeframe timeframe) {
        // TODO Auto-generated method stub
        return null;
    }
}
