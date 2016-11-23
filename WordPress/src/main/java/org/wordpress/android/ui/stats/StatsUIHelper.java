package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.text.Spannable;
import android.text.style.URLSpan;
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
import org.wordpress.android.WordPress;
import org.wordpress.android.util.DisplayUtils;

class StatsUIHelper {
    // Max number of rows to show in a stats fragment
    private static final int STATS_GROUP_MAX_ITEMS = 10;
    private static final int STATS_CHILD_MAX_ITEMS = 50;
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

    // Load more bars for 720DP tablets
    private static boolean shouldLoadMoreBars() {
        return (StatsUtils.getSmallestWidthDP() >= TABLET_720DP);
    }

    public static void reloadLinearLayout(Context ctx, ListAdapter adapter, LinearLayout linearLayout, int maxNumberOfItemsToshow) {
        if (ctx == null || linearLayout == null || adapter == null) {
            return;
        }

        // limit number of items to show otherwise it would cause performance issues on the LinearLayout
        int count = Math.min(adapter.getCount(), maxNumberOfItemsToshow);

        if (count == 0) {
            linearLayout.removeAllViews();
            return;
        }

        int numExistingViews = linearLayout.getChildCount();
        // remove excess views
        if (count < numExistingViews) {
            int numToRemove = numExistingViews - count;
            linearLayout.removeViews(count, numToRemove);
            numExistingViews = count;
        }

        int bgColor = Color.TRANSPARENT;
        for (int i = 0; i < count; i++) {
            final View view;
            // reuse existing view when possible
            if (i < numExistingViews) {
                View convertView = linearLayout.getChildAt(i);
                view = adapter.getView(i, convertView, linearLayout);
                view.setBackgroundColor(bgColor);
                setViewBackgroundWithoutResettingPadding(view, i == 0 ? 0 : R.drawable.stats_list_item_background);
            } else {
                view = adapter.getView(i, null, linearLayout);
                view.setBackgroundColor(bgColor);
                setViewBackgroundWithoutResettingPadding(view, i == 0 ? 0 : R.drawable.stats_list_item_background);
                linearLayout.addView(view);
            }
        }
        linearLayout.invalidate();
    }

