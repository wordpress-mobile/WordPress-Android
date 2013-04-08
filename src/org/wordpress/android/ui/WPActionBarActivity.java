
package org.wordpress.android.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsSpinner;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.prefs.PreferencesActivity;
import org.wordpress.android.ui.reader.ReaderActivity;
import org.wordpress.android.util.EscapeUtils;

/**
 * Base class for Activities that include a standard action bar and menu drawer.
 */
public abstract class WPActionBarActivity extends SherlockFragmentActivity {

    private static final String TAG = "WPActionBarActivity";

    /**
     * Request code used when no accounts exist, and user is prompted to add an
     * account.
     */
    static final int ADD_ACCOUNT_REQUEST = 100;
    /**
     * Request code for reloading menu after returning from  the PreferencesActivity.
     */
    static final int SETTINGS_REQUEST = 200;
    
    /**
     * Used to restore active activity on app creation
     */
    protected static final int READER_ACTIVITY = 0;
    protected static final int POSTS_ACTIVITY = 1;
    protected static final int PAGES_ACTIVITY = 2;
    protected static final int COMMENTS_ACTIVITY = 3;
    protected static final int STATS_ACTIVITY = 4;
    protected static final int QUICK_PHOTO_ACTIVITY = 5;
    protected static final int QUICK_VIDEO_ACTIVITY = 6;
    protected static final int VIEW_SITE_ACTIVITY = 7;
    protected static final int DASHBOARD_ACTIVITY = 8;
    protected static final int SETTINGS_ACTIVITY = 9;
    
    protected MenuDrawer mMenuDrawer;
    private static int[] blogIDs;
    protected boolean isAnimatingRefreshButton;
    protected boolean shouldAnimateRefreshButton;
    protected boolean mShouldFinish;
    private boolean mIsDotComBlog;
    private boolean mIsXLargeDevice;
    private boolean mBlogSpinnerInitialized;
    private int mActivePosition;

