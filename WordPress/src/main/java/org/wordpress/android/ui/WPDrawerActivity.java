
package org.wordpress.android.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.DrawerItems.DrawerItem;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.prefs.SettingsActivity;
import org.wordpress.android.ui.reader.ReaderPostListActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPActivityUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import java.util.List;
import java.util.Map;

/**
 * Base class for Activities that include a standard action bar and menu drawer.
 */
public abstract class WPDrawerActivity extends ActionBarActivity {
    public static final int NEW_BLOG_CANCELED = 10;
    private static final String SCROLL_POSITION_ID = "WPDrawerActivity";
    /**
     * AuthenticatorRequest code used when no accounts exist, and user is prompted to add an
     * account.
     */
    private static final int ADD_ACCOUNT_REQUEST = 100;
    /**
     * AuthenticatorRequest code for reloading menu after returning from  the PreferencesActivity.
     */
    private static final int SETTINGS_REQUEST = 200;
    /**
     * AuthenticatorRequest code for re-authentication
     */
    private static final int AUTHENTICATE_REQUEST = 300;

    private static int[] mBlogIDs;
    private boolean isAnimatingRefreshButton;
    private boolean mShouldFinish;
    private boolean mBlogSpinnerInitialized;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;
    private ListView mDrawerListView;
    private Spinner mBlogSpinner;
    private ListScrollPositionManager mScrollPositionManager;

    private static final int OPENED_FROM_DRAWER_DELAY = 250;

