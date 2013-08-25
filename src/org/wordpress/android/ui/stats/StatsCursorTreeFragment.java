package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.Utils;

public class StatsCursorTreeFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>, StatsCursorLoaderCallback {

    private static final int MAX_ITEMS_ON_TABLET = 10;
    private static final int LOADER_URI_GROUP_INDEX = -1;
    
    private static final String ARGS_GROUP_URI = "ARGS_GROUP_URI";
    private static final String ARGS_CHILDREN_URI = "ARGS_CHILDREN_URI";
    private static final String ARGS_ENTRY_LABEL = "ARGS_ENTRY_LABEL";
    private static final String ARGS_TOTALS_LABEL = "ARGS_TOTALS_LABEL";
    private static final String ARGS_EMPTY_LABEL = "ARGS_EMPTY_LABEL";

    public static final String TAG = StatsCursorTreeFragment.class.getSimpleName();
    
    private TextView mEntryLabel;
    private TextView mTotalsLabel;
    private TextView mEmptyLabel;
    private ExpandableListView mListView;
    private LinearLayout mLinearLayout;
    
    private SparseBooleanArray mGroupIdToExpandedMap;

    private CursorTreeAdapter mAdapter;
    private ContentObserver mContentObserver = new MyObserver(new Handler());
    
    private StatsCursorInterface mCallback;
    
    public static StatsCursorTreeFragment newInstance(Uri groupUri, Uri childrenUri, int entryLabelResId, int totalsLabelResId, int emptyLabelResId) {
        
        StatsCursorTreeFragment fragment = new StatsCursorTreeFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_GROUP_URI, groupUri.toString());
        args.putString(ARGS_CHILDREN_URI, childrenUri.toString());
        args.putInt(ARGS_ENTRY_LABEL, entryLabelResId);
        args.putInt(ARGS_TOTALS_LABEL, totalsLabelResId);
        args.putInt(ARGS_EMPTY_LABEL, emptyLabelResId);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    private Uri getGroupUri() {
        return Uri.parse(getArguments().getString(ARGS_GROUP_URI));
    }
    
    private Uri getChildrenUri() {
        return Uri.parse(getArguments().getString(ARGS_CHILDREN_URI));
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (StatsCursorInterface) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString() + " must implement " + StatsCursorInterface.class.getSimpleName());
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        mGroupIdToExpandedMap = new SparseBooleanArray();
        
        View view = inflater.inflate(R.layout.stats_expandable_list_fragment, container, false);
        
        mEntryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        mEntryLabel.setText(getEntryLabelResId());
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        mTotalsLabel.setText(getTotalsLabelResId());
        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        mEmptyLabel.setText(Html.fromHtml(getString(getEmptyLabelResId())));
        configureEmptyLabel();
        
        if (isTablet()) {
            mLinearLayout = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
            mLinearLayout.setVisibility(View.VISIBLE);
        } else {
            mListView = (ExpandableListView) view.findViewById(R.id.stats_list_listview);
            mListView.setAdapter(mAdapter);
            mListView.setVisibility(View.VISIBLE);
        }
        
