
package org.wordpress.android.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.prefs.PreferencesActivity;
import org.wordpress.android.ui.reader.ReaderPostListActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.PullToRefreshHelper;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base class for Activities that include a standard action bar and menu drawer.
 */
public abstract class WPActionBarActivity extends Activity {
    public static final int NEW_BLOG_CANCELED = 10;

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


    protected MenuDrawer mMenuDrawer;
    private static int[] blogIDs;
    protected boolean isAnimatingRefreshButton;
    protected boolean mShouldFinish;
    private boolean mBlogSpinnerInitialized;
    private boolean mReauthCanceled;
    private boolean mNewBlogActivityRunning;

    private MenuAdapter mAdapter;
    protected List<MenuDrawerItem> mMenuItems = new ArrayList<MenuDrawerItem>();
    private ListView mListView;
    private Spinner mBlogSpinner;
    protected boolean mFirstLaunch = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // configure all the available menu items
        mMenuItems.add(new ReaderMenuItem());
        mMenuItems.add(new NotificationsMenuItem());
        mMenuItems.add(new PostsMenuItem());
        mMenuItems.add(new MediaMenuItem());
        mMenuItems.add(new PagesMenuItem());
        mMenuItems.add(new CommentsMenuItem());
        mMenuItems.add(new ThemesMenuItem());
        mMenuItems.add(new StatsMenuItem());
        mMenuItems.add(new QuickPhotoMenuItem());
        mMenuItems.add(new QuickVideoMenuItem());
        mMenuItems.add(new ViewSiteMenuItem());
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
        } else {
            WordPress.shouldRestoreSelectedActivity = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
        refreshMenuDrawer();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected boolean isActivityDestroyed() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed());
    }

    protected void refreshMenuDrawer(){
        // the current blog may have changed while we were away
        setupCurrentBlog();
        if (mMenuDrawer != null) {
            updateMenuDrawer();
        }

        Blog currentBlog = WordPress.getCurrentBlog();

        if (currentBlog != null && mListView != null && mListView.getHeaderViewsCount() > 0) {
            for (int i = 0; i < blogIDs.length; i++) {
                if (blogIDs[i] == currentBlog.getLocalTableBlogId()) {
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
     * @param contentViewID {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(int contentViewID) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentViewID);

        initMenuDrawer();
    }

    /**
     * Create a menu drawer and attach it to the activity.
     *
     * @param contentView {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(View contentView) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentView);

        initMenuDrawer();
    }

    /**
     * returns true if this is an extra-large device in landscape mode
     */
    protected boolean isXLargeLandscape() {
        return isXLarge() && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    protected boolean isXLarge() {
        return ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) ==
                Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    protected boolean isLargeOrXLarge() {
        int mask = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
        return (mask == Configuration.SCREENLAYOUT_SIZE_LARGE
             || mask == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    /**
     * Attach a menu drawer to the Activity
     * Set to be a static drawer if on a landscape x-large device
     */
    private MenuDrawer attachMenuDrawer() {
        final MenuDrawer menuDrawer;
        ActionBar actionBar = getActionBar();

        if (isStaticMenuDrawer()) {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.STATIC, Position.LEFT);
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        } else {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            menuDrawer.setDrawerIndicatorEnabled(true);
        }

        int shadowSizeInPixels = getResources().getDimensionPixelSize(R.dimen.menu_shadow_width);
        menuDrawer.setDropShadowSize(shadowSizeInPixels);
        menuDrawer.setDropShadowColor(getResources().getColor(R.color.md__shadowColor));
        menuDrawer.setSlideDrawable(R.drawable.ic_drawer);
        return menuDrawer;
    }

    public boolean isStaticMenuDrawer() {
        return isXLargeLandscape();
    }

    private void initMenuDrawer() {
        initMenuDrawer(-1);
    }

    /**
     * Create menu drawer ListView and listeners
     */
    private void initMenuDrawer(int blogSelection) {
        if (mMenuDrawer == null) {
            return;
        }
        mListView = new ListView(this);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setDivider(null);
        mListView.setDividerHeight(0);
        mListView.setCacheColorHint(android.R.color.transparent);

        // if the ActionBar overlays window content, we must insert a view which is the same
        // height as the ActionBar as the first header in the ListView - without this the
        // ActionBar will cover the first item
        if (DisplayUtils.hasActionBarOverlay(getWindow())) {
            final int actionbarHeight = DisplayUtils.getActionBarHeight(this);
            RelativeLayout header = new RelativeLayout(this);
            header.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, actionbarHeight));
            mListView.addHeaderView(header, null, false);
        }

        mAdapter = new MenuAdapter(this);
        String[] blogNames = getBlogNames();
        if (blogNames.length > 1) {
            addBlogSpinner(blogNames);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // account for header views
                int menuPosition = position - mListView.getHeaderViewsCount();
                // bail if the adjusted position is out of bounds for the adapter
                if (menuPosition < 0 || menuPosition >= mAdapter.getCount())
                    return;
                MenuDrawerItem item = mAdapter.getItem(menuPosition);
                // if the item has an id, remember it for launch
                if (item.hasItemId()) {
                    ActivityId.trackLastActivity(WPActionBarActivity.this, item.getItemId());
                }
                // only perform selection if the item isn't already selected
                if (!item.isSelected())
                    item.selectItem();
                // save the last activity preference
                // close the menu drawer
                mMenuDrawer.closeMenu();
                // if we have an intent, start the new activity
            }
        });

        mMenuDrawer.setMenuView(mListView);
        mListView.setAdapter(mAdapter);
        if (blogSelection != -1 && mBlogSpinner != null) {
            mBlogSpinner.setSelection(blogSelection);
        }
        updateMenuDrawer();
    }

    private void addBlogSpinner(String[] blogNames) {
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout spinnerWrapper = (LinearLayout) layoutInflater.inflate(R.layout.blog_spinner, null);
        if (spinnerWrapper != null) {
            spinnerWrapper.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBlogSpinner != null) {
                        mBlogSpinner.performClick();
                    }
                }
            });
        }
        mBlogSpinner = (Spinner) spinnerWrapper.findViewById(R.id.blog_spinner);
        mBlogSpinner.setOnItemSelectedListener(mItemSelectedListener);
        populateBlogSpinner(blogNames);
        mListView.addHeaderView(spinnerWrapper);
    }

    /*
     * sets the adapter for the blog spinner and populates it with the passed array of blog names
     */
    private void populateBlogSpinner(String[] blogNames) {
        if (mBlogSpinner == null)
            return;
        mBlogSpinnerInitialized = false;
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            mBlogSpinner.setAdapter(new BlogSpinnerAdapter(actionBar.getThemedContext(), blogNames));
        } else {
            mBlogSpinner.setAdapter(new BlogSpinnerAdapter(this, blogNames));
        }
    }

    /*
     * update the blog names shown by the blog spinner
     */
    protected void refreshBlogSpinner(String[] blogNames) {
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
        private LayoutInflater mInflater;

        BlogSpinnerAdapter(Context context, String[] blogNames) {
            super();
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mBlogNames = blogNames;
        }

        protected void setBlogNames(String[] blogNames) {
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
                view = mInflater.inflate(R.layout.spinner_menu_dropdown_item, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = (TextView) view.findViewById(R.id.menu_text_dropdown);
            text.setText((String)getItem(position));

            return view;
        }
    }

    protected void startActivityWithDelay(final Intent i) {
        if (isXLargeLandscape()) {
            // Tablets in landscape don't need a delay because the menu drawer doesn't close
            startActivity(i);
        } else {
            // When switching to LAST_ACTIVITY_PREFERENCE onCreate we don't need to delay
            if (mFirstLaunch) {
                startActivity(i);
                return;
            }
            // Let the menu animation finish before starting a new activity
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(i);
                }
            }, 400);
        }
    }

    /**
     * Update all of the items in the menu drawer based on the current active blog.
     */
    public void updateMenuDrawer() {
        mAdapter.clear();
        // iterate over the available menu items and only show the ones that should be visible
        Iterator<MenuDrawerItem> availableItems = mMenuItems.iterator();
        while (availableItems.hasNext()) {
            MenuDrawerItem item = availableItems.next();
            if (item.isVisible()) {
                mAdapter.add(item);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public static class MenuAdapter extends ArrayAdapter<MenuDrawerItem> {
        MenuAdapter(Context context) {
            super(context, R.layout.menu_drawer_row, R.id.menu_row_title, new ArrayList<MenuDrawerItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            MenuDrawerItem item = getItem(position);

            TextView titleTextView = (TextView) view.findViewById(R.id.menu_row_title);
            titleTextView.setText(item.getTitleRes());

            ImageView iconImageView = (ImageView) view.findViewById(R.id.menu_row_icon);
            iconImageView.setImageResource(item.getIconRes());
            // Hide the badge always
            view.findViewById(R.id.menu_row_badge).setVisibility(View.GONE);

            if (item.isSelected()) {
                // http://stackoverflow.com/questions/5890379/setbackgroundresource-discards-my-xml-layout-attributes
                int bottom = view.getPaddingBottom();
                int top = view.getPaddingTop();
                int right = view.getPaddingRight();
                int left = view.getPaddingLeft();
                view.setBackgroundResource(R.color.blue_dark);
                view.setPadding(left, top, right, bottom);
            } else {
                view.setBackgroundResource(R.drawable.md_list_selector);
            }
            // allow the menudrawer item to configure the view
            item.configureView(view);

            return view;
        }
    }


    /**
     * Called when the activity has detected the user's press of the back key.
     * If the activity has a menu drawer attached that is opened or in the
     * process of opening, the back button press closes it. Otherwise, the
     * normal back action is taken.
     */
    @Override
    public void onBackPressed() {
        if (mMenuDrawer != null) {
            final int drawerState = mMenuDrawer.getDrawerState();
            if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
                mMenuDrawer.closeMenu();
                return;
            }
        }
        super.onBackPressed();
    }

    /**
     * Get the names of all the blogs configured within the application. If a
     * blog does not have a specific name, the blog URL is returned.
     *
     * @return array of blog names
     */
    protected static String[] getBlogNames() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleAccounts();

        int blogCount = accounts.size();
        blogIDs = new int[blogCount];
        String[] blogNames = new String[blogCount];

        for (int i = 0; i < blogCount; i++) {
            Map<String, Object> account = accounts.get(i);
            blogNames[i] = BlogUtils.getBlogNameFromAccountMap(account);
            blogIDs[i] = Integer.valueOf(account.get("id").toString());
        }

        return blogNames;
    }

    private boolean askToSignInIfNot() {
        if (!WordPress.isSignedIn(WPActionBarActivity.this)) {
            AppLog.d(T.NUX, "No accounts configured.  Sending user to set up an account");
            mShouldFinish = false;
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.putExtra("request", WelcomeActivity.SIGN_IN_REQUEST);
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
        Intent intent;
        intent = new Intent(WPActionBarActivity.this, ReaderPostListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
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
                mNewBlogActivityRunning = false;
                if (resultCode == RESULT_OK) {
                    // new blog has been added, so rebuild cache of blogs and setup current blog
                    getBlogNames();
                    setupCurrentBlog();
                    if (mMenuDrawer != null) {
                        initMenuDrawer();
                        mMenuDrawer.openMenu(false);
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
                if (mMenuDrawer != null && resultCode != PreferencesActivity.RESULT_SIGNED_OUT) {
                    // If we need to add or remove the blog spinner, init the drawer again
                    initMenuDrawer();

                    String[] blogNames = getBlogNames();
                    if (blogNames.length >= 1) {
                        setupCurrentBlog();
                    }
                    if (data != null && data.getBooleanExtra(PreferencesActivity.CURRENT_BLOG_CHANGED, true)) {
                        onBlogChanged();
                    }
                    WordPress.registerForCloudMessaging(this);
                }

                break;
            case AUTHENTICATE_REQUEST:
                if (resultCode == RESULT_CANCELED) {
                    mReauthCanceled = true;
                    Intent i = new Intent(this, WelcomeActivity.class);
                    startActivityForResult(i, ADD_ACCOUNT_REQUEST);
                } else {
                    WordPress.registerForCloudMessaging(this);
                }
                break;
        }
    }

    private OnItemSelectedListener mItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // http://stackoverflow.com/questions/5624825/spinner-onitemselected-executes-when-it-is-not-suppose-to/5918177#5918177
            if (!mBlogSpinnerInitialized) {
                mBlogSpinnerInitialized = true;
            } else {
                WordPress.setCurrentBlog(blogIDs[position]);
                updateMenuDrawer();
                onBlogChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mMenuDrawer != null) {
                mMenuDrawer.toggleMenu();
                return true;
            } else {
                onBackPressed();
            }
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent i = new Intent(this, PreferencesActivity.class);
            startActivityForResult(i, SETTINGS_REQUEST);
        } else if (item.getItemId() == R.id.menu_signout) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getResources().getText(R.string.sign_out));
            dialogBuilder.setMessage(getString(R.string.sign_out_confirm));
            dialogBuilder.setPositiveButton(R.string.sign_out,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            AnalyticsTracker.refreshMetadata();
                            WordPress.signOut(WPActionBarActivity.this);
                            refreshMenuDrawer();
                        }
                    });
            dialogBuilder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            // Just close the window.
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!isFinishing())
                dialogBuilder.create().show();
        } else if (item.getItemId()  == R.id.menu_refresh) {
            // Broadcast a refresh action, PullToRefreshHelper should trigger the default pull to refresh action
            WordPress.sendLocalBroadcast(this, PullToRefreshHelper.BROADCAST_ACTION_REFRESH_MENU_PRESSED);
        }
        return super.onOptionsItemSelected(item);
    }

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
            new ApiHelper.RefreshBlogContentTask(this, WordPress.getCurrentBlog(), callback).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, false);
        }
    }

    /**
     * This method is called when the user changes the active blog or hides all blogs
     */
    public void onBlogChanged() {
        WordPress.wpDB.updateLastBlogId(WordPress.getCurrentLocalTableBlogId());
        // the menu may have changed, we need to change the selection if the selected item
        // is not available in the menu anymore
        Iterator<MenuDrawerItem> itemIterator = mMenuItems.iterator();
        while (itemIterator.hasNext()) {
            MenuDrawerItem item = itemIterator.next();
            // if the item is selected, but it's no longer visible we need to
            // select the first available item from the adapter
            if (item.isSelected() && !item.isVisible()) {
                // then select the first item and activate it
                if (mAdapter.getCount() > 0) {
                    mAdapter.getItem(0).selectItem();
                }
                // if it has an item id save it to the preferences
                if (item.hasItemId()) {
                    ActivityId.trackLastActivity(WPActionBarActivity.this, item.getItemId());
                }
                break;
            }
        }

        refreshCurrentBlogContent();
        if (shouldUpdateCurrentBlogStatsInBackground()) {
            WordPress.sUpdateCurrentBlogStats.forceRun();
        }
    }

    /**
     * this method is called when the user switch blog - descendants should override
     * if want to stop refreshing of Stats when switching blog.
     */
    protected boolean shouldUpdateCurrentBlogStatsInBackground() {
        return true;
    }

    /**
     * this method is called when the user signs out of the app - descendants should override
     * this to perform activity-specific cleanup upon signout
     */
    public void onSignout() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (isXLarge()) {
            if (mMenuDrawer != null) {
                // Re-attach the drawer if an XLarge device is rotated, so it can be static if in landscape
                View content = mMenuDrawer.getContentContainer().getChildAt(0);
                if (content != null) {
                    mMenuDrawer.getContentContainer().removeView(content);
                    mMenuDrawer = attachMenuDrawer();
                    mMenuDrawer.setContentView(content);
                    if (mBlogSpinner != null) {
                        initMenuDrawer(mBlogSpinner.getSelectedItemPosition());
                    } else {
                        initMenuDrawer();
                    }
                }
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    private class ReaderMenuItem extends MenuDrawerItem {
        ReaderMenuItem(){
            super(ActivityId.READER, R.string.reader, R.drawable.dashboard_icon_subs);
        }

        @Override
        public Boolean isVisible(){
            return WordPress.hasValidWPComCredentials(WPActionBarActivity.this);
        }

        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof ReaderPostListActivity;
        }
        @Override
        public void onSelectItem(){
            if (!isSelected())
                mShouldFinish = true;
            Intent intent;
            intent = new Intent(WPActionBarActivity.this, ReaderPostListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
    }

    private class PostsMenuItem extends MenuDrawerItem {
        PostsMenuItem() {
            super(ActivityId.POSTS, R.string.posts, R.drawable.dashboard_icon_posts);
        }

        @Override
        public Boolean isSelected() {
            WPActionBarActivity activity = WPActionBarActivity.this;
            return (activity instanceof PostsActivity) && !(activity instanceof PagesActivity);
        }

        @Override
        public void onSelectItem() {
            if (!(WPActionBarActivity.this instanceof PostsActivity)
                    || (WPActionBarActivity.this instanceof PagesActivity)) {
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_POSTS);
            }
            Intent intent = new Intent(WPActionBarActivity.this, PostsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class MediaMenuItem extends MenuDrawerItem {
        MediaMenuItem(){
            super(ActivityId.MEDIA, R.string.media, R.drawable.dashboard_icon_media);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof MediaBrowserActivity;
        }
        @Override
        public void onSelectItem(){
            if (!(WPActionBarActivity.this instanceof MediaBrowserActivity)) {
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_MEDIA_LIBRARY);
            }
            Intent intent = new Intent(WPActionBarActivity.this, MediaBrowserActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class PagesMenuItem extends MenuDrawerItem {
        PagesMenuItem(){
            super(ActivityId.PAGES, R.string.pages, R.drawable.dashboard_icon_pages);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof PagesActivity;
        }
        @Override
        public void onSelectItem(){
            if (WordPress.getCurrentBlog() == null)
                return;
            if (!(WPActionBarActivity.this instanceof PagesActivity)) {
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_PAGES);
            }
            Intent intent = new Intent(WPActionBarActivity.this, PagesActivity.class);
            intent.putExtra("id", WordPress.getCurrentBlog().getLocalTableBlogId());
            intent.putExtra("isNew", true);
            intent.putExtra(PostsActivity.EXTRA_VIEW_PAGES, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class CommentsMenuItem extends MenuDrawerItem {
        CommentsMenuItem(){
            super(ActivityId.COMMENTS, R.string.tab_comments, R.drawable.dashboard_icon_comments);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof CommentsActivity;
        }
        @Override
        public void onSelectItem(){
            if (WordPress.getCurrentBlog() == null)
                return;
            if (!(WPActionBarActivity.this instanceof CommentsActivity)) {
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_COMMENTS);
            }
            Intent intent = new Intent(WPActionBarActivity.this, CommentsActivity.class);
            intent.putExtra("id", WordPress.getCurrentBlog().getLocalTableBlogId());
            intent.putExtra("isNew", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
        @Override
        public void configureView(View view){
            if (WordPress.getCurrentBlog() != null) {
            TextView bagdeTextView = (TextView) view.findViewById(R.id.menu_row_badge);
                int commentCount = WordPress.getCurrentBlog().getUnmoderatedCommentCount();
                if (commentCount > 0) {
                    bagdeTextView.setVisibility(View.VISIBLE);
                } else
                {
                    bagdeTextView.setVisibility(View.GONE);
                }
                bagdeTextView.setText(String.valueOf(commentCount));
            }
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class ThemesMenuItem extends MenuDrawerItem {
        ThemesMenuItem(){
            super(ActivityId.THEMES, R.string.themes, R.drawable.dashboard_icon_themes);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof ThemeBrowserActivity;
        }
        @Override
        public void onSelectItem(){
            if (!(WPActionBarActivity.this instanceof ThemeBrowserActivity))
                mShouldFinish = true;
            Intent intent = new Intent(WPActionBarActivity.this, ThemeBrowserActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }

        @Override
        public Boolean isVisible() {
            if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isAdmin() && WordPress.getCurrentBlog().isDotcomFlag())
                return true;
            return false;
        }
    }


    private class StatsMenuItem extends MenuDrawerItem {
        StatsMenuItem(){
            super(ActivityId.STATS, R.string.tab_stats, R.drawable.dashboard_icon_stats);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof StatsActivity;
        }
        @Override
        public void onSelectItem(){
            if (WordPress.getCurrentBlog() == null)
                return;
            if (!isSelected())
                mShouldFinish = true;

            Intent intent = new Intent(WPActionBarActivity.this, StatsActivity.class);
            intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, WordPress.getCurrentBlog().getLocalTableBlogId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class QuickPhotoMenuItem extends MenuDrawerItem {
        QuickPhotoMenuItem(){
            super(R.string.quick_photo, R.drawable.dashboard_icon_photo);
        }
        @Override
        public void onSelectItem(){
            mShouldFinish = false;
            Intent intent = new Intent(WPActionBarActivity.this, EditPostActivity.class);
            intent.putExtra("quick-media", DeviceUtils.getInstance().hasCamera(getApplicationContext())
                    ? Constants.QUICK_POST_PHOTO_CAMERA
                    : Constants.QUICK_POST_PHOTO_LIBRARY);
            intent.putExtra("isNew", true);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class QuickVideoMenuItem extends MenuDrawerItem {
        QuickVideoMenuItem(){
            super(R.string.quick_video, R.drawable.dashboard_icon_video);
        }
        @Override
        public void onSelectItem(){
            mShouldFinish = false;
            Intent intent = new Intent(WPActionBarActivity.this, EditPostActivity.class);
            intent.putExtra("quick-media", DeviceUtils.getInstance().hasCamera(getApplicationContext())
                    ? Constants.QUICK_POST_VIDEO_CAMERA
                    : Constants.QUICK_POST_VIDEO_LIBRARY);
            intent.putExtra("isNew", true);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class ViewSiteMenuItem extends MenuDrawerItem {
        ViewSiteMenuItem(){
            super(ActivityId.VIEW_SITE, R.string.view_site, R.drawable.dashboard_icon_view);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof ViewSiteActivity;
        }
        @Override
        public void onSelectItem(){
            if (!(WPActionBarActivity.this instanceof ViewSiteActivity)) {
                mShouldFinish = true;
                AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_VIEW_SITE);
            }
            Intent intent = new Intent(WPActionBarActivity.this, ViewSiteActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
        @Override
        public Boolean isVisible() {
            return WordPress.wpDB.getNumVisibleAccounts() != 0;
        }
    }

    private class NotificationsMenuItem extends MenuDrawerItem {
        NotificationsMenuItem(){
            super(ActivityId.NOTIFICATIONS, R.string.notifications, R.drawable.dashboard_icon_notifications);
        }
        @Override
        public Boolean isVisible(){
            return WordPress.hasValidWPComCredentials(WPActionBarActivity.this);
        }
        @Override
        public Boolean isSelected(){
            return WPActionBarActivity.this instanceof NotificationsActivity;
        }
        @Override
        public void onSelectItem(){
            if (!(WPActionBarActivity.this instanceof NotificationsActivity))
                mShouldFinish = true;
            Intent intent = new Intent(WPActionBarActivity.this, NotificationsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
        }
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
            if (intent == null || intent.getAction() == null)
                return;
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_SIGNOUT)) {
                onSignout();
            }
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_CREDENTIALS)) {
                AuthenticationDialogUtils.showAuthErrorDialog(WPActionBarActivity.this);
            }
            if (intent.getAction().equals(SimperiumUtils.BROADCAST_ACTION_SIMPERIUM_NOT_AUTHORIZED)
                    && WPActionBarActivity.this instanceof NotificationsActivity) {
                AuthenticationDialogUtils.showAuthErrorDialog(WPActionBarActivity.this, R.string.sign_in_again,
                        R.string.simperium_connection_error);
            }
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_TWO_FA_AUTH)) {
                // TODO: add a specific message like "you must use a specific app password"
                AuthenticationDialogUtils.showAuthErrorDialog(WPActionBarActivity.this);
            }
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_INVALID_SSL_CERTIFICATE)) {
                SelfSignedSSLCertsManager.askForSslTrust(WPActionBarActivity.this);
            }
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_XMLRPC_LOGIN_LIMIT)) {
                ToastUtils.showToast(context, R.string.limit_reached, Duration.LONG);
            }
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_BLOG_LIST_CHANGED)) {
                initMenuDrawer();
            }
        }
    };
}