    @Override
    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean menuDrawerDisabled = false;
        if (getIntent() != null) {
            menuDrawerDisabled = getIntent().getBooleanExtra(StatsActivity.ARG_NO_MENU_DRAWER, false);
        }
        if (isStaticMenuDrawer() && !menuDrawerDisabled) {
            setContentView(R.layout.activity_drawer_static);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.color_primary_dark));
            }
        } else {
            setContentView(R.layout.activity_drawer);
        }

        setSupportActionBar(getToolbar());
    }

    /*
     * fade out the view containing the current drawer activity
     */
    private void hideActivityView() {
        // activity_container is the parent view which contains the toolbar (first child) and
        // the activity itself (second child)
        ViewGroup container = (ViewGroup) findViewById(R.id.activity_container);
        if (container == null || container.getChildCount() < 2) {
            return;
        }
        final View activityView = container.getChildAt(1);
        if (activityView == null || activityView.getVisibility() != View.VISIBLE) {
            return;
        }
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(activityView, View.ALPHA, 1.0f, 0.0f);
        fadeOut.setDuration(OPENED_FROM_DRAWER_DELAY);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();

        if (isAnimatingRefreshButton) {
            isAnimatingRefreshButton = false;
        }
        if (mShouldFinish) {
            overridePendingTransition(0, 0);
            finish();
        }
        if (mScrollPositionManager != null) {
            mScrollPositionManager.saveToPreferences(this, SCROLL_POSITION_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
        refreshMenuDrawer();
        if (mDrawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
        if (mScrollPositionManager != null) {
            mScrollPositionManager.restoreFromPreferences(this, SCROLL_POSITION_ID);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected boolean isActivityDestroyed() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed());
    }

    void refreshMenuDrawer() {
        if (mDrawerAdapter == null) return;
        // the current blog may have changed while we were away
        setupCurrentBlog();
        updateMenuDrawer();

        Blog currentBlog = WordPress.getCurrentBlog();

        if (currentBlog != null && mDrawerListView != null && mDrawerListView.getHeaderViewsCount() > 0) {
            for (int i = 0; i < mBlogIDs.length; i++) {
                if (mBlogIDs[i] == currentBlog.getLocalTableBlogId()) {
                    if (mBlogSpinner != null) {
                        mBlogSpinner.setSelection(i);
                    }
                }
            }
        }
    }

    /**
     * Create a menu drawer and attach it to the activity.
     *
     * @param contentViewId {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(int contentViewId) {
        ViewGroup container = (ViewGroup) findViewById(R.id.activity_container);
        container.addView(getLayoutInflater().inflate(contentViewId, null));
        initMenuDrawer();
    }

    protected Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.toolbar);
        }

        return mToolbar;
    }

    public boolean isStaticMenuDrawer() {
        return DisplayUtils.isLandscape(this)
            && DisplayUtils.isXLarge(this);
    }

    /**
     * Create menu drawer ListView and listeners
     */
    private void initMenuDrawer() {
        // locate the drawer layout - note that it will not exist on landscape tablets
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            int drawerWidth =
                    isStaticMenuDrawer() ?
                            getResources().getDimensionPixelSize(R.dimen.drawer_width_static) :
                            WPActivityUtils.getOptimalDrawerWidth(this);
            ViewGroup leftDrawer = (ViewGroup) mDrawerLayout.findViewById(R.id.capture_insets_frame_layout);
            leftDrawer.getLayoutParams().width = drawerWidth;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.color_primary_dark));
            }
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mDrawerToggle = new ActionBarDrawerToggle(
                    this,
                    mDrawerLayout,
                    getToolbar(),
                    R.string.open_drawer,
                    R.string.close_drawer
            ) {
                public void onDrawerClosed(View view) {
                    invalidateOptionsMenu();
                }
                public void onDrawerOpened(View drawerView) {
                    invalidateOptionsMenu();
                }
            };
            mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        // add listVew header containing spinner if it hasn't already been added - note that
        // initBlogSpinner() will setup the spinner
        mDrawerListView = (ListView) findViewById(R.id.drawer_list);
        if (mDrawerListView.getHeaderViewsCount() == 0) {
            View view = getLayoutInflater().inflate(R.layout.drawer_header, mDrawerListView, false);
            mDrawerListView.addHeaderView(view, null, false);
        }
        mScrollPositionManager = new ListScrollPositionManager(mDrawerListView, false);
        View settingsRow = findViewById(R.id.settings_row);
        settingsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });

        mDrawerAdapter = new DrawerAdapter(this);
        mDrawerListView.setAdapter(mDrawerAdapter);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int menuPosition = position - mDrawerListView.getHeaderViewsCount();
                DrawerItem item = (DrawerItem) mDrawerAdapter.getItem(menuPosition);
                drawerItemSelected(item);
            }
        });

        initBlogSpinner();
        updateMenuDrawer();

        setToolbarClickListener();
    }

    protected void setToolbarClickListener() {
        // Set navigation listener, which ensures menu button works on all devices (#2157)
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFinishing()) return;

                FragmentManager fm = getFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else if (isStaticMenuDrawer()) {
                    finish();
                } else {
                    toggleDrawer();
                }
            }
        });
    }

    /*
     * setup the spinner in the drawer header which shows a list of the user's blogs
     */
    private void initBlogSpinner() {
        TextView txtBlogName = (TextView) findViewById(R.id.text_header_blog_name);
        mBlogSpinner = (Spinner) findViewById(R.id.blog_spinner);
        String[] blogNames = getBlogNames();
        if (blogNames.length > 1) {
            // more than one blog so show spinner enabling user to choose
            txtBlogName.setVisibility(View.GONE);
            mBlogSpinner.setVisibility(View.VISIBLE);
            mBlogSpinner.setOnItemSelectedListener(mItemSelectedListener);
            populateBlogSpinner(blogNames);
        } else if (blogNames.length == 1) {
            // only one blog so hide spinner and show name of blog
            txtBlogName.setVisibility(View.VISIBLE);
            txtBlogName.setText(blogNames[0]);
            mBlogSpinner.setVisibility(View.GONE);
            mBlogSpinner.setOnItemSelectedListener(null);
        } else {
            // no blogs so hide spinner and blog name
            txtBlogName.setVisibility(View.GONE);
            mBlogSpinner.setVisibility(View.GONE);
            mBlogSpinner.setOnItemSelectedListener(null);
        }
    }

    void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    protected void hideDrawer() {
        View drawer = findViewById(R.id.left_drawer);
        if (drawer != null) {
            drawer.setVisibility(View.GONE);
        }
    }

    private void toggleDrawer() {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                closeDrawer();
            } else {
                openDrawer();
            }
        }
    }

    /**
     * called when user selects an item from the drawer
     */
    private void drawerItemSelected(DrawerItem item) {
        // do nothing if item is already selected
        if (item == null || item.isSelected(this)) {
            closeDrawer();
            return;
        }

        // if the item has an activity id, remember it for launch
        ActivityId activityId = item.getDrawerItemId().toActivityId();
        if (activityId != ActivityId.UNKNOWN) {
            ActivityId.trackLastActivity(activityId);
        }

        final Intent intent;
        switch (item.getDrawerItemId()) {
            case READER:
                mShouldFinish = true;
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case NOTIFICATIONS:
                mShouldFinish = true;
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case POSTS:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_POSTS);
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case MEDIA:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY);
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case PAGES:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_PAGES);
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case COMMENTS:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_COMMENTS);
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case THEMES:
                mShouldFinish = true;
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case STATS:
                mShouldFinish = true;
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case VIEW_SITE:
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_VIEW_SITE);
                intent = WPActivityUtils.getIntentForActivityId(this, activityId);
                break;
            case QUICK_PHOTO:
                mShouldFinish = false;
                intent = new Intent(WPDrawerActivity.this, EditPostActivity.class);
                intent.putExtra("quick-media", DeviceUtils.getInstance().hasCamera(getApplicationContext())
                        ? Constants.QUICK_POST_PHOTO_CAMERA
                        : Constants.QUICK_POST_PHOTO_LIBRARY);
                break;
            default :
                mShouldFinish = false;
                intent = null;
                break;
        }

        if (intent == null) {
            ToastUtils.showToast(this, R.string.reader_toast_err_generic);
            return;
        }

        if (mShouldFinish) {
            // set the ActionBar title to that of the incoming activity - left blank for the
            // reader since it shows a spinner in the toolbar
            if (getSupportActionBar() != null) {
                int titleResId = item.getTitleResId();
                if (titleResId != 0 && activityId != ActivityId.READER) {
                    getSupportActionBar().setTitle(getString(titleResId));
                } else {
                    getSupportActionBar().setTitle(null);
                }
            }

            // close the drawer and fade out the activity container so current activity appears to be going away
            closeDrawer();
            hideActivityView();

            // start the new activity after a brief delay to give drawer time to close
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
            }, OPENED_FROM_DRAWER_DELAY);
        } else {
            // current activity isn't being finished, so just start the new activity
            closeDrawer();
            startActivity(intent);
        }
    }

    protected ActionBarDrawerToggle getDrawerToggle() {
        return mDrawerToggle;
    }

    /*
     * sets the adapter for the blog spinner and populates it with the passed array of blog names
     */
    private void populateBlogSpinner(String[] blogNames) {
        if (mBlogSpinner == null) {
            return;
        }
        mBlogSpinnerInitialized = false;
        Context context = WPActivityUtils.getThemedContext(this);
        mBlogSpinner.setAdapter(new BlogSpinnerAdapter(context, blogNames));
    }

    /*
     * update the blog names shown by the blog spinner
     */
    void refreshBlogSpinner(String[] blogNames) {
        // spinner will be null if it's not supposed to be shown
        if (mBlogSpinner == null || mBlogSpinner.getAdapter() == null) {
            return;
        }

        ((BlogSpinnerAdapter) mBlogSpinner.getAdapter()).setBlogNames(blogNames);
    }

    /*
     * adapter used by the blog spinner - shows the name of each blog
     */
    private class BlogSpinnerAdapter extends BaseAdapter {
        private String[] mBlogNames;
        private final LayoutInflater mInflater;

        BlogSpinnerAdapter(Context context, String[] blogNames) {
            super();
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mBlogNames = blogNames;
        }

        void setBlogNames(String[] blogNames) {
            mBlogNames = blogNames;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return (mBlogNames != null ? mBlogNames.length : 0);
        }

        @Override
        public Object getItem(int position) {
            if (position < 0 || position >= getCount())
                return "";
            return mBlogNames[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.spinner_textview_drawer, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setText((String) getItem(position));

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.spinner_menu_dropdown_item, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = (TextView) view.findViewById(R.id.menu_text_dropdown);
            text.setText((String) getItem(position));

            return view;
        }
    }

    /**
     * Update all of the items in the menu drawer based on the current active blog.
     */
    public void updateMenuDrawer() {
        mDrawerAdapter.refresh();
    }

    /**
     * Called when the activity has detected the user's press of the back key.
     * If the activity has a menu drawer attached that is opened or in the
     * process of opening, the back button press closes it. Otherwise, the
     * normal back action is taken.
     */
    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Get the names of all the blogs configured within the application. If a
     * blog does not have a specific name, the blog URL is returned.
     *
     * @return array of blog names
     */
    private static String[] getBlogNames() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleAccounts();

        int blogCount = accounts.size();
        mBlogIDs = new int[blogCount];
        String[] blogNames = new String[blogCount];

        for (int i = 0; i < blogCount; i++) {
            Map<String, Object> account = accounts.get(i);
            blogNames[i] = BlogUtils.getBlogNameOrHostNameFromAccountMap(account);
            mBlogIDs[i] = Integer.valueOf(account.get("id").toString());
        }

        return blogNames;
    }

    private boolean askToSignInIfNot() {
        if (!WordPress.isSignedIn(WPDrawerActivity.this)) {
            AppLog.d(T.NUX, "No accounts configured.  Sending user to set up an account");
            mShouldFinish = false;
            Intent intent = new Intent(this, SignInActivity.class);
            intent.putExtra("request", SignInActivity.SIGN_IN_REQUEST);
            startActivityForResult(intent, ADD_ACCOUNT_REQUEST);
            return false;
        }
        return true;
    }

    /**
     * Setup the global state tracking which blog is currently active if the user is signed in.
     */
    public void setupCurrentBlog() {
        if (askToSignInIfNot()) {
            WordPress.getCurrentBlog();
        }
    }

    private void showReader() {
        Intent intent = new Intent(WPDrawerActivity.this, ReaderPostListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void showSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_REQUEST);
    }

    /*
     * redirect to the Reader if there aren't any visible blogs
     * returns true if redirected, false otherwise
     */
    protected boolean showReaderIfNoBlog() {
        if (WordPress.wpDB.getNumVisibleAccounts() == 0) {
            showReader();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADD_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    // new blog has been added, so rebuild cache of blogs and setup current blog
                    getBlogNames();
                    setupCurrentBlog();
                    if (mDrawerListView != null) {
                        initBlogSpinner();
                    }
                    WordPress.registerForCloudMessaging(this);
                    // If logged in without blog, redirect to the Reader view
                    showReaderIfNoBlog();
                } else {
                    finish();
                }
                break;
            case SETTINGS_REQUEST:
                // user returned from settings - skip if user signed out
                if (mDrawerListView != null && resultCode != SettingsActivity.RESULT_SIGNED_OUT) {
                    // If we need to add or remove the blog spinner, init the drawer again
                    initBlogSpinner();

                    String[] blogNames = getBlogNames();
                    if (blogNames.length >= 1) {
                        setupCurrentBlog();
                    }
                    if (data != null && data.getBooleanExtra(SettingsActivity.CURRENT_BLOG_CHANGED, true)) {
                        blogChanged();
                    }
                    WordPress.registerForCloudMessaging(this);
                }

                break;
            case AUTHENTICATE_REQUEST:
                if (resultCode == RESULT_CANCELED) {
                    Intent i = new Intent(this, SignInActivity.class);
                    startActivityForResult(i, ADD_ACCOUNT_REQUEST);
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
        }
    }

    private final OnItemSelectedListener mItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // http://stackoverflow.com/questions/5624825/spinner-onitemselected-executes-when-it-is-not-suppose-to/5918177#5918177
            if (!mBlogSpinnerInitialized) {
                mBlogSpinnerInitialized = true;
            } else {
                WordPress.setCurrentBlog(mBlogIDs[position]);
                updateMenuDrawer();
                blogChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private void refreshCurrentBlogContent() {
        if (WordPress.getCurrentBlog() != null) {
            ApiHelper.GenericCallback callback = new ApiHelper.GenericCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing()) {
                        return;
                    }
                    // refresh spinner in case a blog's name has changed
                    refreshBlogSpinner(getBlogNames());
                    updateMenuDrawer();
                }

                @Override
                public void onFailure(ErrorType errorType, String errorMessage, Throwable throwable) {
                }
            };
            new ApiHelper.RefreshBlogContentTask(WordPress.getCurrentBlog(), callback).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, false);
        }
    }

    /**
     * This method is called when the user changes the active blog or hides all blogs
     */
    private void blogChanged() {
        WordPress.wpDB.updateLastBlogId(WordPress.getCurrentLocalTableBlogId());

        // the list of items in the drawer may have changed, so check if there's no longer any
        // selected item and if so select the first one
        if (mDrawerAdapter != null && !mDrawerAdapter.hasSelectedItem(this) && mDrawerAdapter.getCount() > 0) {
            DrawerItem drawerItem = (DrawerItem) mDrawerAdapter.getItem(0);
            drawerItemSelected(drawerItem);
        }

        refreshCurrentBlogContent();

        if (WordPress.getCurrentBlog() != null) {
            onBlogChanged();
        }

    }

    /**
     * This method is called in when the user changes the active blog - descendants should override
     * this to perform activity-specific updates upon blog change
     */
    protected void onBlogChanged() {
    }

    /**
     * this method is called when the user signs out of the app - descendants should override
     * this to perform activity-specific cleanup upon signout
     */
    public void onSignout() {
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
        filter.addAction(WordPress.BROADCAST_ACTION_REST_API_UNAUTHORIZED);
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
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_SIGNOUT)) {
                onSignout();
                return;
            }

            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS)
                    || intent.getAction().equals(WordPress.BROADCAST_ACTION_REST_API_UNAUTHORIZED)
                    || intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH)) {
                AuthenticationDialogUtils.showAuthErrorView(WPDrawerActivity.this);
                return;
            }

            if (intent.getAction().equals(SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED)
                    && WPDrawerActivity.this instanceof NotificationsActivity) {
                AuthenticationDialogUtils.showAuthErrorView(WPDrawerActivity.this, R.string.sign_in_again,
                        R.string.simperium_connection_error);
                return;
            }

            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE)) {
                SelfSignedSSLCertsManager.askForSslTrust(WPDrawerActivity.this, null);
                return;
            }

            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT)) {
                ToastUtils.showToast(context, R.string.limit_reached, Duration.LONG);
                return;
            }

            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_BLOG_LIST_CHANGED)) {
                initBlogSpinner();
            }
        }
    };
}
