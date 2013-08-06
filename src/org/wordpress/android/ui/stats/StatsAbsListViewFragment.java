package org.wordpress.android.ui.stats;


import java.util.Locale;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.Tab;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.util.Utils;

public abstract class StatsAbsListViewFragment extends StatsAbsViewFragment implements TabListener, OnCheckedChangeListener {

    protected ViewPager mViewPager;
    protected HorizontalTabView mTabView;
    protected FragmentStatePagerAdapter mAdapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);
        
        setRetainInstance(true);
        
        if (Utils.isTablet(getActivity())) {
            initTabletLayout(view);
        } else {
            initPhoneLayout(view);
        }
        
        
        return view;
    }

    private void initTabletLayout(View view) {
        
        TextView titleView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleView.setText(getTitle().toUpperCase(Locale.getDefault()));
        
        String[] titles = getTabTitles();
        
        RadioGroup rg = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);
        rg.setVisibility(View.VISIBLE);
        rg.setOnCheckedChangeListener(this);
        
        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) LayoutInflater.from(getActivity()).inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            int dp4 = (int) dpToPx(4);
            params.setMargins(dp4, 0, dp4, 0);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            rg.addView(rb);
            if (i == 0)
                rb.setChecked(true);
        }
        
        Fragment fragment = getFragment(0);
        getChildFragmentManager().beginTransaction().add(R.id.stats_pager_container, fragment).commit();
        
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // TODO
    }
    
    private void initPhoneLayout(View view) {
        mViewPager = (ViewPager) view.findViewById(R.id.stats_pager_viewpager);
        mViewPager.setVisibility(View.VISIBLE);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabView.setSelectedTab(position);
            }
        });

        mTabView = (HorizontalTabView) view.findViewById(R.id.stats_pager_tabs);
        mTabView.setVisibility(View.VISIBLE);
        mTabView.setTabListener(this);
        
        mAdapter = getAdapter();
        mViewPager.setAdapter(mAdapter);
        addTabs();
        mTabView.setSelectedTab(0);
    }
    
    private void addTabs() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mTabView.addTab(mTabView.newTab().setText(mAdapter.getPageTitle(i)));
        }
    }

    @Override
    public void onTabSelected(Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    public abstract FragmentStatePagerAdapter getAdapter();

    public abstract String[] getTabTitles();

    protected abstract Fragment getFragment(int position);
    
    private float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }    
    
    @Override
    public void refresh() {
        int count = getTabTitles().length;
        for (int i = 0; i < count; i++){
            refresh(i);
        }
    }
    
    public abstract void refresh(int position);
}
