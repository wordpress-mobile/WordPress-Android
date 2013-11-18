package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.util.WPViewPager;


public class NewAccountActivity extends SherlockFragmentActivity {
    /**
     * The number of pages (wizard steps)
     */
    private static final int NUM_PAGES = 3;

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
    NewUserPageFragment userFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_new_account);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (WPViewPager) findViewById(R.id.pager);
        mPagerAdapter = new NewAccountPagerAdapter( super.getSupportFragmentManager() );
        mPager.setAdapter(mPagerAdapter);
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
                    userFragment = new NewUserPageFragment();
                    currentPage = userFragment;
                    break;
                default:
                    currentPage = new NewUserPageFragment();
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
}