package org.wordpress.android.ui;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.SlidingTabLayout;
import org.wordpress.android.widgets.WPMainViewPager;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */

/*
 * TODO: tab icons are currently placeholders for the real icons
 * TODO: handle notifications & reader with no wp.com account
 * TODO: notifications tab needs a badge when their are unseen notes
 */

public class WPMainActivity extends ActionBarActivity
    implements ViewPager.OnPageChangeListener
{
    private WPMainViewPager mViewPager;
    private SlidingTabLayout mTabs;
    private WPMainTabAdapter mTabAdapter;

    private int mPreviousPosition = -1;

    public interface FragmentVisibilityListener {
        public void onVisibilityChanged(boolean isVisible);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.color_status_bar));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (WPMainViewPager) findViewById(R.id.viewpager_main);
        mTabAdapter = new WPMainTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        mTabs = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.tab_indicator));
        mTabs.setDistributeEvenly(true);
        Integer icons[] = {R.drawable.main_tab_sites,
                           R.drawable.main_tab_reader,
                           R.drawable.main_tab_me,
                           R.drawable.main_tab_notifications};
        mTabs.setCustomTabView(R.layout.tab_icon, R.id.tab_icon, R.id.tab_badge, icons);
        mTabs.setViewPager(mViewPager);
        mTabs.setBadge(WPMainTabAdapter.TAB_NOTIFS, true);

        // page change listener must be set on the tab layout rather than the ViewPager
        mTabs.setOnPageChangeListener(this);

        if (savedInstanceState == null) {
            if (showSignInIfRequired()) {
                // return to the tab that was showing the last time
                int position = AppPrefs.getMainTabIndex();
                if (mTabAdapter.isValidPosition(position) && position != mViewPager.getCurrentItem()) {
                    mViewPager.setCurrentItem(position);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    @Override
    public void onPageSelected(int position) {
        // remember the index of this page
        AppPrefs.setMainTabIndex(position);

        // tell the previous fragment that it's being hidden
        Fragment previousFragment = mTabAdapter.getFragment(mPreviousPosition);
        if (previousFragment instanceof FragmentVisibilityListener) {
            ((FragmentVisibilityListener) previousFragment).onVisibilityChanged(false);
        }

        // tell this fragment that it's being shown
        Fragment thisFragment = mTabAdapter.getFragment(position);
        if (thisFragment instanceof FragmentVisibilityListener) {
            ((FragmentVisibilityListener) thisFragment).onVisibilityChanged(true);
        }

        mPreviousPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // nop
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // nop
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.READER_SUBS:
            case RequestCodes.READER_REBLOG:
                ReaderPostListFragment readerFragment = getReaderListFragment();
                if (readerFragment != null) {
                    readerFragment.handleActivityResult(requestCode, resultCode, data);
                }
                break;
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    WordPress.registerForCloudMessaging(this);
                } else {
                    finish();
                }
                break;
            case RequestCodes.REAUTHENTICATE:
                if (resultCode == RESULT_CANCELED) {
                    showSignIn();
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
            case RequestCodes.SETTINGS:
                // user returned from settings
                if (showSignInIfRequired()) {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
        }
    }

   /*
    * displays the sign-in activity if the user isn't logged in - returns true if user
    * is already signed in
    */
    private boolean showSignInIfRequired() {
        if (WordPress.isSignedIn(this)) {
            return true;
        }
        showSignIn();
        return false;
    }

    private void showSignIn() {
        mPreviousPosition = -1;
        startActivityForResult(new Intent(this, SignInActivity.class), RequestCodes.ADD_ACCOUNT);
    }

    /*
     * returns the reader list fragment from the reader tab
     */
    private ReaderPostListFragment getReaderListFragment() {
        Fragment fragment = mTabAdapter.getFragment(WPMainTabAdapter.TAB_READER);
        if (fragment != null && fragment instanceof ReaderPostListFragment) {
            return (ReaderPostListFragment) fragment;
        }
        return null;
    }

    /**
     * broadcast receiver which detects when user signs out of the app and calls onSignout()
     * so descendants of this activity can do cleanup upon signout
     */
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WordPress.BROADCAST_ACTION_SIGNOUT);
        filter.addAction(WordPress.BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH);
        filter.addAction(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS);
        filter.addAction(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE);
        filter.addAction(WordPress.BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT);
        filter.addAction(WordPress.BROADCAST_ACTION_BLOG_LIST_CHANGED);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        try {
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
            lbm.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // exception occurs if receiver already unregistered (safe to ignore)
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            switch (intent.getAction()) {
                case WordPress.BROADCAST_ACTION_SIGNOUT:
                    showSignIn();
                    break;
                case WordPress.BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS:
                    AuthenticationDialogUtils.showAuthErrorDialog(WPMainActivity.this);
                    break;
                case SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED:
                    // TODO: this applies to the notifications tab, should show message there
                    AuthenticationDialogUtils.showAuthErrorDialog(
                            WPMainActivity.this,
                            R.string.sign_in_again,
                            R.string.simperium_connection_error);
                    break;
                case WordPress.BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH:
                    // TODO: add a specific message like "you must use a specific app password"
                    AuthenticationDialogUtils.showAuthErrorDialog(WPMainActivity.this);
                    break;
                case WordPress.BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE:
                    SelfSignedSSLCertsManager.askForSslTrust(WPMainActivity.this, null);
                    break;
                case WordPress.BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT:
                    ToastUtils.showToast(context, R.string.limit_reached, ToastUtils.Duration.LONG);
                    break;
                case WordPress.BROADCAST_ACTION_BLOG_LIST_CHANGED:
                    // TODO: reload blog list if showing
                    break;
            }
        }
    };

}
