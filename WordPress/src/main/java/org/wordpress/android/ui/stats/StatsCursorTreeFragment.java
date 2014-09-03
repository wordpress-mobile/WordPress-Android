package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

/**
 * A fragment that appears as a 'page' in the {@link StatsAbsPagedViewFragment}. Similar to {@link StatsCursorFragment},
 * except it is used for stats that have expandable groups, such as Referrers or Clicks.
 * <p>
 * The fragment has a {@link ContentObserver} to listen for changes in the supplied group URIs.
 * By implementing {@link LoaderCallbacks}, it asynchronously fetches new data to update itself.
 * It then restarts loaders on the children URI for each group id, which results in the children views being updated.
 * </p>
 * <p>
 * This fragment appears as a linearlayout, with a maximum of 10 entries.
 * A linearlayout is necessary because a listview cannot be placed inside the scrollview of the root layout.
 * The linearlayout also gets its group and children views from the CursorTreeAdapter.
 * </p>
 */
public class StatsCursorTreeFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, StatsCursorLoaderCallback {
    private static final int LOADER_URI_GROUP_INDEX = -1;
    private static final String ARGS_GROUP_URI = "ARGS_GROUP_URI";
    private static final String ARGS_CHILDREN_URI = "ARGS_CHILDREN_URI";
    private static final String ARGS_ENTRY_LABEL = "ARGS_ENTRY_LABEL";
    private static final String ARGS_TOTALS_LABEL = "ARGS_TOTALS_LABEL";
    private static final String ARGS_EMPTY_LABEL_TITLE = "ARGS_EMPTY_LABEL_TITLE";
    private static final String ARGS_EMPTY_LABEL_DESC = "ARGS_EMPTY_LABEL_DESC";

    public static final String TAG = StatsCursorTreeFragment.class.getSimpleName();

    private TextView mEmptyLabel;
    private LinearLayout mLinearLayout;

    private SparseBooleanArray mGroupIdToExpandedMap;

    private CursorTreeAdapter mAdapter;
    private final ContentObserver mContentObserver = new MyObserver(new Handler());

    private StatsCursorInterface mCallback;

