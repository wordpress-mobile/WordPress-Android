package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * A fragment that appears as a 'page' in the {@link StatsAbsPagedViewFragment}.
 * The fragment has a {@link ContentObserver} to listen for changes in the supplied URIs.
 * By implementing {@link LoaderCallbacks}, it asynchronously fetches new data to update itself.
 * <p>
 * For phone layouts, this fragment appears as a listview, with the CursorAdapter supplying the cells.
 * </p>
 * <p>
 * For tablet layouts, this fragment appears as a linearlayout, with a maximum of 10 entries.
 * A linearlayout is necessary because a listview cannot be placed inside the scrollview of the tablet's root layout.
 * The linearlayout also gets its views from the CursorAdapter.
 * </p>
 */
public class StatsCursorFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARGS_URI = "ARGS_URI";
    private static final String ARGS_ENTRY_LABEL = "ARGS_ENTRY_LABEL";
    private static final String ARGS_TOTALS_LABEL = "ARGS_TOTALS_LABEL";
    private static final String ARGS_EMPTY_LABEL_TITLE = "ARGS_EMPTY_LABEL_TITLE";
    private static final String ARGS_EMPTY_LABEL_DESC = "ARGS_EMPTY_LABEL_DESC";
    private static final int NO_STRING_ID = -1;

    public static final String TAG = StatsCursorFragment.class.getSimpleName();

    private TextView mEmptyLabel;
    private LinearLayout mLinearLayout;

    private CursorAdapter mAdapter;
    private final ContentObserver mContentObserver = new MyObserver(new Handler());

    private StatsCursorInterface mCallback;

    public static StatsCursorFragment newInstance(Uri uri, int entryLabelResId, int totalsLabelResId,
                                                  int emptyLabelTitleResId) {
        return newInstance(uri, entryLabelResId, totalsLabelResId, emptyLabelTitleResId, NO_STRING_ID);
    }

    public static StatsCursorFragment newInstance(Uri uri, int entryLabelResId, int totalsLabelResId,
                                                  int emptyLabelTitleResId, int emptyLabelDescResId) {
        StatsCursorFragment fragment = new StatsCursorFragment();

        Bundle args = new Bundle();
        args.putString(ARGS_URI, uri.toString());
        args.putInt(ARGS_ENTRY_LABEL, entryLabelResId);
        args.putInt(ARGS_TOTALS_LABEL, totalsLabelResId);
        args.putInt(ARGS_EMPTY_LABEL_TITLE, emptyLabelTitleResId);
        args.putInt(ARGS_EMPTY_LABEL_DESC, emptyLabelDescResId);
        fragment.setArguments(args);

        return fragment;
    }

    private Uri getUri() {
        return Uri.parse(getArguments().getString(ARGS_URI));
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
        View view = inflater.inflate(R.layout.stats_list_fragment, container, false);

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());
        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);

        String label;
        if (getEmptyLabelDescResId() == NO_STRING_ID) {
            label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b>";
        } else {
            label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b> " + getString(getEmptyLabelDescResId());
        }
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getCurrentBlog() == null)
            return null;

        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
        if (TextUtils.isEmpty(blogId)) blogId = "0";
        return new CursorLoader(getActivity(), getUri(), null, "blogId=?", new String[] { blogId }, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCallback.onCursorLoaded(getUri(), data);
        if (mAdapter != null)
            mAdapter.changeCursor(data);
        configureEmptyLabel();
        reloadLinearLayout();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);
        configureEmptyLabel();
        reloadLinearLayout();
    }

    public void setListAdapter(CursorAdapter adapter) {
        mAdapter = adapter;
        reloadLinearLayout();
    }

    private void reloadLinearLayout() {
        if (getActivity() == null || mLinearLayout == null || mAdapter == null)
            return;

        // limit number of items to show otherwise it would cause performance issues on the LinearLayout
        int count = Math.min(mAdapter.getCount(), StatsActivity.STATS_GROUP_MAX_ITEMS);

        if (count == 0) {
            mLinearLayout.removeAllViews();
            return;
        }

        int numExistingViews = mLinearLayout.getChildCount();
        int altRowColor = getResources().getColor(R.color.stats_alt_row);

        // remove excess views
        if (count < numExistingViews) {
            int numToRemove = numExistingViews - count;
            mLinearLayout.removeViews(count, numToRemove);
            numExistingViews = count;
        }

        for (int i = 0; i < count; i++) {
            int bgColor = (i % 2 == 1 ? altRowColor : Color.TRANSPARENT);
            final View view;
            // reuse existing view when possible
            if (i < numExistingViews) {
                View convertView = mLinearLayout.getChildAt(i);
                view = mAdapter.getView(i, convertView, mLinearLayout);
                view.setBackgroundColor(bgColor);
            } else {
                view = mAdapter.getView(i, null, mLinearLayout);
                view.setBackgroundColor(bgColor);
                mLinearLayout.addView(view);
            }
        }
        mLinearLayout.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(getUri(), true, mContentObserver);
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
               getLoaderManager().restartLoader(0, null, StatsCursorFragment.this);
       }
    }

    private void configureEmptyLabel() {
        if (mAdapter == null || mAdapter.getCount() == 0)
            mEmptyLabel.setVisibility(View.VISIBLE);
        else
            mEmptyLabel.setVisibility(View.GONE);
    }
}
