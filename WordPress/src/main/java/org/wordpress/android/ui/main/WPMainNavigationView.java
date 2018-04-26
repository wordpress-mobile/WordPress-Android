package org.wordpress.android.ui.main;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemReselectedListener;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AniUtils.Duration;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.reflect.Field;

import static android.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE;

/*
 * Bottom navigation view and related fragment adapter used by the main activity
 * for the four primary views
 */
public class WPMainNavigationView extends BottomNavigationView
        implements OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private static final int NUM_PAGES = 5;

    static final int PAGE_MY_SITE = 0;
    static final int PAGE_READER = 1;
    static final int PAGE_NEW_POST = 2;
    static final int PAGE_ME = 3;
    static final int PAGE_NOTIFS = 4;

    private NavAdapter mNavAdapter;
    private FragmentManager mFragmentManager;
    private OnPageListener mListener;
    private int mPrevPosition = -1;

    interface OnPageListener {
        void onPageChanged(int position);
        void onNewPostButtonClicked();
    }

    public WPMainNavigationView(Context context) {
        super(context);
    }

    public WPMainNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPMainNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    void init(@NonNull FragmentManager fm, @NonNull OnPageListener listener) {
        mFragmentManager = fm;
        mListener = listener;

        mNavAdapter = new NavAdapter();
        assignNavigationListeners(true);
        disableShiftMode();

        // overlay each item with our custom view
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) getChildAt(0);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int position = 0; position < getMenu().size(); position++) {
            BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(position);
            View customView = inflater.inflate(R.layout.bottomn_navigation_item, menuView, false);
            TextView txtLabel = customView.findViewById(R.id.nav_label);
            ImageView imgIcon = customView.findViewById(R.id.nav_icon);
            txtLabel.setText(getTitleForPosition(position));
            imgIcon.setImageResource(getDrawableResForPosition(position));
            itemView.addView(customView);
        }
    }

    /*
     * uses reflection to disable "shift mode" so the item are equal width
     */
    @SuppressLint("RestrictedApi")
    private void disableShiftMode() {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                item.setShiftingMode(false);
                // force the view to update
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            AppLog.e(T.MAIN, "Unable to disable shift mode", e);
        } catch (IllegalAccessException e) {
            AppLog.e(T.MAIN, "Unable to disable shift mode", e);
        }
    }

    private void assignNavigationListeners(boolean assign) {
        setOnNavigationItemSelectedListener(assign ? this : null);
        setOnNavigationItemReselectedListener(assign ? this : null);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int position = getPositionForItemId(item.getItemId());
        if (position == PAGE_NEW_POST) {
            handlePostButtonClicked();
            return false;
        } else {
            setCurrentPosition(position, false);
            mListener.onPageChanged(position);
            return true;
        }
    }

    private void handlePostButtonClicked() {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) getChildAt(0);
        View postView = menuView.getChildAt(PAGE_NEW_POST);

        // animate the button before telling the listener the post button was clicked - this way
        // the user sees the animation before the editor appears
        AniUtils.startAnimation(postView, R.anim.notifications_button_scale, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // noop
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mListener.onNewPostButtonClicked();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
                // noop
            }
        });
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
        // scroll the active fragment's contents to the top when user re-taps the current item
        int position = getPositionForItemId(item.getItemId());
        if (position != PAGE_NEW_POST) {
            Fragment fragment = mNavAdapter.getFragment(position);
            if (fragment instanceof OnScrollToTopListener) {
                ((OnScrollToTopListener) fragment).onScrollToTop();
            }
        }
    }

    Fragment getActiveFragment() {
        return mNavAdapter.getFragment(getCurrentPosition());
    }

    private int getPositionForItemId(@IdRes int itemId) {
        switch (itemId) {
            case R.id.nav_sites:
                return PAGE_MY_SITE;
            case R.id.nav_reader:
                return PAGE_READER;
            case R.id.nav_write:
                return PAGE_NEW_POST;
            case R.id.nav_me:
                return PAGE_ME;
            default:
                return PAGE_NOTIFS;
        }
    }

    private @IdRes int getItemIdForPosition(int position) {
        switch (position) {
            case PAGE_MY_SITE:
                return R.id.nav_sites;
            case PAGE_READER:
                return R.id.nav_reader;
            case PAGE_NEW_POST:
                return R.id.nav_write;
            case PAGE_ME:
                return R.id.nav_me;
            default:
                return R.id.nav_notifications;
        }
    }

    int getCurrentPosition() {
        return getPositionForItemId(getSelectedItemId());
    }

    void setCurrentPosition(int position) {
        setCurrentPosition(position, true);
    }

    private void setCurrentPosition(int position, boolean ensureSelected) {
        // new post page can't be selected, only tapped
        if (position == PAGE_NEW_POST) {
            return;
        }

        // remove the title and selected state from the previously selected item
        if (mPrevPosition > -1) {
            showTitleForPosition(mPrevPosition, false);
            getImageViewForPosition(mPrevPosition).setSelected(false);
        }

        // set the title and selected state from the newly selected item
        showTitleForPosition(position, true);
        getImageViewForPosition(position).setSelected(true);
        mPrevPosition = position;

        if (ensureSelected) {
            // temporarily disable the nav listeners so they don't fire when we change the selected page
            assignNavigationListeners(false);
            try {
                setSelectedItemId(getItemIdForPosition(position));
            } finally {
                assignNavigationListeners(true);
            }
        }

        Fragment fragment = mNavAdapter.getFragment(position);
        if (fragment != null) {
            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setTransition(TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }

    @DrawableRes int getDrawableResForPosition(int position) {
        switch (position) {
            case PAGE_MY_SITE:
                return R.drawable.ic_my_sites_white_32dp;
            case PAGE_READER:
                return R.drawable.ic_reader_white_32dp;
            case PAGE_NEW_POST:
                return R.drawable.ic_create_white_24dp;
            case PAGE_ME:
                return R.drawable.ic_user_circle_white_32dp;
            default:
                return R.drawable.ic_bell_white_32dp;
        }
    }

    CharSequence getTitleForPosition(int position) {
        @StringRes int idRes;
        switch (position) {
            case PAGE_MY_SITE:
                idRes = R.string.my_site_section_screen_title;
                break;
            case PAGE_READER:
                idRes = R.string.reader_screen_title;
                break;
            case PAGE_NEW_POST:
                idRes = R.string.write_post;
                break;
            case PAGE_ME:
                idRes = R.string.me_section_screen_title;
                break;
            default:
                idRes = R.string.notifications_screen_title;
                break;
        }
        return getContext().getString(idRes);
    }

    private TextView getTitleViewForPosition(int position) {
        BottomNavigationItemView itemView = getItemView(position);
        return itemView.findViewById(R.id.nav_label);
    }

    private ImageView getImageViewForPosition(int position) {
        BottomNavigationItemView itemView = getItemView(position);
        return itemView.findViewById(R.id.nav_icon);
    }

    private void showTitleForPosition(int position, boolean show) {
        getTitleViewForPosition(position).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     */
    void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // reset the timestamp that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        ReaderPostListFragment.resetLastUpdateDate();

        // remember the current position, reset the adapter so new fragments are created, then restore position
        int position = getCurrentPosition();
        mNavAdapter.reset();
        setCurrentPosition(position);
    }

    Fragment getFragment(int position) {
        return mNavAdapter.getFragment(position);
    }

    private BottomNavigationItemView getItemView(int position) {
        if (isValidPosition(position)) {
            BottomNavigationMenuView menuView = (BottomNavigationMenuView) getChildAt(0);
            return (BottomNavigationItemView) menuView.getChildAt(position);
        }
        return null;
    }

    void showNoteBadge(boolean showBadge) {
        BottomNavigationItemView notifView = getItemView(PAGE_NOTIFS);
        View badgeView = notifView.findViewById(R.id.badge);

        int currentVisibility = badgeView.getVisibility();
        int newVisibility = showBadge ? View.VISIBLE : View.GONE;
        if (currentVisibility == newVisibility) {
            return;
        }

        if (showBadge) {
            AniUtils.fadeIn(badgeView, Duration.MEDIUM);
        } else {
            AniUtils.fadeOut(badgeView, Duration.MEDIUM);
        }
    }

    boolean isValidPosition(int position) {
        return (position >= 0 && position < NUM_PAGES);
    }

    private class NavAdapter {
        private final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_PAGES);

        void reset() {
            mFragments.clear();
        }

        private Fragment createFragment(int position) {
            Fragment fragment;
            switch (position) {
                case PAGE_MY_SITE:
                    fragment = MySiteFragment.newInstance();
                    break;
                case PAGE_READER:
                    fragment = ReaderPostListFragment.newInstance();
                    break;
                case PAGE_ME:
                    fragment = MeFragment.newInstance();
                    break;
                case PAGE_NOTIFS:
                    fragment = NotificationsListFragment.newInstance();
                    break;
                default:
                    return null;
            }

            mFragments.put(position, fragment);
            return fragment;
        }

        Fragment getFragment(int position) {
            if (isValidPosition(position) && mFragments.get(position) != null) {
              return mFragments.get(position);
            } else {
                return createFragment(position);
            }
        }
    }
}
