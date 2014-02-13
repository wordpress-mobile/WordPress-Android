package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.PostUploadService;
import org.wordpress.android.util.WPMobileStatsUtil;
import org.wordpress.android.util.WPViewPager;

public class EditPostActivity extends SherlockFragmentActivity {

    public static final String EXTRA_POSTID = "postId";
    public static final String EXTRA_IS_PAGE = "isPage";
    public static final String EXTRA_IS_NEW_POST = "isNewPost";
    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_QUICKPRESS_BLOG_ID = "quickPressBlogId";
    public static final String STATE_KEY_CURRENT_POST = "stateKeyCurrentPost";
    public static final String STATE_KEY_ORIGINAL_POST = "stateKeyOriginalPost";

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;

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
        actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        String action = getIntent().getAction();
        if (savedInstanceState == null) {
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)
                    || EditPostContentFragment.NEW_MEDIA_GALLERY.equals(action)
                    || EditPostContentFragment.NEW_MEDIA_POST.equals(action)
                    || getIntent().hasExtra(EXTRA_IS_QUICKPRESS)
                    || (extras != null && extras.getInt("quick-media", -1) > -1)) {

                if (getIntent().hasExtra(EXTRA_QUICKPRESS_BLOG_ID)) {
                    // QuickPress might want to use a different blog than the current blog
                    int blogId = getIntent().getIntExtra(EXTRA_QUICKPRESS_BLOG_ID, -1);
                    try {
                        Blog quickPressBlog = new Blog(blogId);
                        if (quickPressBlog.isHidden()) {
                            // Don't continue if blog is hidden
                            showErrorAndFinish(R.string.error_blog_hidden);
                            return;
                        }
                        WordPress.currentBlog = quickPressBlog;
                    } catch (Exception e) {
                        // QuickPress Blog not found
                        showErrorAndFinish(R.string.blog_not_found);
                        return;
                    }
                }

                // Create a new post for share intents and QuickPress
                mPost = new Post(WordPress.getCurrentLocalTableBlogId(), false);
                mIsNewPost = true;
            } else if (extras != null) {
                // Load post from the postId passed in extras
                long postId = extras.getLong(EXTRA_POSTID, -1);
                boolean isPage = extras.getBoolean(EXTRA_IS_PAGE);
                mIsNewPost = extras.getBoolean(EXTRA_IS_NEW_POST);
                mPost = new Post(WordPress.getCurrentLocalTableBlogId(), postId, isPage);
                mOriginalPost = new Post(WordPress.getCurrentLocalTableBlogId(), postId, isPage);

                if (isPage) {
                    WPMobileStatsUtil.trackEventForWPCom(WPMobileStatsUtil.StatsEventPageDetailOpenedEditor);
                    mStatEventEditorClosed = WPMobileStatsUtil.StatsEventPageDetailClosedEditor;
                } else {
                    WPMobileStatsUtil.trackEventForWPCom(WPMobileStatsUtil.StatsEventPostDetailOpenedEditor);
                    mStatEventEditorClosed = WPMobileStatsUtil.StatsEventPostDetailClosedEditor;
                }
            } else {
                // A postId extra must be passed to this activity
                showErrorAndFinish(R.string.post_not_found);
                return;
            }
        } else if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_POST)) {
            try {
                mPost = (Post) savedInstanceState.getSerializable(STATE_KEY_CURRENT_POST);
                mOriginalPost = (Post) savedInstanceState.getSerializable(STATE_KEY_ORIGINAL_POST);
            } catch (ClassCastException e) {
                mPost = null;
            }
        }

        // Ensure we have a valid blog
        if (WordPress.getCurrentBlog() == null) {
            showErrorAndFinish(R.string.blog_not_found);
            return;
        }

        // Ensure we have a valid post
        if (mPost == null || mPost.getId() < 0) {
            showErrorAndFinish(R.string.post_not_found);
            return;
        }

        setTitle(WordPress.getCurrentBlog().getBlogName());

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (WPViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPagingEnabled(false);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                supportInvalidateOptionsMenu();
                if (position == PAGE_CONTENT) {
                    setTitle(WordPress.getCurrentBlog().getBlogName());
                } else if (position == PAGE_SETTINGS) {
                    setTitle(mPost.isPage() ? R.string.page_settings : R.string.post_settings);
                } else if (position == PAGE_PREVIEW) {
                    setTitle(mPost.isPage() ? R.string.preview_page : R.string.preview_post);
                    savePost(true);
                    if (mEditPostPreviewFragment != null)
                        mEditPostPreviewFragment.loadPost(mPost);
                }
            }
        });

        // Autosave handler
        mAutoSaveHandler = new Handler();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves both post objects so we can restore them in onCreate()
        savePost(true);
        outState.putSerializable(STATE_KEY_CURRENT_POST, mPost);
        outState.putSerializable(STATE_KEY_ORIGINAL_POST, mOriginalPost);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.edit_post, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem previewMenuItem = menu.findItem(R.id.menu_preview_post);
        MenuItem saveMenuItem = menu.findItem(R.id.menu_save_post);
        if (mViewPager != null && mViewPager.getCurrentItem() > PAGE_CONTENT) {
            previewMenuItem.setVisible(false);
            saveMenuItem.setVisible(false);
        } else {
            previewMenuItem.setVisible(true);
            saveMenuItem.setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_save_post) {
            if (mPost.isUploaded()) {
                WPMobileStatsUtil.flagProperty(getStatEventEditorClosed(), WPMobileStatsUtil.StatsPropertyPostDetailClickedUpdate);
            } else {
                WPMobileStatsUtil.flagProperty(getStatEventEditorClosed(), WPMobileStatsUtil.StatsPropertyPostDetailClickedPublish);
            }
            savePost(false);
            PostUploadService.addPostToUpload(mPost);
            startService(new Intent(this, PostUploadService.class));
            Intent i = new Intent();
            i.putExtra("shouldRefresh", true);
            setResult(RESULT_OK, i);
            finish();
            return true;
        } else if (itemId == R.id.menu_preview_post) {
            mViewPager.setCurrentItem(PAGE_PREVIEW);
        } else if (itemId == android.R.id.home) {
            if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
                mViewPager.setCurrentItem(PAGE_CONTENT);
                supportInvalidateOptionsMenu();
            } else {
                showCancelAlert();
            }
            return true;
        }
        return false;
    }

    private void showErrorAndFinish(int errorMessageId) {
        Toast.makeText(this, getResources().getText(errorMessageId), Toast.LENGTH_LONG).show();
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
        // Update post content from fragment fields
        if (mEditPostContentFragment != null)
            mEditPostContentFragment.savePostContent(isAutosave);
        if (mEditPostSettingsFragment != null)
            mEditPostSettingsFragment.savePostSettings();
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
            mViewPager.setCurrentItem(PAGE_CONTENT);
            supportInvalidateOptionsMenu();
            return;
        }

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

        savePost(true);

        // Compare the current Post to the original and if no changes have been made,
        // set the Post back to the original and go back to the previous view
        if (mOriginalPost != null && !mPost.hasChanges(mOriginalPost)) {
            mOriginalPost.update();
            WordPress.currentPost = mOriginalPost;
            finish();
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getString((mPost.isPage()) ? R.string.edit_page : R.string.edit_post));
        dialogBuilder.setMessage(getString(R.string.prompt_save_changes));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                savePost(false);
                if(mPost.hasChanges(mOriginalPost)) {
                    WordPress.currentPost = mPost;
                }
                Intent i = new Intent();
                i.putExtra("shouldRefresh", true);
                setResult(RESULT_OK, i);
                finish();
            }
        });
        dialogBuilder.setNeutralButton(getString(R.string.discard), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // When discard option is chosen, restore existing post or delete new post if it was autosaved.
                if (mOriginalPost != null && !mIsNewPost) {
                    mOriginalPost.update();
                    WordPress.currentPost = mOriginalPost;
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

    public String getStatEventEditorClosed() {
        return mStatEventEditorClosed;
    }

    public void showPostSettings() {
        mViewPager.setCurrentItem(PAGE_SETTINGS);
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
                    return new EditPostContentFragment();
                case 1:
                    return new EditPostSettingsFragment();
                default:
                    return new EditPostPreviewFragment();
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mEditPostContentFragment = (EditPostContentFragment)fragment;
                    break;
                case 1:
                    mEditPostSettingsFragment = (EditPostSettingsFragment)fragment;
                    break;
                case 2:
                    mEditPostPreviewFragment = (EditPostPreviewFragment)fragment;
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    public boolean isEditingPostContent() {
        return (mViewPager.getCurrentItem() == PAGE_CONTENT);
    }
}
