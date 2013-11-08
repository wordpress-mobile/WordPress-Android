package org.wordpress.android.ui.accounts;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.R;
import org.wordpress.android.util.LinePageIndicator;
import org.wordpress.android.util.WPViewPager;


public class TutorialActivity extends SherlockFragmentActivity {
    /**
     * The number of pages (wizard steps)
     */
    private static final int NUM_PAGES = 4;

    public static final String VIEWED_TUTORIAL = "viewedTutorial";

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

    // Keep references to single page here
    private TutorialFragmentStats mTutorialFragmentStats;
    private TutorialFragmentReader mTutorialFragmentReader;
    private TutorialFragmentNotifications mTutorialFragmentNotifications;
    private TutorialFragmentGetStarted mTutorialFragmentGetStarted;
    
    private RelativeLayout mFooterView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_tutorial);

        // The tutorial is a one time deal, set a pref bool to not show it if the user signs out and in again
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(VIEWED_TUTORIAL, true);
        editor.commit();
        
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (WPViewPager) findViewById(R.id.pager);
        mPager.setPagingEnabled(true);
        mPagerAdapter = new TutorialPagerAdapter( super.getSupportFragmentManager() );
        mPager.setAdapter(mPagerAdapter);
        
        mFooterView = (RelativeLayout) findViewById(R.id.footer_view);
        
        mLinePageIndicator = (LinePageIndicator)findViewById(R.id.pageIndicator);
        mLinePageIndicator.setViewPager(mPager);

        RelativeLayout footerView = (RelativeLayout)findViewById(R.id.footer_view);
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Show success dialog
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        NUXDialogFragment alert = NUXDialogFragment.newInstance(getString(R.string.nux_dialog_success),
                getString(R.string.nux_dialog_success_message),
                getString(R.string.nux_dialog_success_continue), R.drawable.nux_icon_check);
        alert.show(ft, "alert");
    }
    
    private class TutorialPagerAdapter extends FragmentStatePagerAdapter {
       
        public TutorialPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            NewAccountAbstractPageFragment currentPage = null;
            Bundle args = new Bundle();
            args.putInt(NewAccountAbstractPageFragment.ARG_PAGE, position);
            
            switch (position) {
                case 0:
                    mTutorialFragmentStats = new TutorialFragmentStats();
                    currentPage = mTutorialFragmentStats;
                    break;
                case 1:
                    mTutorialFragmentReader = new TutorialFragmentReader();
                    currentPage = mTutorialFragmentReader;
                    break;
                case 2:
                    mTutorialFragmentNotifications = new TutorialFragmentNotifications();
                    currentPage = mTutorialFragmentNotifications;
                    break;
                case 3:
                    mTutorialFragmentGetStarted = new TutorialFragmentGetStarted();
                    currentPage = mTutorialFragmentGetStarted;
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
}