        return view;
    }

    private int getEntryLabelResId() {
        return getArguments().getInt(ARGS_ENTRY_LABEL);
    }

    private int getTotalsLabelResId() {
        return getArguments().getInt(ARGS_TOTALS_LABEL);
    }

    private int getEmptyLabelResId() {
        return getArguments().getInt(ARGS_EMPTY_LABEL);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_URI_GROUP_INDEX, null, this);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getCurrentBlog() == null)
            return null;

        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        Uri uri = getGroupUri();
        
        if (id == LOADER_URI_GROUP_INDEX) { 
            return new CursorLoader(getActivity(), uri, null, "blogId=?", new String[] { blogId }, null);
        } else {
            uri = getChildrenUri();
            String groupId = args.getString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID);
            long date = args.getLong(StatsCursorLoaderCallback.BUNDLE_DATE);
            return new CursorLoader(getActivity(), uri, null, "blogId=? AND groupId=? AND date=?", new String[] { blogId, groupId, date + "" }, null);
        }
        
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        
        // cursor is for groups
        if (loader.getId() == LOADER_URI_GROUP_INDEX) {
            
            // start loaders on children
            while (data.moveToNext()) {
                String groupId = data.getString(data.getColumnIndex("groupId"));
                long date = data.getLong(data.getColumnIndex("date"));
                
                Bundle bundle = new Bundle();
                bundle.putString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID, groupId);
                bundle.putLong(StatsCursorLoaderCallback.BUNDLE_DATE, date);
                
                getLoaderManager().restartLoader(data.getPosition(), bundle, StatsCursorTreeFragment.this);
            }
        

            mCallback.onCursorLoaded(getGroupUri(), data);
            
            if (mAdapter != null)
                mAdapter.changeCursor(data);
        } else {
            // cursor is for children
            if (mAdapter != null && ((Integer) loader.getId()) != null) {
                mAdapter.setChildrenCursor(loader.getId(), data);
            }
        }
        
        configureEmptyLabel();
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        
        mGroupIdToExpandedMap.clear();
        
        if (mAdapter != null)
            mAdapter.changeCursor(null);
        configureEmptyLabel();
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    public void setListAdapter(CursorTreeAdapter adapter) {
        mAdapter = adapter;
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    private void reloadLinearLayout() {
        if (mLinearLayout == null || mAdapter == null)
            return; 
        
        mLinearLayout.removeAllViews();
        
        // limit number of items to show otherwise it would cause performance issues on the linearlayout
        int groupCount = Math.min(mAdapter.getGroupCount(), MAX_ITEMS_ON_TABLET);
        for (int i = 0; i < groupCount; i++) {
            
            boolean isExpanded = mGroupIdToExpandedMap.get(i);
            View view = mAdapter.getGroupView(i, isExpanded, null, mLinearLayout);
            view.setTag(i);
//            if (i % 2 == 1)
//                view.setBackgroundColor(getResources().getColor(R.color.stats_alt_row));
            mLinearLayout.addView(view);
            view.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    mGroupIdToExpandedMap.put(position, !mGroupIdToExpandedMap.get(position));
                    reloadLinearLayout();
                }
            });

            // add divider
            getActivity().getLayoutInflater().inflate(R.layout.stats_list_divider, mLinearLayout, true);
            
            
            if (isExpanded) {
                int childrenCount = mAdapter.getChildrenCount(i);
                for (int j = 0; j < childrenCount; j++) {
                    boolean isLastChild = (j == childrenCount - 1);
                    View childView = mAdapter.getChildView(i, j, isLastChild, null, mLinearLayout);
                    mLinearLayout.addView(childView);
                    
                    // add divider
                    getActivity().getLayoutInflater().inflate(R.layout.stats_list_divider, mLinearLayout, true);
                    
                }
            }
            
        }
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(getGroupUri(), true, mContentObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
    }
    
    class MyObserver extends ContentObserver {      
       public MyObserver(Handler handler) {
          super(handler);           
       }

       @Override
       public void onChange(boolean selfChange) {
           if (isAdded())
               getLoaderManager().restartLoader(LOADER_URI_GROUP_INDEX, null, StatsCursorTreeFragment.this);           
       }        
    }
    
    private boolean isTablet() {
        return Utils.isTablet();
    }

    private void configureEmptyLabel() {
        if (mAdapter == null || mAdapter.getGroupCount() == 0)
            mEmptyLabel.setVisibility(View.VISIBLE);
        else
            mEmptyLabel.setVisibility(View.GONE);
    }

    @Override
    public void onUriRequested(int id, Uri uri, Bundle bundle) {
        if (uri.equals(getChildrenUri())) {
            getLoaderManager().restartLoader(id, bundle, StatsCursorTreeFragment.this);
        }
    }
}
