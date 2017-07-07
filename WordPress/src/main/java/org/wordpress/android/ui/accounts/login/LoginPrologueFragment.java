package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.WPViewPager;

public class LoginPrologueFragment extends Fragment {

    public static final String TAG = "login_prologue_fragment_tag";

    LoginListener mLoginListener;

    private WPViewPager mPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_signup_screen, container, false);

        view.findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.showEmailLoginScreen();
                }
            }
        });

        view.findViewById(R.id.create_site_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.doStartSignup();
                }
            }
        });

        mPager = (WPViewPager) view.findViewById(R.id.intros_pager);
        LoginProloguePagerAdapter adapter = new LoginProloguePagerAdapter(getChildFragmentManager());
        mPager.setAdapter(adapter);

        // Using a TabLayout for simulating a page indicator strip
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout_indicator);
        tabLayout.setupWithViewPager(mPager, true);

        if (savedInstanceState == null) {
            DisplayUtils.padWithStatusBarHeight(getResources(), tabLayout);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    public void onNextPromo() {
        int nextItem = mPager.getCurrentItem() + 1;
        if (nextItem == mPager.getAdapter().getCount()) {
            nextItem = 0;
        }

        mPager.setCurrentItem(nextItem, true);
    }

    public boolean pagerHasMovedManually() {
        return mPager != null && mPager.hasMovedManually();
    }
}
