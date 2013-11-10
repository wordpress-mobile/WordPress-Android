package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.Window;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.R;
import org.wordpress.android.util.LinePageIndicator;
import org.wordpress.android.util.WPViewPager;


public class WelcomeActivity extends SherlockFragmentActivity {

    public static final String SKIP_WELCOME = "skipWelcome";

    /**
     * The number of pages (wizard steps)
     */
    private static final int NUM_PAGES = 2;

    public static final int CREATE_ACCOUNT_REQUEST = 0;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private WPViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;
    
    private LinePageIndicator mLinePageIndicator;

    //keep references to single page here
    WelcomeFragmentHome welcomeFragmentHome;
    WelcomeFragmentPublish welcomeFragmentPublish;
    WelcomeFragmentSignIn welcomeFragmentSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (WPViewPager) findViewById(R.id.pager);
        mPager.setPagingEnabled(false);
        mPagerAdapter = new NewAccountPagerAdapter( super.getSupportFragmentManager() );
        mPager.setAdapter(mPagerAdapter);

        if (getIntent().getBooleanExtra(SKIP_WELCOME, false))
            mPager.setCurrentItem(1);
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
            args.putInt(NewAccountAbstractPageFragment.ARG_PAGE, position);
            
            switch (position) {
                case 0:
                    welcomeFragmentHome = new WelcomeFragmentHome(mPager);
                    currentPage = welcomeFragmentHome;
                    break;
                case 1:
                    welcomeFragmentSignIn = new WelcomeFragmentSignIn();
                    currentPage = welcomeFragmentSignIn;
                    break;
                default:
                    currentPage = new NewBlogPageFragment();
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

        switch (requestCode) {
            case CREATE_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK && data != null) {
                    String username = data.getStringExtra("username");
                    if (username != null) {
                        mPager.setCurrentItem(2);
                        welcomeFragmentSignIn.signInDotComUser();
                    }
                }
                break;
        }

    }


}