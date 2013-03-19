
package org.wordpress.android.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import net.simonvt.menudrawer.MenuDrawer;

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
import org.wordpress.android.ui.reader.ReaderPagerActivity;
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

    protected MenuDrawer mMenuDrawer;
    private static int[] blogIDs;
    protected boolean isAnimatingRefreshButton;
    protected boolean mShouldFinish;
    private boolean mIsDotComBlog;
    private int mActivePosition;

    private MenuAdapter mAdapter;
    private ListView mListView;
    private Spinner mBlogSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this instanceof WebViewActivity)
            requestWindowFeature(Window.FEATURE_PROGRESS);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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

        if (currentBlog != null && mListView.getHeaderViewsCount() > 0) {
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

        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
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
        
        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        mMenuDrawer.setContentView(contentView);

        initMenuDrawer();
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
        mBlogSpinner = (Spinner) layoutInflater.inflate(R.layout.blog_spinner, null);
        mBlogSpinner.setOnItemSelectedListener(mItemSelectedListener);
        SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(getSupportActionBar()
                .getThemedContext(),
                R.layout.sherlock_spinner_dropdown_item, blogNames);
        mBlogSpinner.setAdapter(mSpinnerAdapter);
        mListView.addHeaderView(mBlogSpinner);  
    }

    protected void startActivityWithDelay(final Intent i) {
        // Let the menu animation finish before starting a new activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(i);
            }
        }, 400);
    }

    /**
     * Update all of the items in the menu drawer based on the current active
     * blog.
     */
    protected void updateMenuDrawer() {

        mIsDotComBlog = WordPress.currentBlog != null && WordPress.currentBlog.isDotcomFlag();

        List<Object> items = new ArrayList<Object>();
        Resources resources = getResources();
        items.add(new MenuDrawerItem(resources.getString(R.string.posts),
                R.drawable.dashboard_icon_posts));
        items.add(new MenuDrawerItem(resources.getString(R.string.pages),
                R.drawable.dashboard_icon_pages));
        items.add(new MenuDrawerItem(resources.getString(R.string.tab_comments),
                R.drawable.dashboard_icon_comments));
        items.add(new MenuDrawerItem(resources.getString(R.string.tab_stats),
                R.drawable.dashboard_icon_stats));
        if (mIsDotComBlog)
            items.add(new MenuDrawerItem(resources.getString(R.string.reader),
                    R.drawable.dashboard_icon_subs));
        items.add(new MenuDrawerItem(resources.getString(R.string.quick_photo),
                R.drawable.dashboard_icon_photo));
        items.add(new MenuDrawerItem(resources.getString(R.string.quick_video),
                R.drawable.dashboard_icon_video));
        items.add(new MenuDrawerItem(resources.getString(R.string.view_site),
                R.drawable.preview_icon));
        items.add(new MenuDrawerItem(resources.getString(R.string.wp_admin),
                R.drawable.dashboard_icon_wp));
        items.add(new MenuDrawerItem(resources.getString(R.string.settings),
                R.drawable.dashboard_icon_settings));

        if ((WPActionBarActivity.this instanceof PostsActivity))
            mActivePosition = 1;

        if ((WPActionBarActivity.this instanceof PagesActivity))
            mActivePosition = 2;
        else if ((WPActionBarActivity.this instanceof CommentsActivity))
            mActivePosition = 3;
        else if ((WPActionBarActivity.this instanceof ViewWebStatsActivity))
            mActivePosition = 4;
        else if ((WPActionBarActivity.this instanceof ReaderPagerActivity))
            mActivePosition = 5;
        else if ((WPActionBarActivity.this instanceof ViewSiteActivity))
            mActivePosition = 8;
        else if ((WPActionBarActivity.this instanceof DashboardActivity))
            mActivePosition = 9;
            
        if (!mIsDotComBlog && mActivePosition > 4)
            mActivePosition--;
        
        mAdapter = new MenuAdapter(items);
        mListView.setAdapter(mAdapter);
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Adjust position if only one blog is in the app
            if (mListView.getHeaderViewsCount() == 0)
                position++;
            // Adjust position if blog isn't on WP.com (No Reader)
            if (!mIsDotComBlog && position > 4)
                position++;
            
            if (position == mActivePosition) {
                // Same row selected
                mMenuDrawer.closeMenu();
                return;
            }

            mActivePosition = position;
            mAdapter.notifyDataSetChanged();
            Intent intent = null;

            switch (position) {
                case 1:
                    if (!(WPActionBarActivity.this instanceof PostsActivity)
                            || (WPActionBarActivity.this instanceof PagesActivity))
                        mShouldFinish = true;
                    intent = new
                            Intent(WPActionBarActivity.this, PostsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case 2:
                    if (!(WPActionBarActivity.this instanceof PagesActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, PagesActivity.class);
                    intent.putExtra("id", WordPress.currentBlog.getId());
                    intent.putExtra("isNew",
                            true);
                    intent.putExtra("viewPages", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case 3:
                    if (!(WPActionBarActivity.this instanceof CommentsActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, CommentsActivity.class);
                    intent.putExtra("id", WordPress.currentBlog.getId());
                    intent.putExtra("isNew",
                            true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case 4:
                    if (!(WPActionBarActivity.this instanceof ViewWebStatsActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, ViewWebStatsActivity.class);
                    intent.putExtra("id", WordPress.currentBlog.getId());
                    intent.putExtra("isNew",
                            true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case 5:
                    if (!(WPActionBarActivity.this instanceof ReaderPagerActivity))
                        mShouldFinish = true;
                    int readerBlogID = WordPress.wpDB.getWPCOMBlogID();
                    if
                    (WordPress.currentBlog.isDotcomFlag()) {
                        intent = new Intent(WPActionBarActivity.this, ReaderPagerActivity.class);
                        intent.putExtra("id", readerBlogID);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    }
                    break;
                case 6:
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
                case 7:
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
                case 8:
                    if (!(WPActionBarActivity.this instanceof ViewSiteActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, ViewSiteActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case 9:
                    if (!(WPActionBarActivity.this instanceof DashboardActivity))
                        mShouldFinish = true;
                    intent = new Intent(WPActionBarActivity.this, DashboardActivity.class);
                    intent.putExtra("loadAdmin", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    break;
                case 10:
                    // Settings shouldn't be launched with a delay, or close the drawer
                    mShouldFinish = false;
                    Intent settingsIntent = new Intent(WPActionBarActivity.this, PreferencesActivity.class);
                    startActivityForResult(settingsIntent, SETTINGS_REQUEST);
                    return;
            }

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

            v.setTag(R.id.mdActiveViewPosition, position);

            if ((position + 1) == mActivePosition) {
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
            if (position == 2 && WordPress.currentBlog != null) {
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
                } else {
                    finish();
                }
                break;
            case SETTINGS_REQUEST:
                if (resultCode == RESULT_OK) {
                    setupCurrentBlog();
                    if (mMenuDrawer != null) {
                        updateMenuDrawer();
                        String[] blogNames = getBlogNames();
                        // If we need to add or remove the blog spinner, init the drawer again
                        if ((blogNames.length > 1 && mListView.getHeaderViewsCount() == 0) || blogNames.length == 1 && mListView.getHeaderViewsCount() > 0)
                            this.initMenuDrawer();
                    }
                }
                break;
        }
    }
    
    private AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
            WordPress.setCurrentBlog(blogIDs[position]);
            updateMenuDrawer();
            onBlogChanged();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
            
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
}
