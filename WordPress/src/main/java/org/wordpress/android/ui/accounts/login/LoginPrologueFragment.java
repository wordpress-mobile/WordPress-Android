package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.widgets.WPViewPager;

public class LoginPrologueFragment extends Fragment {
    public static final String TAG = "login_prologue_fragment_tag";

    LoginPrologueListener mLoginPrologueListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_signup_screen, container, false);

        view.findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginPrologueListener != null) {
                    mLoginPrologueListener.showEmailLoginScreen();
                }
            }
        });

        view.findViewById(R.id.create_site_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginPrologueListener != null) {
                    mLoginPrologueListener.doStartSignup();
                }
            }
        });

        ViewPager.OnPageChangeListener listener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                switch (LoginProloguePagerAdapter.getTag(position)) {
                    case LoginProloguePagerAdapter.LOGIN_PROLOGUE_POST_TAG:
                        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED_POST);
                        break;
                    case LoginProloguePagerAdapter.LOGIN_PROLOGUE_STATS_TAG:
                        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED_STATS);
                        break;
                    case LoginProloguePagerAdapter.LOGIN_PROLOGUE_READER_TAG:
                        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED_READER);
                        break;
                    case LoginProloguePagerAdapter.LOGIN_PROLOGUE_NOTIFICATIONS_TAG:
                        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED_NOTIFICATIONS);
                        break;
                    case LoginProloguePagerAdapter.LOGIN_PROLOGUE_JETPACK_TAG:
                        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED_JETPACK);
                        break;
                }

                AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        };

        WPViewPager pager = view.findViewById(R.id.intros_pager);
        LoginProloguePagerAdapter adapter = new LoginProloguePagerAdapter(getChildFragmentManager());
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(listener);

        // Using a TabLayout for simulating a page indicator strip
        TabLayout tabLayout = view.findViewById(R.id.tab_layout_indicator);
        tabLayout.setupWithViewPager(pager, true);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_VIEWED);
        }

        return view;
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // important for accessibility - talkback
        getActivity().setTitle(R.string.login_prologue_screen_title);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginPrologueListener) {
            mLoginPrologueListener = (LoginPrologueListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginPrologueListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginPrologueListener = null;
    }
}
