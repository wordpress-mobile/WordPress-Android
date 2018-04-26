package org.wordpress.android.ui.main;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemReselectedListener;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

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
    private View mBadgeView;
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

        mNavAdapter = new NavAdapter(mFragmentManager);
        assignNavigationListeners(true);
        disableShiftMode();

        // we only show a title for the selected item so remove all the titles (note we can't do this in
        // xml because it results in a warning)
        for (int i = 0; i < getMenu().size(); i++) {
            getMenu().getItem(i).setTitle(null);
        }

        BottomNavigationMenuView menuView = (BottomNavigationMenuView) getChildAt(0);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // add a larger icon to the post button
        BottomNavigationItemView postView = (BottomNavigationItemView) menuView.getChildAt(PAGE_NEW_POST);
        View postIcon = inflater.inflate(R.layout.new_post_item, menuView, false);
        postView.addView(postIcon);

        // add the notification badge to the notification menu item
        BottomNavigationItemView notifView = (BottomNavigationItemView) menuView.getChildAt(PAGE_NOTIFS);
        mBadgeView = inflater.inflate(R.layout.badge_layout, menuView, false);
        notifView.addView(mBadgeView);
        mBadgeView.setVisibility(View.GONE);
    }

    /*
     * uses reflection to disable "shift mode" so the item are equal width and all show captions
     * https://readyandroid.wordpress.com/disable-bottomnavigationview-shift-mode/
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
                // set once again checked value, so view will be updated
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

    private int getPositionForItemId(int itemId) {
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

        // remove the title from the previous position then set it for the new one
        if (mPrevPosition > -1) {
            getMenu().getItem(mPrevPosition).setTitle(null);
        }
        getMenu().getItem(position).setTitle(getMenuTitleForPosition(position));
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

    CharSequence getMenuTitleForPosition(int position) {
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

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     * TODO: test this with the new nav
     */
    void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // reset the timestamp that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        ReaderPostListFragment.resetLastUpdateDate();

        // remember the current position, then recreate the adapter so new fragments are created
        int position = getCurrentPosition();
        mNavAdapter = new NavAdapter(mFragmentManager);

        // restore previous position
        setCurrentPosition(position);
    }

    Fragment getFragment(int position) {
        return mNavAdapter.getFragment(position);
    }

    void showNoteBadge(boolean showBadge) {
        int currentVisibility = mBadgeView.getVisibility();
        int newVisibility = showBadge ? View.VISIBLE : View.GONE;
        if (currentVisibility == newVisibility) {
            return;
        }

        if (showBadge) {
            AniUtils.fadeIn(mBadgeView, Duration.MEDIUM);
        } else {
            AniUtils.fadeOut(mBadgeView, Duration.MEDIUM);
        }
    }

    /*
     * TODO: at a later stage we should convert this to android.support.v4.app.FragmentStatePagerAdapter
     */
    private class NavAdapter extends FragmentStatePagerAdapter {
        private final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_PAGES);

        NavAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragement no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                // nop
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        @Override
        public Fragment getItem(int position) {
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

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        Fragment getFragment(int position) {
            if (isValidPosition(position)) {
                if (mFragments.get(position) == null) {
                    return getItem(position);
                }
                return mFragments.get(position);
            } else {
                return null;
            }
        }
    }
}
