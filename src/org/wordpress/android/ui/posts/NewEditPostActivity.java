package org.wordpress.android.ui.posts;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.PostUploadService;

public class NewEditPostActivity extends SherlockFragmentActivity implements ActionBar.TabListener {

    public static String EXTRA_POSTID = "postId";
    public static String EXTRA_IS_PAGE = "isPage";
    public static String EXTRA_IS_NEW_POST = "isNewPost";

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
    ViewPager mViewPager;

    private Post mPost;
    private Post mOriginalPost;

    private EditPostContentFragment mEditPostContentFragment;
    private EditPostSettingsFragment mEditPostSettingsFragment;

    private boolean mIsNewPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_edit_post);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                mEditPostContentFragment.setContentEditTextFocusable(position == 0);
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
                    // big oopsie
                    Toast.makeText(this, getResources().getText(R.string.post_not_found), Toast.LENGTH_LONG).show();
                    finish();
                    return;
                } else {
                    mOriginalPost = new Post(WordPress.getCurrentBlog().getId(), postId, isPage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        }
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
        super.onPrepareOptionsMenu(menu);

        if (getSupportActionBar() != null)
            menu.findItem(R.id.menu_edit_post).setVisible(getSupportActionBar().getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS);

        return true;
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_edit_post) {

            savePost();

                /*if (mQuickMediaType >= 0) {
                    if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA || mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                        mPost.setQuickPostType("QuickPhoto");
                    else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA || mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                        mPost.setQuickPostType("QuickVideo");
                }*/
            PostUploadService.addPostToUpload(mPost);
            startService(new Intent(this, PostUploadService.class));
            Intent i = new Intent();
            i.putExtra("shouldRefresh", true);
            setResult(RESULT_OK, i);
            finish();
            return true;
        } else if (itemId == android.R.id.home) {
            showCancelAlert(true);
            return true;
        }
        return false;
    }

    public Post getPost() {
        return mPost;
    }

    private void savePost() {
        // Update post content from fragment fields
        if (mEditPostContentFragment != null)
            mEditPostContentFragment.savePostContent(false);
        if (mEditPostSettingsFragment != null)
            mEditPostSettingsFragment.savePostSettings(false);
    }

    @Override
    public void onBackPressed() {
        showCancelAlert(false);
    }

    private void showCancelAlert(final boolean isUpPress) {
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
                savePost();
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
            // Return a PlaceholderFragment (defined as a static inner class below).


            switch (position) {
                case 0:
                    mEditPostContentFragment = new EditPostContentFragment();
                    return mEditPostContentFragment;
                case 1:
                    mEditPostSettingsFragment = new EditPostSettingsFragment();
                    return mEditPostSettingsFragment;
                default:
                    return PlaceholderFragment.newInstance(position + 1);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_new_edit_post, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

}
