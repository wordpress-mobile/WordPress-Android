package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.WPLinkMovementMethod;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Fragment for referrer stats. Has two pages, for Today's and Yesterday's stats.
 * Referrers contain expandable lists.
 */
public class StatsReferrersFragment extends StatsAbsPagedViewFragment {
    
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

    private static class GroupViewHolder {
        TextView entryTextView;
        TextView totalsTextView;
        View imageFrame;
        NetworkImageView networkImageView;
        ImageView errorImageView;
    }

    private class ChildViewHolder {
        TextView entryTextView;
        TextView totalsTextView;
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
        protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.stats_list_cell, parent, false);

            ChildViewHolder holder = new ChildViewHolder();
            holder.entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            holder.totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            view.setTag(holder);

            return view;
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            final ChildViewHolder holder = (ChildViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrersTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrersTable.Columns.TOTAL));

            // name, url
            if (name.startsWith("http")) {
                Spanned link = Html.fromHtml("<a href=\"" + name + "\">" + name + "</a>");
                holder.entryTextView.setText(link);
                holder.entryTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
            } else {
                holder.entryTextView.setText(name);
                holder.entryTextView.setMovementMethod(null);
            }

            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
            
            // totals
            holder.totalsTextView.setText(formatter.format(total));

            // no icon
        }

        @Override
        protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.stats_group_cell, parent, false);

            GroupViewHolder holder = new GroupViewHolder();
            holder.entryTextView = (TextView) view.findViewById(R.id.stats_group_cell_entry);
            holder.errorImageView = (ImageView) view.findViewById(R.id.stats_group_cell_blank_image);
            holder.imageFrame = view.findViewById(R.id.stats_group_cell_image_frame);
            holder.networkImageView = (NetworkImageView) view.findViewById(R.id.stats_group_cell_image);
            holder.totalsTextView = (TextView) view.findViewById(R.id.stats_group_cell_total);
            view.setTag(holder);

            return view;
        }

        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
            GroupViewHolder holder = (GroupViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.TOTAL));
            String url = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.URL));
            String icon = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.ICON));
            int children = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.CHILDREN));

            boolean urlValid = (url != null && url.length() > 0); 
            
            // chevron
            toggleChevrons(children > 0, isExpanded, view);
            
            // name, url
            if (urlValid) {
                Spanned link = Html.fromHtml("<a href=\"" + url + "\">" + name + "</a>");
                holder.entryTextView.setText(link);
                holder.entryTextView.setMovementMethod(WPLinkMovementMethod.getInstance());
            } else {
                holder.entryTextView.setText(name);
                holder.entryTextView.setMovementMethod(null);
            }

            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
            
            // totals
            holder.totalsTextView.setText(formatter.format(total));

            // icon
            holder.imageFrame.setVisibility(View.VISIBLE);
            if (icon != null && icon.length() > 0) {
                holder.networkImageView.setErrorImageResId(R.drawable.stats_blank_image);
                holder.networkImageView.setDefaultImageResId(R.drawable.stats_blank_image);
                holder.networkImageView.setImageUrl(icon, WordPress.imageLoader);
                holder.networkImageView.setVisibility(View.VISIBLE);
                holder.errorImageView.setVisibility(View.GONE);
            } else {
                holder.networkImageView.setVisibility(View.GONE);
                holder.errorImageView.setVisibility(View.VISIBLE);
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

}