    public static StatsCursorTreeFragment newInstance(Uri groupUri, Uri childrenUri, int entryLabelResId,
                                                      int totalsLabelResId, int emptyLabelTitleResId,
                                                      int emptyLabelDescResId, int localTableBlogID) {
        StatsCursorTreeFragment fragment = new StatsCursorTreeFragment();

        Bundle args = new Bundle();
        args.putString(ARGS_GROUP_URI, groupUri.toString());
        args.putString(ARGS_CHILDREN_URI, childrenUri.toString());
        args.putInt(ARGS_ENTRY_LABEL, entryLabelResId);
        args.putInt(ARGS_TOTALS_LABEL, totalsLabelResId);
        args.putInt(ARGS_EMPTY_LABEL_TITLE, emptyLabelTitleResId);
        args.putInt(ARGS_EMPTY_LABEL_DESC, emptyLabelDescResId);
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGroupIdToExpandedMap = new SparseBooleanArray();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_expandable_list_fragment, container, false);

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());

        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        String label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b> " + getString(getEmptyLabelDescResId());
        if (label.contains("<")) {
            mEmptyLabel.setText(Html.fromHtml(label));
        } else {
            mEmptyLabel.setText(label);
        }
        configureEmptyLabel();

        mLinearLayout = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
        mLinearLayout.setVisibility(View.VISIBLE);

        return view;
    }

    private int getEntryLabelResId() {
        return getArguments().getInt(ARGS_ENTRY_LABEL);
    }

    private int getTotalsLabelResId() {
        return getArguments().getInt(ARGS_TOTALS_LABEL);
    }

    private int getEmptyLabelTitleResId() {
        return getArguments().getInt(ARGS_EMPTY_LABEL_TITLE);
    }

    private int getEmptyLabelDescResId() {
        return getArguments().getInt(ARGS_EMPTY_LABEL_DESC);
    }

    private int getLocalTableBlogID() {
        return getArguments().getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_URI_GROUP_INDEX, null, this);
    }

    private int mNumChildLoaders = 0;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getBlog(getLocalTableBlogID()) == null)
            return null;

        String blogId = WordPress.getBlog(getLocalTableBlogID()).getDotComBlogId();
        if (TextUtils.isEmpty(blogId))
            blogId = "0";

        Uri uri = getGroupUri();

        if (id == LOADER_URI_GROUP_INDEX) {
            return new CursorLoader(getActivity(), uri, null, "blogId=?", new String[] { blogId }, null);
        } else {
            mNumChildLoaders++;
            uri = getChildrenUri();
            String groupId = args.getString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID);
            long date = args.getLong(StatsCursorLoaderCallback.BUNDLE_DATE);
            return new CursorLoader(getActivity(), uri, null, "blogId=? AND groupId=? AND date=?", new String[] { blogId, groupId, date + "" }, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // cursor is for groups
        boolean isGroupLoader = (loader.getId() == LOADER_URI_GROUP_INDEX);
        if (isGroupLoader) {
            // start loaders on children
            while (data.moveToNext()) {
                String groupId = data.getString(data.getColumnIndex("groupId"));
                long date = data.getLong(data.getColumnIndex("date"));

                Bundle bundle = new Bundle();
                bundle.putString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID, groupId);
                bundle.putLong(StatsCursorLoaderCallback.BUNDLE_DATE, date);

                getLoaderManager().restartLoader(data.getPosition(), bundle, StatsCursorTreeFragment.this);
            }

            if (mCallback != null) {
                mCallback.onCursorLoaded(getGroupUri(), data);
            } else {
                AppLog.e(AppLog.T.STATS, "mCallback is null");
            }

            if (mAdapter != null)
                mAdapter.changeCursor(data);
        } else {
            // cursor is for children
            if (mNumChildLoaders > 0)
                mNumChildLoaders--;
            if (mAdapter != null) {
                // due to a race condition that occurs when stats are refreshed,
                // it is possible to have more rows in the listview initially than when done refreshing,
                // causing null pointer exceptions to occur.
                try {
                    mAdapter.setChildrenCursor(loader.getId(), data);
                } catch (NullPointerException e) {
                    // do nothing
                }
            }
        }

        // refresh views if this was a group loader, or if all child loaders have completed
        if (isGroupLoader || mNumChildLoaders == 0) {
            configureEmptyLabel();
            StatsUIHelper.reloadGroupViews(getActivity(), mAdapter, mGroupIdToExpandedMap, mLinearLayout);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGroupIdToExpandedMap.clear();
        mNumChildLoaders = 0;

        if (mAdapter != null)
            mAdapter.changeCursor(null);
        configureEmptyLabel();
        StatsUIHelper.reloadGroupViews(getActivity(), mAdapter, mGroupIdToExpandedMap, mLinearLayout);
    }

    public void setCallback(StatsCursorInterface callback) {
        mCallback = callback;
    }

    public void setListAdapter(CursorTreeAdapter adapter) {
        mAdapter = adapter;
        StatsUIHelper.reloadGroupViews(getActivity(), mAdapter, mGroupIdToExpandedMap, mLinearLayout);
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

    private void configureEmptyLabel() {
        if (mAdapter == null || mAdapter.getGroupCount() == 0)
            mEmptyLabel.setVisibility(View.VISIBLE);
        else
            mEmptyLabel.setVisibility(View.GONE);
    }

    @Override
    public void onUriRequested(int id, Uri uri, Bundle bundle) {
        if (isAdded() && uri.equals(getChildrenUri())) {
            getLoaderManager().restartLoader(id, bundle, StatsCursorTreeFragment.this);
        }
    }
}
