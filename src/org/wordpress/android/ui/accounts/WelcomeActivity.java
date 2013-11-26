package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.util.WPViewPager;


public class WelcomeActivity extends SherlockFragmentActivity {
    /**
     * The number of pages (wizard steps)
     */
    private static final int NUM_PAGES = 1; // TODO: this will probably be merged with New
                                            // Account Activity
    public static final int SIGNIN_REQUEST = 1;
    public static final int CREATE_ACCOUNT_REQUEST = 2;
    public static final int CREATE_BLOG_REQUEST = 3;
    public static final int ADD_SELF_HOSTED_BLOG = 4;

    public static String START_FRAGMENT_KEY = "start-fragment";

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private WPViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    //keep references to single page here
    NewBlogFragment welcomeFragmentNewBlog;
    WelcomeFragmentSignIn welcomeFragmentSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (WPViewPager) findViewById(R.id.pager);
        mPager.setPagingEnabled(false);
        mPagerAdapter = new NewAccountPagerAdapter(super.getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getInt(START_FRAGMENT_KEY, -1) == CREATE_BLOG_REQUEST) {
            createBlog();
        }
        if (extras != null && extras.getInt(START_FRAGMENT_KEY, -1) == ADD_SELF_HOSTED_BLOG) {
            addSelfHosted();
        }

    }

    public void showNextItem() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    }
    
    public void showPrevItem() {
        if (mPager.getCurrentItem() == 0)
            return;
        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0)
            super.onBackPressed();
        if (mPager.getPreviousPage() < mPager.getCurrentItem())
            mPager.setCurrentItem(mPager.getPreviousPage());
        else
            showPrevItem();
    }

    private class NewAccountPagerAdapter extends FragmentStatePagerAdapter {
       
        public NewAccountPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            NewAccountAbstractPageFragment currentPage = null;
            Bundle args = new Bundle();

            switch (position) {
                default:
                    welcomeFragmentSignIn = new WelcomeFragmentSignIn();
                    currentPage = welcomeFragmentSignIn;
                    break;
            }

            currentPage.setArguments(args);
            return currentPage;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String username = data.getStringExtra("username");
            if (username != null) {
                mPager.setCurrentItem(1);
                welcomeFragmentSignIn.signInDotComUser();
            }
        }
    }

    protected void addSelfHosted() {
    }

    protected void createBlog() {
        Intent newAccountIntent = new Intent(WelcomeActivity.this, NewAccountActivity.class);
        newAccountIntent.putExtra(WelcomeActivity.START_FRAGMENT_KEY,
                WelcomeActivity.CREATE_BLOG_REQUEST);
        startActivityForResult(newAccountIntent, WelcomeActivity.CREATE_BLOG_REQUEST);
    }
}