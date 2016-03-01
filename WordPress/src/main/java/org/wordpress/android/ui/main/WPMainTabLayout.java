package org.wordpress.android.ui.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;

/**
 * tab layout for main activity
 */
public class WPMainTabLayout extends TabLayout {

    private View mNoteBadge;

    public WPMainTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WPMainTabLayout(Context context) {
        super(context);
    }

    public WPMainTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void createTabs() {
        addTab(R.drawable.main_tab_sites, R.string.tabbar_accessibility_label_my_site, false);
        addTab(R.drawable.main_tab_reader, R.string.reader, false);
        addTab(R.drawable.main_tab_me, R.string.tabbar_accessibility_label_me, false);
        addTab(R.drawable.main_tab_notifications, R.string.notifications, true);
        checkNoteBadge();
    }

    private void addTab(@DrawableRes int iconId, @StringRes int contentDescriptionId, boolean isNoteTab) {
        View customView = LayoutInflater.from(getContext()).inflate(R.layout.tab_icon, null);

        ImageView icon = (ImageView) customView.findViewById(R.id.tab_icon);
        icon.setImageResource(iconId);
        icon.setContentDescription(getResources().getText(contentDescriptionId));

        // each tab has a badge icon, but we only care about the one on the notifications tab
        if (isNoteTab) {
            mNoteBadge = customView.findViewById(R.id.tab_badge);
        }
        addTab(newTab().setCustomView(customView));
    }

    /*
     * adds or removes the badge on the notifications tab depending on whether there are
     * unread notifications
     */
    void checkNoteBadge() {
        showNoteBadge(SimperiumUtils.hasUnreadNotes());
    }

    void showNoteBadge(boolean showBadge) {
        if (mNoteBadge == null) return;

        boolean isBadged = (mNoteBadge.getVisibility() == View.VISIBLE);
        if (showBadge == isBadged) {
            return;
        }

        float start = showBadge ? 0f : 1f;
        float end = showBadge ? 1f : 0f;

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, start, end);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, start, end);
        ObjectAnimator animScale = ObjectAnimator.ofPropertyValuesHolder(mNoteBadge, scaleX, scaleY);

        if (showBadge) {
            animScale.setInterpolator(new BounceInterpolator());
            animScale.setDuration(getContext().getResources().getInteger(android.R.integer.config_longAnimTime));
            animScale.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mNoteBadge.setVisibility(View.VISIBLE);
                }
            });
        } else {
            animScale.setInterpolator(new AccelerateInterpolator());
            animScale.setDuration(getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));
            animScale.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mNoteBadge.setVisibility(View.GONE);
                }
            });
        }

        animScale.start();
    }

    private boolean isValidPosition(int position) {
        return (position >=0 && position < getTabCount());
    }

    public void setSelectedTabPosition(int position) {
        if (!isValidPosition(position) || getSelectedTabPosition() == position) {
            return;
        }
        Tab tab = getTabAt(position);
        if (tab != null) {
            tab.select();
        }
    }
}