    private MenuAdapter mAdapter;
    private ListView mListView;
    private IcsSpinner mBlogSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4)
            mIsXLargeDevice = true;    
    }

    @Override
    protected void onPause() {
        super.onPause();

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

        // the current blog may have changed while we were away
        setupCurrentBlog();
        if (mMenuDrawer != null) {
            updateMenuDrawer();
        }

        Blog currentBlog = WordPress.getCurrentBlog();

        if (currentBlog != null && mListView != null && mListView.getHeaderViewsCount() > 0) {
            for (int i = 0; i < blogIDs.length; i++) {
                if (blogIDs[i] == currentBlog.getId()) {
                    mBlogSpinner.setSelection(i);
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentView);

        initMenuDrawer();
    }
    
    /**
     * Attach a menu drawer to the Activity
     * Set to be a static drawer if on a landscape x-large device
     */
    private MenuDrawer attachMenuDrawer() {
        MenuDrawer menuDrawer = null;
        if (mIsXLargeDevice) {
            // on a x-large screen device
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                menuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT, Position.LEFT, true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                menuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        }
        int shadowSizeInPixels = getResources().getDimensionPixelSize(R.dimen.menu_shadow_width);
        menuDrawer.setDropShadowSize(shadowSizeInPixels);
        menuDrawer.setDropShadowColor(getResources().getColor(R.color.md__shadowColor));
        return menuDrawer;
    }
    
    /**
     * Create menu drawer ListView and listeners
     */
    private void initMenuDrawer() {
        mListView = new ListView(this);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setDivider(null);
        mListView.setDividerHeight(0);
        mListView.setCacheColorHint(android.R.color.transparent);
        
        String[] blogNames = getBlogNames();
        if (blogNames.length > 1) {
            addBlogSpinner(blogNames);
        }
        
        mListView.setOnItemClickListener(mItemClickListener);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                mMenuDrawer.invalidate();
            }
        });
        
        mMenuDrawer.setMenuView(mListView);

        updateMenuDrawer();
    }

    private void addBlogSpinner(String[] blogNames) {
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout spinnerWrapper = (LinearLayout) layoutInflater.inflate(R.layout.blog_spinner, null);
        
        spinnerWrapper.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mBlogSpinner != null) {
                    mBlogSpinner.performClick();
                }
            }
        });
        
        mBlogSpinner = (IcsSpinner) spinnerWrapper.findViewById(R.id.blog_spinner);
        mBlogSpinner.setOnItemSelectedListener(mItemSelectedListener);
        SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(getSupportActionBar()
                .getThemedContext(),
                R.layout.sherlock_spinner_dropdown_item, blogNames);
        mBlogSpinner.setAdapter(mSpinnerAdapter);
        mListView.addHeaderView(spinnerWrapper);
    }

    protected void startActivityWithDelay(final Intent i) {

        if (mIsXLargeDevice && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Tablets in landscape don't need a delay because the menu drawer doesn't close
            startActivity(i);
        } else {
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
     * Update all of the items in the menu drawer based on the current active
     * blog.
     */
    protected void updateMenuDrawer() {

        mIsDotComBlog = WordPress.currentBlog != null && WordPress.currentBlog.isDotcomFlag();

        List<Object> items = new ArrayList<Object>();
        Resources resources = getResources();
        if (mIsDotComBlog)
            items.add(new MenuDrawerItem(resources.getString(R.string.reader),
                    R.drawable.dashboard_icon_subs));
        items.add(new MenuDrawerItem(resources.getString(R.string.posts),
                R.drawable.dashboard_icon_posts));
        items.add(new MenuDrawerItem(resources.getString(R.string.pages),
                R.drawable.dashboard_icon_pages));
        items.add(new MenuDrawerItem(resources.getString(R.string.tab_comments),
                R.drawable.dashboard_icon_comments));
        items.add(new MenuDrawerItem(resources.getString(R.string.tab_stats),
                R.drawable.dashboard_icon_stats));
        items.add(new MenuDrawerItem(resources.getString(R.string.quick_photo),
                R.drawable.dashboard_icon_photo));
        items.add(new MenuDrawerItem(resources.getString(R.string.quick_video),
                R.drawable.dashboard_icon_video));
        items.add(new MenuDrawerItem(resources.getString(R.string.view_site),
                R.drawable.dashboard_icon_view));
        items.add(new MenuDrawerItem(resources.getString(R.string.wp_admin),
                R.drawable.dashboard_icon_wp));
        items.add(new MenuDrawerItem(resources.getString(R.string.settings),
                R.drawable.dashboard_icon_settings));

        if ((WPActionBarActivity.this instanceof ReaderActivity))
            mActivePosition = 0;
        
        if ((WPActionBarActivity.this instanceof PostsActivity))
            mActivePosition = 1;

        if ((WPActionBarActivity.this instanceof PagesActivity))
            mActivePosition = 2;
        else if ((WPActionBarActivity.this instanceof CommentsActivity))
            mActivePosition = 3;
        else if ((WPActionBarActivity.this instanceof StatsActivity))
            mActivePosition = 4;
        else if ((WPActionBarActivity.this instanceof ViewSiteActivity))
            mActivePosition = 7;
        else if ((WPActionBarActivity.this instanceof DashboardActivity))
            mActivePosition = 8;
        
        mAdapter = new MenuAdapter(items);
        mListView.setAdapter(mAdapter);
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Adjust position if only one blog is in the app
            if (mListView.getHeaderViewsCount() > 0 && position > 0)
                position--;
            
            if (!mIsDotComBlog)
                position++;
            
            if (position == mActivePosition) {
                // Same row selected
                mMenuDrawer.closeMenu();
                return;
            }
            
            int activityTag = (Integer) view.getTag();

            mActivePosition = position;
            mAdapter.notifyDataSetChanged();
            Intent intent = null;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WPActionBarActivity.this);
            SharedPreferences.Editor editor = settings.edit();
            
            switch (activityTag) {
                case READER_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof ReaderActivity))
                        mShouldFinish = true;
                    int readerBlogID = WordPress.wpDB.getWPCOMBlogID();
                    if
                    (WordPress.currentBlog.isDotcomFlag()) {
                        intent = new Intent(WPActionBarActivity.this, ReaderActivity.class);
                        intent.putExtra("id", readerBlogID);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        editor.putInt("wp_pref_last_activity", READER_ACTIVITY);
                    }
                    break;
                case POSTS_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof PostsActivity)
                            || (WPActionBarActivity.this instanceof PagesActivity))
                        mShouldFinish = true;
                    intent = new
                            Intent(WPActionBarActivity.this, PostsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", POSTS_ACTIVITY);
                    break;
                case PAGES_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof PagesActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, PagesActivity.class);
                    intent.putExtra("id", WordPress.currentBlog.getId());
                    intent.putExtra("isNew",
                            true);
                    intent.putExtra("viewPages", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", PAGES_ACTIVITY);
                    break;
                case COMMENTS_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof CommentsActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, CommentsActivity.class);
                    intent.putExtra("id", WordPress.currentBlog.getId());
                    intent.putExtra("isNew",
                            true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", COMMENTS_ACTIVITY);
                    break;
                case STATS_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof StatsActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, StatsActivity.class);
                    intent.putExtra("id", WordPress.currentBlog.getId());
                    intent.putExtra("isNew",
                            true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", STATS_ACTIVITY);
                    break;
                case QUICK_PHOTO_ACTIVITY:
                    mShouldFinish = false;
                    PackageManager pm = WPActionBarActivity.this.getPackageManager();
                    intent = new Intent(WPActionBarActivity.this, EditPostActivity.class);
                    if
                    (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        intent.putExtra("quick-media", Constants.QUICK_POST_PHOTO_CAMERA);
                    } else {
                        intent.putExtra("quick-media", Constants.QUICK_POST_PHOTO_LIBRARY);
                    }
                    intent.putExtra("isNew", true);
                    break;
                case QUICK_VIDEO_ACTIVITY:
                    mShouldFinish = false;
                    PackageManager vpm = WPActionBarActivity.this.getPackageManager();
                    intent = new Intent(WPActionBarActivity.this, EditPostActivity.class);
                    if (vpm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        intent.putExtra("quick-media", Constants.QUICK_POST_VIDEO_CAMERA);
                    } else {
                        intent.putExtra("quick-media", Constants.QUICK_POST_VIDEO_LIBRARY);
                    }
                    intent.putExtra("isNew", true);
                    break;
                case VIEW_SITE_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof ViewSiteActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, ViewSiteActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", VIEW_SITE_ACTIVITY);
                    break;
                case DASHBOARD_ACTIVITY:
                    if (!(WPActionBarActivity.this instanceof DashboardActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, DashboardActivity.class);
                    intent.putExtra("loadAdmin", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    editor.putInt("wp_pref_last_activity", DASHBOARD_ACTIVITY);
                    break;
                case SETTINGS_ACTIVITY:
                    // Settings shouldn't be launched with a delay, or close the drawer
                    mShouldFinish = false;
                    Intent settingsIntent = new Intent(WPActionBarActivity.this, PreferencesActivity.class);
                    startActivityForResult(settingsIntent, SETTINGS_REQUEST);
                    return;
            }

            editor.commit();
            if (intent != null) {
                mMenuDrawer.closeMenu();
                startActivityWithDelay(intent);
            }
        }
    };
    
    private class MenuAdapter extends BaseAdapter {

        private List<Object> mItems;

        MenuAdapter(List<Object> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof MenuItem ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Object item = getItem(position);

            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.menu_drawer_row, parent, false);
            }

            TextView titleTextView = (TextView) v.findViewById(R.id.menu_row_title);
            titleTextView.setText(((MenuDrawerItem) item).mTitle);

            ImageView iconImageView = (ImageView) v.findViewById(R.id.menu_row_icon);
            iconImageView.setImageResource(((MenuDrawerItem) item).mIconRes);
           
            v.setTag((mIsDotComBlog) ? position : position + 1);

            int positionCheck = mActivePosition;
            if (!mIsDotComBlog)
                positionCheck--;
            if ((position) == positionCheck) {
                // http://stackoverflow.com/questions/5890379/setbackgroundresource-discards-my-xml-layout-attributes
                int bottom = v.getPaddingBottom();
                int top = v.getPaddingTop();
                int right = v.getPaddingRight();
                int left = v.getPaddingLeft();
                v.setBackgroundResource(R.drawable.menu_drawer_selected);
                v.setPadding(left, top, right, bottom);
            } else {
                v.setBackgroundResource(R.drawable.md_list_selector);
            }


            TextView bagdeTextView = (TextView) v.findViewById(R.id.menu_row_badge);
            int commentRow = (mIsDotComBlog) ? 3 : 2; 
            if (position == commentRow && WordPress.currentBlog != null) {
                int commentCount = WordPress.currentBlog.getUnmoderatedCommentCount();
                if (commentCount > 0) {
                    bagdeTextView.setVisibility(View.VISIBLE);
                } else
                {
                    bagdeTextView.setVisibility(View.GONE);
                }
                bagdeTextView.setText(String.valueOf(commentCount));
            } else {
                bagdeTextView.setVisibility(View.GONE);
            }

            return v;
        }
    }

    private static class MenuDrawerItem {

        String mTitle;
        int mIconRes;

        MenuDrawerItem(String title, int iconRes) {
            mTitle = title;
            mIconRes = iconRes;
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
    private static String[] getBlogNames() {
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();

        int blogCount = accounts.size();
        blogIDs = new int[blogCount];
        String[] blogNames = new String[blogCount];

        for (int i = 0; i < blogCount; i++) {
            Map<String, Object> account = accounts.get(i);
            String name;
            if (account.get("blogName") != null) {
                name = EscapeUtils.unescapeHtml(account.get("blogName").toString());
            } else {
                name = account.get("url").toString();
            }
            blogNames[i] = name;
            blogIDs[i] = Integer.valueOf(account.get("id").toString());
        }

        return blogNames;
    }

    /**
     * Setup the global state tracking which blog is currently active.
     * <p>
     * If the global state is not already set, try and determine the last active
     * blog from the last time the application was used. If we're not able to
     * determine the last active blog, just select the first one.
     * <p>
     * If no blogs are configured, display the "new account" activity to allow
     * the user to setup a blog.
     */
    public void setupCurrentBlog() {
        Blog currentBlog = WordPress.getCurrentBlog();

        // no blogs are configured, so display new account activity
        if (currentBlog == null) {
            Log.d(TAG, "No accounts configured.  Sending user to set up an account");
            mShouldFinish = false;
            Intent i = new Intent(this, NewAccountActivity.class);
            startActivityForResult(i, ADD_ACCOUNT_REQUEST);
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ADD_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    // new blog has been added, so rebuild cache of blogs and
                    // setup current blog
                    getBlogNames();
                    setupCurrentBlog();
                    initMenuDrawer();
                    mMenuDrawer.peekDrawer(0);
                } else {
                    finish();
                }
                break;
            case SETTINGS_REQUEST:
                if (resultCode == RESULT_OK) {
                    if (mMenuDrawer != null) {
                        updateMenuDrawer();
                        String[] blogNames = getBlogNames();
                        // If we need to add or remove the blog spinner, init the drawer again
                        if ((blogNames.length > 1 && mListView.getHeaderViewsCount() == 0)
                                || blogNames.length == 1 && mListView.getHeaderViewsCount() > 0)
                            this.initMenuDrawer();
                        else if (blogNames.length > 1 && mBlogSpinner != null) {
                            SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(
                                    getSupportActionBar()
                                            .getThemedContext(),
                                    R.layout.sherlock_spinner_dropdown_item, blogNames);
                            mBlogSpinner.setAdapter(mSpinnerAdapter);
                        }
                        
                        if (blogNames.length >= 1) {
                            setupCurrentBlog();
                            onBlogChanged();
                        }
                    }
                }
                break;
        }
    }
    
    private IcsAdapterView.OnItemSelectedListener mItemSelectedListener = new IcsAdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(IcsAdapterView<?> arg0, View arg1, int position, long arg3) {
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
        public void onNothingSelected(IcsAdapterView<?> arg0) { 
        }
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mMenuDrawer != null) {
                    mMenuDrawer.toggleMenu();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the user changes the active blog.
     */
    public void onBlogChanged() {
        WordPress.wpDB.updateLastBlogId(WordPress.currentBlog.getId());
    }

    public void startAnimatingRefreshButton(MenuItem refreshItem) {
        if (refreshItem != null && !isAnimatingRefreshButton) {
            isAnimatingRefreshButton = true;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(
                    getResources().getLayout(R.layout.menu_refresh_view), null);
            RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setInterpolator(new LinearInterpolator());
            anim.setRepeatCount(Animation.INFINITE);
            anim.setDuration(1400);
            iv.startAnimation(anim);
            refreshItem.setActionView(iv);
        }
    }

    public void stopAnimatingRefreshButton(MenuItem refreshItem) {
        isAnimatingRefreshButton = false;
        if (refreshItem != null && refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
 
        if (mIsXLargeDevice) {
            // Re-attach the drawer if an XLarge device is rotated, so it can be static if in landscape
            View content = mMenuDrawer.getContentContainer().getChildAt(0);
            mMenuDrawer.getContentContainer().removeView(content);
            mMenuDrawer = attachMenuDrawer();
            mMenuDrawer.setContentView(content);
            initMenuDrawer();
        }
 
        super.onConfigurationChanged(newConfig);
    }
}
