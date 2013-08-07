
package org.wordpress.android.ui.stats;

import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.CursorAdapter;
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
import org.wordpress.android.datasets.StatsTagsAndCategoriesTable;
import org.wordpress.android.models.StatsTagsandCategories;
import org.wordpress.android.models.StatsTagsandCategories.Type;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.Utils;

public class StatsTagsAndCategoriesFragment extends StatsAbsViewFragment {

    private static final Uri STATS_TAGS_AND_CATEGORIES_URI = StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI;

    public static final String TAG = StatsTagsAndCategoriesFragment.class.getSimpleName();
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);

        if (Utils.isTablet()) {
            TextView tv = (TextView) view.findViewById(R.id.stats_pager_title);
            tv.setText(getTitle().toUpperCase(Locale.getDefault()));
        }
        
        FragmentManager fm = getChildFragmentManager();
        
        int entryLabelResId = R.string.stats_entry_tags_and_categories;
        int totalsLabelResId = R.string.stats_totals_views;

        StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_TAGS_AND_CATEGORIES_URI, entryLabelResId, totalsLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.stats_pager_container, fragment, StatsCursorFragment.TAG);
        ft.commit();
        
        return view;
    }

    public class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsTagsAndCategoriesTable.Columns.TOPIC));
            int total = cursor.getInt(cursor.getColumnIndex(StatsTagsAndCategoriesTable.Columns.VIEWS));
            String type = cursor.getString(cursor.getColumnIndex(StatsTagsAndCategoriesTable.Columns.TYPE));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);

            // tag and category icons
            if (type.equals(Type.CATEGORY.getLabel())) {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stats_icon_categories, 0, 0, 0);
            } else if (type.equals(Type.TAG.getLabel())) {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stats_icon_tags, 0, 0, 0);
            } else {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
        return getString(R.string.stats_view_tags_and_categories);
    }

    @Override
    public void refresh() {
        final String blogId = getCurrentBlogId();
        if (getCurrentBlogId() == null)
            return;
                    
        WordPress.restClient.getStatsTagsAndCategories(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseJsonTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("WordPress Stats", StatsTagsAndCategoriesFragment.class.getSimpleName() + ": " + error.toString());
                    }
                });
    }
    
    private static class ParseJsonTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null && response.has("result")) {
                try {
                    JSONArray results = response.getJSONArray("result");
                    for (int i = 0; i < results.length(); i++ ) {
                        JSONObject result = results.getJSONObject(i);
                        StatsTagsandCategories stat = new StatsTagsandCategories(blogId, result);
                        ContentValues values = StatsTagsAndCategoriesTable.getContentValues(stat);
                        context.getContentResolver().insert(STATS_TAGS_AND_CATEGORIES_URI, values);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            return null;
        }        
    }
}
