package org.wordpress.android.ui.posts;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.PostUploadService;
import org.wordpress.android.util.WPMobileStatsUtil;
import org.wordpress.android.util.WPViewPager;

public class NewEditPostActivity extends SherlockFragmentActivity implements ActionBar.TabListener {

    public static String EXTRA_POSTID = "postId";
    public static String EXTRA_IS_PAGE = "isPage";
    public static String EXTRA_IS_NEW_POST = "isNewPost";

    private static final int AUTOSAVE_INTERVAL_MILLIS = 30000;
    private Handler mAutoSaveHandler;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    WPViewPager mViewPager;

    private Post mPost;
    private Post mOriginalPost;

    private EditPostContentFragment mEditPostContentFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;
    private EditPostPreviewFragment mEditPostPreviewFragment;

    private boolean mIsNewPost;

    private String mStatEventEditorClosed = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_edit_post);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Autosave handler
        mAutoSaveHandler = new Handler();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (WPViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                if (position == 2 && mEditPostPreviewFragment != null) {
                    savePost(false);
                    mEditPostPreviewFragment.loadPost(mPost);
                }
            }
        });

        // Add the 3 tabs for the post editor
        actionBar.addTab(
                actionBar.newTab()
                        .setText(mSectionsPagerAdapter.getPageTitle(0))
                        .setTabListener(this)
                        .setIcon(R.drawable.tab_icon_write));
        actionBar.addTab(
                actionBar.newTab()
                        .setText(mSectionsPagerAdapter.getPageTitle(1))
                        .setTabListener(this)
                        .setIcon(R.drawable.tab_icon_settings));
        actionBar.addTab(
                actionBar.newTab()
                        .setText(mSectionsPagerAdapter.getPageTitle(2))
                        .setTabListener(this)
                        .setIcon(R.drawable.tab_icon_preview));

        setTitle(WordPress.getCurrentBlog().getBlogName());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            long postId = extras.getLong(EXTRA_POSTID, -1);
            boolean isPage = extras.getBoolean(EXTRA_IS_PAGE);
            mIsNewPost = extras.getBoolean(EXTRA_IS_NEW_POST);
            try {
                mPost = new Post(WordPress.getCurrentBlog().getId(), postId, isPage);
                if (mPost == null) {
                    showPostErrorAndFinish();
                    return;
                } else {
                    mOriginalPost = new Post(WordPress.getCurrentBlog().getId(), postId, isPage);
                }

                if (isPage) {
                    WPMobileStatsUtil.trackEventForWPCom(WPMobileStatsUtil.StatsEventPageDetailOpenedEditor);
                    mStatEventEditorClosed = WPMobileStatsUtil.StatsEventPageDetailClosedEditor;
                } else {
                    WPMobileStatsUtil.trackEventForWPCom(WPMobileStatsUtil.StatsEventPostDetailOpenedEditor);
                    mStatEventEditorClosed = WPMobileStatsUtil.StatsEventPostDetailClosedEditor;
                }

            } catch (Exception e) {
                showPostErrorAndFinish();
            }
        } else {
            showPostErrorAndFinish();
        }

        // Check for Android share action
        // If it is a share action, create a new post
        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)
                || EditPostContentFragment.NEW_MEDIA_GALLERY.equals(action)
                || EditPostContentFragment.NEW_MEDIA_POST.equals(action)
                || (extras != null && extras.getInt("quick-media", -1) > -1)) {
            mPost = new Post(WordPress.getCurrentBlog().getId(), false);
            if (mPost.getId() < 0) {
                showPostErrorAndFinish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.postDelayed(autoSaveRunnable, AUTOSAVE_INTERVAL_MILLIS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
    }

    @Override
    protected void onDestroy() {
        WPMobileStatsUtil.trackEventForWPComWithSavedProperties(mStatEventEditorClosed);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.edit_post, menu);
        return true;
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_edit_post) {
            savePost(false);
            PostUploadService.addPostToUpload(mPost);
            startService(new Intent(this, PostUploadService.class));
            Intent i = new Intent();
            i.putExtra("shouldRefresh", true);
            setResult(RESULT_OK, i);
            finish();
            return true;
        } else if (itemId == android.R.id.home) {
            showCancelAlert();
            return true;
        }
        return false;
    }

    private void showPostErrorAndFinish() {
        Toast.makeText(this, getResources().getText(R.string.post_not_found), Toast.LENGTH_LONG).show();
        finish();
    }

    private Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            savePost(true);
            mAutoSaveHandler.postDelayed(this, AUTOSAVE_INTERVAL_MILLIS);
        }
    };

    public Post getPost() {
        return mPost;
    }

    private void savePost(boolean isAutosave) {
        if (isAutosave)
            Toast.makeText(this, "AUTOSAVED", Toast.LENGTH_SHORT).show();
        // Update post content from fragment fields
        if (mEditPostContentFragment != null)
            mEditPostContentFragment.savePostContent(isAutosave);
        if (mEditPostSettingsFragment != null)
            mEditPostSettingsFragment.savePostSettings();
    }

    @Override
    public void onBackPressed() {
        if (getSupportActionBar() != null) {
            if (getSupportActionBar().isShowing())
                showCancelAlert();
            else if (mEditPostContentFragment != null)
                mEditPostContentFragment.setContentEditingModeVisible(false);
        }
    }

    private void showCancelAlert() {
        // Empty post? Let's not prompt then.
        if (mEditPostContentFragment != null && mEditPostContentFragment.hasEmptyContentFields()) {
            if (mIsNewPost)
                mPost.delete();
            finish();
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getString((mPost.isPage()) ? R.string.edit_page : R.string.edit_post));
        dialogBuilder.setMessage(getString(R.string.prompt_save_changes));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                savePost(false);
                Intent i = new Intent();
                i.putExtra("shouldRefresh", true);
                setResult(RESULT_OK, i);
                finish();
            }
        });
        dialogBuilder.setNeutralButton(getString(R.string.discard), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // When discard options is chosen, restore existing post or delete new post if it was autosaved.
                if (mOriginalPost != null) {
                    mOriginalPost.update();
                } else if (mPost != null && mIsNewPost) {
                    mPost.delete();
                }
                finish();
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public void setViewPagerEnabled(boolean isEnabled) {
        if (mViewPager != null)
            mViewPager.setPagingEnabled(isEnabled);
    }

    public String getStatEventEditorClosed() {
        return mStatEventEditorClosed;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    mEditPostContentFragment = new EditPostContentFragment();
                    return mEditPostContentFragment;
                case 1:
                    mEditPostSettingsFragment = new EditPostSettingsFragment();
                    return mEditPostSettingsFragment;
                default:
                    mEditPostPreviewFragment = new EditPostPreviewFragment();
                    return mEditPostPreviewFragment;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "WRITE";
                case 1:
                    return getString(R.string.settings).toUpperCase(l);
                case 2:
                    return getString(R.string.preview).toUpperCase(l);
            }
            return null;
        }
    }
}
