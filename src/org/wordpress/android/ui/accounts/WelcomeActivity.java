package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.RelativeLayout;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.wordpress.android.R;
import org.wordpress.android.util.LinePageIndicator;
import org.wordpress.android.util.WPViewPager;
import org.wordpress.android.widgets.WPTextView;


public class WelcomeActivity extends SherlockFragmentActivity {

    public static final String SKIP_WELCOME = "skipWelcome";

    /**
     * The number of pages (wizard steps)
     */
    private static final int NUM_PAGES = 3;

    static final int CREATE_ACCOUNT_REQUEST = 0;

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
    
    private RelativeLayout mFooterView;
    
    private WPTextView mSignInButton;
    private WPTextView mCreateAccountButton;
    private WPTextView mSignInTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);

        mSignInButton = (WPTextView) findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(2);
            }
            
        });
        mCreateAccountButton = (WPTextView) findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);
        mSignInTextView = (WPTextView) findViewById(R.id.nux_sign_in);
        mSignInTextView.setOnClickListener(mCreateAccountListener);
        
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (WPViewPager) findViewById(R.id.pager);
        mPager.setPagingEnabled(true);
        mPagerAdapter = new NewAccountPagerAdapter( super.getSupportFragmentManager() );
        mPager.setAdapter(mPagerAdapter);
        
        mFooterView = (RelativeLayout) findViewById(R.id.footer_view);
        
        mLinePageIndicator = (LinePageIndicator)findViewById(R.id.pageIndicator);
        mLinePageIndicator.setViewPager(mPager);
        mLinePageIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    mLinePageIndicator.setVisibility(View.GONE);
                    mSignInButton.setVisibility(View.GONE);
                    mCreateAccountButton.setVisibility(View.GONE);
                    mSignInTextView.setVisibility(View.VISIBLE);
                } else {
                    mLinePageIndicator.setVisibility(View.VISIBLE);
                    mSignInButton.setVisibility(View.VISIBLE);
                    mCreateAccountButton.setVisibility(View.VISIBLE);
                    mSignInTextView.setVisibility(View.GONE);
                }
            }
        });
        
        // Hide the footer view if the soft keyboard is showing
        // See: http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        final View mainView = findViewById(R.id.main_view);
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = mainView.getRootView().getHeight() - mainView.getHeight();
                if (heightDiff > 100) {
                    mFooterView.setVisibility(View.GONE);
                } else {
                    mFooterView.setVisibility(View.VISIBLE);
                }
             }
        });

        if (getIntent().getBooleanExtra(SKIP_WELCOME, false))
            mPager.setCurrentItem(2);
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
                    welcomeFragmentHome = new WelcomeFragmentHome();
                    currentPage = welcomeFragmentHome;
                    break;
                case 1:
                    welcomeFragmentPublish = new WelcomeFragmentPublish();
                    currentPage = welcomeFragmentPublish;
                    break;
                case 2:
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

    private View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(WelcomeActivity.this, NewAccountActivity.class);
            startActivityForResult(newAccountIntent, CREATE_ACCOUNT_REQUEST);
        }
    };
}