    /**
     *
     * Padding information are reset when changing the background Drawable on a View.
     * The reason why setting an image resets the padding is because 9-patch images can encode padding.
     *
     * See http://stackoverflow.com/a/10469121 and
     * http://www.mail-archive.com/android-developers@googlegroups.com/msg09595.html
     *
     * @param v The view to apply the background resource
     * @param backgroundResId The resource ID
     */
    private static void setViewBackgroundWithoutResettingPadding(final View v, final int backgroundResId) {
        final int paddingBottom = v.getPaddingBottom(), paddingLeft = v.getPaddingLeft();
        final int paddingRight = v.getPaddingRight(), paddingTop = v.getPaddingTop();
        v.setBackgroundResource(backgroundResId);
        v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    public static void reloadLinearLayout(Context ctx, ListAdapter adapter, LinearLayout linearLayout) {
        reloadLinearLayout(ctx, adapter, linearLayout, STATS_GROUP_MAX_ITEMS);
    }

    public static void reloadGroupViews(final Context ctx,
                                        final ExpandableListAdapter mAdapter,
                                        final SparseBooleanArray mGroupIdToExpandedMap,
                                        final LinearLayout mLinearLayout) {
        reloadGroupViews(ctx, mAdapter, mGroupIdToExpandedMap, mLinearLayout, STATS_GROUP_MAX_ITEMS);
    }

    public static void reloadGroupViews(final Context ctx,
                                        final ExpandableListAdapter mAdapter,
                                        final SparseBooleanArray mGroupIdToExpandedMap,
                                        final LinearLayout mLinearLayout,
                                        final int maxNumberOfItemsToshow) {
        if (ctx == null || mLinearLayout == null || mAdapter == null || mGroupIdToExpandedMap == null) {
            return;
        }

        int groupCount = Math.min(mAdapter.getGroupCount(), maxNumberOfItemsToshow);
        if (groupCount == 0) {
            mLinearLayout.removeAllViews();
            return;
        }

        int numExistingGroupViews = mLinearLayout.getChildCount();

        // remove excess views
        if (groupCount < numExistingGroupViews) {
            int numToRemove = numExistingGroupViews - groupCount;
            mLinearLayout.removeViews(groupCount, numToRemove);
            numExistingGroupViews = groupCount;
        }

        int bgColor = Color.TRANSPARENT;

        // add each group
        for (int i = 0; i < groupCount; i++) {
            boolean isExpanded = mGroupIdToExpandedMap.get(i);

            // reuse existing view when possible
            final View groupView;
            if (i < numExistingGroupViews) {
                View convertView = mLinearLayout.getChildAt(i);
                groupView = mAdapter.getGroupView(i, isExpanded, convertView, mLinearLayout);
                groupView.setBackgroundColor(bgColor);
                setViewBackgroundWithoutResettingPadding(groupView, i == 0 ? 0 : R.drawable.stats_list_item_background);
            } else {
                groupView = mAdapter.getGroupView(i, isExpanded, null, mLinearLayout);
                groupView.setBackgroundColor(bgColor);
                setViewBackgroundWithoutResettingPadding(groupView, i == 0 ? 0 : R.drawable.stats_list_item_background);
                mLinearLayout.addView(groupView);
            }

            // groupView is recycled, we need to reset it to the original state.
            ViewGroup childContainer = (ViewGroup) groupView.findViewById(R.id.layout_child_container);
            if (childContainer != null) {
                childContainer.setVisibility(View.GONE);
            }
            // Remove any other prev animations set on the chevron
            final ImageView chevron = (ImageView) groupView.findViewById(R.id.stats_list_cell_chevron);
            if (chevron != null) {
                chevron.clearAnimation();
                chevron.setImageResource(R.drawable.stats_chevron_right);
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
                        StatsUIHelper.hideChildViews(groupView, groupPosition, true);
                    }
                }
            });
        }
    }

    /*
     * interpolator for all expand/collapse animations
    */
    private static Interpolator getInterpolator() {
        return new AccelerateInterpolator();
    }

    private static void hideChildViews(View groupView, int groupPosition, boolean animate) {
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
        StatsUIHelper.setGroupChevron(false, groupView, groupPosition, animate);
    }

    /*
     * shows the correct up/down chevron for the passed group
     */
    private static void setGroupChevron(final boolean isGroupExpanded, View groupView, int groupPosition, boolean animate) {
        final ImageView chevron = (ImageView) groupView.findViewById(R.id.stats_list_cell_chevron);
        if (chevron == null) {
            return;
        }
        if (isGroupExpanded) {
            // change the background of the parent
            setViewBackgroundWithoutResettingPadding(groupView, R.drawable.stats_list_item_expanded_background);
        } else {
            setViewBackgroundWithoutResettingPadding(groupView, groupPosition == 0 ? 0 : R.drawable.stats_list_item_background);
        }

        chevron.clearAnimation(); // Remove any other prev animations set on the chevron
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

    private static void showChildViews(ExpandableListAdapter mAdapter, LinearLayout mLinearLayout,
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
                // remove the right/left padding so the child total aligns to left
                childView.setPadding(0,
                        childView.getPaddingTop(),
                        0,
                        isLastChild ? 0 : childView.getPaddingBottom()); // No padding bottom on last child
                setViewBackgroundWithoutResettingPadding(childView, R.drawable.stats_list_item_child_background);
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

        StatsUIHelper.setGroupChevron(true, groupView, groupPosition, animate);
    }

    /**
     * Removes URL underlines in a string by replacing URLSpan occurrences by
     * URLSpanNoUnderline objects.
     *
     * @param pText A Spannable object. For example, a TextView casted as
     *               Spannable.
     */
    public static void removeUnderlines(Spannable pText) {
        URLSpan[] spans = pText.getSpans(0, pText.length(), URLSpan.class);

        for(URLSpan span:spans) {
            int start = pText.getSpanStart(span);
            int end = pText.getSpanEnd(span);
            pText.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            pText.setSpan(span, start, end, 0);
        }
    }

    public static int getNumOfBarsToShow() {
        if (StatsUtils.getSmallestWidthDP() >= TABLET_720DP && DisplayUtils.isLandscape(WordPress.getContext())) {
            return 15;
        } else if (StatsUtils.getSmallestWidthDP() >= TABLET_600DP) {
            return 10;
        } else {
            return 7;
        }
    }
}
