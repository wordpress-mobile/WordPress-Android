package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import org.wordpress.android.R;

public class StatsUIHelper {
    // Max number of rows to show in a stats fragment
    public static final int STATS_GROUP_MAX_ITEMS = 10;
    public static final int STATS_CHILD_MAX_ITEMS = 25;
    private static final int ANIM_DURATION = 150;

    // Used for tablet UI
    private static final int TABLET_720DP = 720;
    private static final int TABLET_600DP = 600;

    private static boolean isInLandscape(Activity act) {
        Display display = act.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return (point.y < point.x);
    }

    // split layout into two for 720DP tablets and 600DP tablets in landscape
    public static boolean shouldLoadSplitLayout(Activity act) {
        return (StatsUtils.getSmallestWidthDP() >= TABLET_720DP
                || (StatsUtils.getSmallestWidthDP() == TABLET_600DP && isInLandscape(act)));
    }

    public static void reloadLinearLayout(Context ctx, ListAdapter adapter, LinearLayout linearLayout) {
        if (ctx == null || linearLayout == null || adapter == null) {
            return;
        }

        // limit number of items to show otherwise it would cause performance issues on the LinearLayout
        int count = Math.min(adapter.getCount(), STATS_GROUP_MAX_ITEMS);

        if (count == 0) {
            linearLayout.removeAllViews();
            return;
        }

        int numExistingViews = linearLayout.getChildCount();
        int altRowColor = ctx.getResources().getColor(R.color.stats_alt_row);

        // remove excess views
        if (count < numExistingViews) {
            int numToRemove = numExistingViews - count;
            linearLayout.removeViews(count, numToRemove);
            numExistingViews = count;
        }

        for (int i = 0; i < count; i++) {
            int bgColor = (i % 2 == 1 ? altRowColor : Color.TRANSPARENT);
            final View view;
            // reuse existing view when possible
            if (i < numExistingViews) {
                View convertView = linearLayout.getChildAt(i);
                view = adapter.getView(i, convertView, linearLayout);
                view.setBackgroundColor(bgColor);
            } else {
                view = adapter.getView(i, null, linearLayout);
                view.setBackgroundColor(bgColor);
                linearLayout.addView(view);
            }
        }
        linearLayout.invalidate();
    }

    public static void reloadGroupViews(final Context ctx,
                                        final ExpandableListAdapter mAdapter,
                                        final SparseBooleanArray mGroupIdToExpandedMap,
                                        final LinearLayout mLinearLayout) {
        if (ctx == null || mLinearLayout == null || mAdapter == null || mGroupIdToExpandedMap == null) {
            return;
        }

        int groupCount = Math.min(mAdapter.getGroupCount(), STATS_GROUP_MAX_ITEMS);
        if (groupCount == 0) {
            mLinearLayout.removeAllViews();
            return;
        }

        int numExistingGroupViews = mLinearLayout.getChildCount();
        int altRowColor = ctx.getResources().getColor(R.color.stats_alt_row);

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
                StatsUIHelper.showChildViews(mAdapter, mLinearLayout, i, groupView, false);
            }

            // toggle expand/collapse when group view is tapped
            final int groupPosition = i;
            groupView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAdapter.getChildrenCount(groupPosition) == 0) {
                        return;
                    }
                    boolean shouldExpand = !mGroupIdToExpandedMap.get(groupPosition);
                    mGroupIdToExpandedMap.put(groupPosition, shouldExpand);
                    if (shouldExpand) {
                        StatsUIHelper.showChildViews(mAdapter, mLinearLayout, groupPosition, groupView, true);
                    } else {
                        StatsUIHelper.hideChildViews(groupView, true);
                    }
                }
            });
        }
    }

    /*
     * interpolator for all expand/collapse animations
    */
    public static Interpolator getInterpolator() {
        return new AccelerateInterpolator();
    }

    public static void hideChildViews(View groupView, boolean animate) {
        final ViewGroup childContainer = (ViewGroup) groupView.findViewById(R.id.layout_child_container);
        if (childContainer == null) {
            return;
        }
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
        StatsUIHelper.setGroupChevron(false, groupView, animate);
    }

    /*
     * shows the correct up/down chevron for the passed group
     */
    public static void setGroupChevron(final boolean isGroupExpanded, View groupView, boolean animate) {
        final ImageView chevron = (ImageView) groupView.findViewById(R.id.stats_list_cell_chevron);
        if (chevron == null) {
            return;
        }

        if (animate) {
            // make sure we start with the correct chevron for the prior state before animating it
            chevron.setImageResource(isGroupExpanded ? R.drawable.stats_chevron_right : R.drawable.stats_chevron_down);
            float start = (isGroupExpanded ? 0.0f : 0.0f);
            float end = (isGroupExpanded ? 90.0f : -90.0f);
            Animation rotate = new RotateAnimation(start, end, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(ANIM_DURATION);
            rotate.setInterpolator(getInterpolator());
            rotate.setFillAfter(true);
            chevron.startAnimation(rotate);
        } else {
            chevron.setImageResource(isGroupExpanded ? R.drawable.stats_chevron_down : R.drawable.stats_chevron_right);
        }
    }

    public static void showChildViews(ExpandableListAdapter mAdapter, LinearLayout mLinearLayout,
                                      int groupPosition, View groupView, boolean animate) {
        int childCount = Math.min(mAdapter.getChildrenCount(groupPosition), STATS_CHILD_MAX_ITEMS);
        if (childCount == 0) {
            return;
        }

        final ViewGroup childContainer = (ViewGroup) groupView.findViewById(R.id.layout_child_container);
        if (childContainer == null) {
            return;
        }

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

        StatsUIHelper.setGroupChevron(true, groupView, animate);
    }
}
