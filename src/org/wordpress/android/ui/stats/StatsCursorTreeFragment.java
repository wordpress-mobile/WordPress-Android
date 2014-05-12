package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * A fragment that appears as a 'page' in the {@link StatsAbsPagedViewFragment}. Similar to {@link StatsCursorFragment},
 * except it is used for stats that have expandable groups, such as Referrers or Clicks.
 * <p>
 * The fragment has a {@link ContentObserver} to listen for changes in the supplied group URIs.
 * By implementing {@link LoaderCallbacks}, it asynchronously fetches new data to update itself.
 * It then restarts loaders on the children URI for each group id, which results in the children views being updated.
 * </p>
 * <p>
 * For phone layouts, this fragment appears as an expandable listview, with a CursorTreeAdapter supplying the group and children views.
 * </p>
 * <p>
 * For tablet layouts, this fragment appears as a linearlayout, with a maximum of 10 entries.
 * A linearlayout is necessary because a listview cannot be placed inside the scrollview of the tablet's root layout.
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

    private static final int ANIM_DURATION = 150;

    public static StatsCursorTreeFragment newInstance(Uri groupUri, Uri childrenUri, int entryLabelResId,
                                                      int totalsLabelResId, int emptyLabelTitleResId,
                                                      int emptyLabelDescResId) {
        StatsCursorTreeFragment fragment = new StatsCursorTreeFragment();

        Bundle args = new Bundle();
        args.putString(ARGS_GROUP_URI, groupUri.toString());
        args.putString(ARGS_CHILDREN_URI, childrenUri.toString());
        args.putInt(ARGS_ENTRY_LABEL, entryLabelResId);
        args.putInt(ARGS_TOTALS_LABEL, totalsLabelResId);
        args.putInt(ARGS_EMPTY_LABEL_TITLE, emptyLabelTitleResId);
        args.putInt(ARGS_EMPTY_LABEL_DESC, emptyLabelDescResId);
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_URI_GROUP_INDEX, null, this);
    }

    private int mNumChildLoaders = 0;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getCurrentBlog() == null)
            return null;

        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
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

            mCallback.onCursorLoaded(getGroupUri(), data);

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
            reloadGroupViews();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGroupIdToExpandedMap.clear();
        mNumChildLoaders = 0;

        if (mAdapter != null)
            mAdapter.changeCursor(null);
        configureEmptyLabel();
        reloadGroupViews();
    }

    public void setListAdapter(CursorTreeAdapter adapter) {
        mAdapter = adapter;
        reloadGroupViews();
    }

    /*
     * interpolator for all expand/collapse animations
     */
    private Interpolator getInterpolator() {
        return new AccelerateInterpolator();
    }

    private void reloadGroupViews() {
        if (getActivity() == null || mLinearLayout == null || mAdapter == null)
            return;

        int groupCount = Math.min(mAdapter.getGroupCount(), StatsActivity.STATS_GROUP_MAX_ITEMS);
        if (groupCount == 0) {
            mLinearLayout.removeAllViews();
            return;
        }

        int numExistingGroupViews = mLinearLayout.getChildCount();
        int altRowColor = getResources().getColor(R.color.stats_alt_row);

        // remove excess views
        if (groupCount < numExistingGroupViews) {
            int numToRemove = numExistingGroupViews - groupCount;
            mLinearLayout.removeViews(groupCount, numToRemove);
            numExistingGroupViews = groupCount;
        }

        // add each group
        for (int i = 0; i < groupCount; i++) {
            boolean isExpanded = mGroupIdToExpandedMap.get(i);
            int bgColor = (i % 2 == 1 ? altRowColor : Color.TRANSPARENT);

            // reuse existing view when possible
            final View groupView;
            if (i < numExistingGroupViews) {
                View convertView = mLinearLayout.getChildAt(i);
                groupView = mAdapter.getGroupView(i, isExpanded, convertView, mLinearLayout);
                groupView.setBackgroundColor(bgColor);
            } else {
                groupView = mAdapter.getGroupView(i, isExpanded, null, mLinearLayout);
                groupView.setBackgroundColor(bgColor);
                mLinearLayout.addView(groupView);
            }

            // add children if this group is expanded
            if (isExpanded) {
                showChildViews(i, groupView, false);
            }

            // toggle expand/collapse when group view is tapped
            final int groupPosition = i;
            groupView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAdapter.getChildrenCount(groupPosition) == 0)
                        return;
                    boolean shouldExpand = !mGroupIdToExpandedMap.get(groupPosition);
                    mGroupIdToExpandedMap.put(groupPosition, shouldExpand);
                    if (shouldExpand) {
                        showChildViews(groupPosition, groupView, true);
                    } else {
                        hideChildViews(groupView, true);
                    }
                }
            });
        }
    }

    private void showChildViews(int groupPosition, View groupView, boolean animate) {
        int childCount = Math.min(mAdapter.getChildrenCount(groupPosition), StatsActivity.STATS_CHILD_MAX_ITEMS);
        if (childCount == 0)
            return;

        final ViewGroup childContainer = (ViewGroup) groupView.findViewById(R.id.layout_child_container);
        if (childContainer == null)
            return;

        int numExistingViews = childContainer.getChildCount();
        if (childCount < numExistingViews) {
            int numToRemove = numExistingViews - childCount;
            childContainer.removeViews(childCount, numToRemove);
            numExistingViews = childCount;
        }

        for (int i = 0; i < childCount; i++) {
            boolean isLastChild = (i == childCount - 1);
            if (i < numExistingViews) {
                View convertView = childContainer.getChildAt(i);
                mAdapter.getChildView(groupPosition, i, isLastChild, convertView, mLinearLayout);
            } else {
                View childView = mAdapter.getChildView(groupPosition, i, isLastChild, null, mLinearLayout);
                // remove the right padding so the child total aligns with the group total
                childView.setPadding(childView.getPaddingLeft(),
                                     childView.getPaddingTop(),
                                     0,
                                     childView.getPaddingBottom());
                childContainer.addView(childView);
            }
        }

        if (childContainer.getVisibility() != View.VISIBLE) {
            if (animate) {
                Animation expand = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f);
                expand.setDuration(ANIM_DURATION);
                expand.setInterpolator(getInterpolator());
                childContainer.startAnimation(expand);
            }
            childContainer.setVisibility(View.VISIBLE);
        }

        setGroupChevron(true, groupView, animate);
    }

    private void hideChildViews(View groupView, boolean animate) {
        final ViewGroup childContainer = (ViewGroup) groupView.findViewById(R.id.layout_child_container);
        if (childContainer == null)
            return;
        if (childContainer.getVisibility() != View.GONE) {
            if (animate) {
                Animation expand = new ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f);
                expand.setDuration(ANIM_DURATION);
                expand.setInterpolator(getInterpolator());
                expand.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) { }
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        childContainer.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });
                childContainer.startAnimation(expand);
            } else {
                childContainer.setVisibility(View.GONE);
            }
        }
        setGroupChevron(false, groupView, animate);
    }

    /*
     * shows the correct up/down chevron for the passed group
     */
    private void setGroupChevron(final boolean isGroupExpanded, View groupView, boolean animate) {
        final ImageView chevron = (ImageView) groupView.findViewById(R.id.stats_list_cell_chevron);
        if (chevron == null)
            return;

        if (animate) {
            // make sure we start with the correct chevron for the prior state before animating it
            chevron.setImageResource(isGroupExpanded ? R.drawable.stats_chevron_right : R.drawable.stats_chevron_down);
            float start = (isGroupExpanded ? 0.0f : 0.0f);
            float end = (isGroupExpanded ? 90.0f : -90.0f);
            Animation rotate = new RotateAnimation(start, end, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(ANIM_DURATION);
            rotate.setInterpolator(getInterpolator());
            rotate.setFillAfter(true);
            chevron.startAnimation(rotate);
        } else {
            chevron.setImageResource(isGroupExpanded ? R.drawable.stats_chevron_down : R.drawable.stats_chevron_right);
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
