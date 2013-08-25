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
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
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
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.models.StatsReferrer;
import org.wordpress.android.models.StatsReferrerGroup;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.util.StatUtils;

public class StatsReferrersFragment extends StatsAbsPagedViewFragment  implements TabListener {
    
    private static final Uri STATS_REFERRER_GROUP_URI = StatsContentProvider.STATS_REFERRER_GROUP_URI;
    private static final Uri STATS_REFERRERS_URI = StatsContentProvider.STATS_REFERRERS_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };
    
    public static final String TAG = StatsReferrersFragment.class.getSimpleName();
    
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
            return TIMEFRAMES.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TIMEFRAMES[position].getLabel();
        }

    }

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_referrers;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_referrers;
        
        Uri groupUri = Uri.parse(STATS_REFERRER_GROUP_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());
        Uri childrenUri = STATS_REFERRERS_URI;
        
        StatsCursorTreeFragment fragment = StatsCursorTreeFragment.newInstance(groupUri, childrenUri, entryLabelResId, totalsLabelResId, emptyLabelResId);
        CustomAdapter adapter = new CustomAdapter(null, getActivity());
        adapter.setCursorLoaderCallback(fragment);
        fragment.setListAdapter(adapter);
        return fragment;
    }
    
    public class CustomAdapter extends CursorTreeAdapter {

        private StatsCursorLoaderCallback mCallback;

        public CustomAdapter(Cursor cursor, Context context) {
            super(cursor, context, true);
        }

        public void setCursorLoaderCallback(StatsCursorLoaderCallback callback) {
            mCallback = callback;
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            String name = cursor.getString(cursor.getColumnIndex(StatsReferrersTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrersTable.Columns.TOTAL));

            // name, url
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            if (name.startsWith("http")) {
                Spanned link = Html.fromHtml("<a href=\"" + name + "\">" + name + "</a>");
                entryTextView.setText(link);
                entryTextView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                entryTextView.setText(name);
            }
            
            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");

            // no icon
        }

        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
            String name = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.TOTAL));
            String url = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.URL));
            String icon = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.ICON));
            int children = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.CHILDREN));

            boolean urlValid = (url != null && url.length() > 0); 
            
            // chevron
            toggleChevrons(children > 0, isExpanded, view);
            
            // name, url
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_group_cell_entry);
            if (urlValid) {
                Spanned link = Html.fromHtml("<a href=\"" + url + "\">" + name + "</a>");
                entryTextView.setText(link);
                entryTextView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                entryTextView.setText(name);
            }
            
            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_group_cell_total);
            totalsTextView.setText(total + "");

            // icon
            view.findViewById(R.id.stats_group_cell_image_frame).setVisibility(View.VISIBLE);
            NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.stats_group_cell_image);
            ImageView errorImageView = (ImageView) view.findViewById(R.id.stats_group_cell_blank_image);
            if (icon != null && icon.length() > 0) {
                imageView.setErrorImageResId(R.drawable.stats_blank_image);
                imageView.setDefaultImageResId(R.drawable.stats_blank_image);
                imageView.setImageUrl(icon, WordPress.imageLoader);
                imageView.setVisibility(View.VISIBLE);
                errorImageView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.GONE);
                errorImageView.setVisibility(View.VISIBLE);
            }   
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            Bundle bundle = new Bundle();
            bundle.putLong(StatsCursorLoaderCallback.BUNDLE_DATE, groupCursor.getLong(groupCursor.getColumnIndex("date")));
            bundle.putString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID, groupCursor.getString(groupCursor.getColumnIndex("groupId")));
            mCallback.onUriRequested(groupCursor.getPosition(), STATS_REFERRERS_URI, bundle);
            return null;
        }
        
        @Override
        protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, parent, false);
        }

        @Override
        protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_group_cell, parent, false);
        }

        private void toggleChevrons(boolean isVisible, boolean isExpanded, View view) {
            ImageView chevronUp = (ImageView) view.findViewById(R.id.stats_group_cell_chevron_up);
            ImageView chevronDown = (ImageView) view.findViewById(R.id.stats_group_cell_chevron_down);
            View frame = view.findViewById(R.id.stats_group_cell_chevron_frame);
            
            if (isVisible) {
                frame.setVisibility(View.VISIBLE);  
                if (isExpanded) {
                    chevronUp.setVisibility(View.VISIBLE);
                    chevronDown.setVisibility(View.GONE);
                } else {
                    chevronUp.setVisibility(View.GONE);
                    chevronDown.setVisibility(View.VISIBLE);
                }
            } else {
                frame.setVisibility(View.GONE);
            }
        }
        
    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_referrers);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }
    
    @Override
    public void refresh(final int position) {
        final String blogId = getCurrentBlogId();
        if (getCurrentBlogId() == null)
            return;
        
        String date = StatUtils.getCurrentDate();
        if (position == 1)
            date = StatUtils.getYesterdaysDate();
                    
        WordPress.restClient.getStatsReferrers(blogId, date,
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        new ParseJsonTask().execute(blogId, response);
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("WordPress Stats", StatsReferrersFragment.class.getSimpleName() + ": " + error.toString());
                    }
                });
    }
    
    private static class ParseJsonTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            String blogId = (String) params[0];
            JSONObject response = (JSONObject) params[1];
            
            Context context = WordPress.getContext();
            
            if (response != null) {
                try {
                    String date = response.getString("date");
                    long dateMs = StatUtils.toMs(date);
                    long twoDays = 2 * 24 * 60 * 60 * 1000;
                    
                    // delete data with the same date, and data older than two days ago (keep yesterday's data)
                    context.getContentResolver().delete(STATS_REFERRER_GROUP_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - twoDays) + "" });
                    context.getContentResolver().delete(STATS_REFERRERS_URI, "blogId=? AND (date=? OR date<=?)", new String[] { blogId, dateMs + "", (dateMs - twoDays) + "" });
                    
                    JSONArray groups = response.getJSONArray("referrers");
                    int groupsCount = groups.length();
                    
                    // insert groups
                    for (int i = 0; i < groupsCount; i++ ) {
                        JSONObject group = groups.getJSONObject(i);
                        StatsReferrerGroup statGroup = new StatsReferrerGroup(blogId, date, group);
                        ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                        context.getContentResolver().insert(STATS_REFERRER_GROUP_URI, values);

                        
                        // insert children, only if there is more than one entry
                        JSONArray referrers = group.getJSONArray("results");
                        int count = referrers.length();
                        if (count > 1) {
                            
                            for (int j = 0; j < count; j++) {
                                StatsReferrer stat = new StatsReferrer(blogId, date, statGroup.getGroupId(), referrers.getJSONArray(j));
                                ContentValues v = StatsReferrersTable.getContentValues(stat);
                                context.getContentResolver().insert(STATS_REFERRERS_URI, v);
                            }
                        }
                        
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            return null;
        }        
    }

}
