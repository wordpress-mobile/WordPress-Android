package org.wordpress.android.ui.stats;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.wordpress.android.ui.stats.Stats.Timeframe;

public class StatsPagerAdapter extends FragmentPagerAdapter {

    private Stats.ViewType mViewType;
    private Timeframe[] mTimeframes;

    public StatsPagerAdapter(FragmentManager fm, Stats.ViewType viewType, Stats.Timeframe[] timeframes) {
        super(fm);
        mViewType = viewType;
        mTimeframes = timeframes;
    }

    @Override
    public Fragment getItem(int i) {
        return StatsAbsCategoryFragment.newInstance(mViewType, mTimeframes[i]);
    }

    @Override
    public int getCount() {
        return mTimeframes.length;
    }
    
    @Override
    public CharSequence getPageTitle(int position) {
        return mTimeframes[position].getLabel();
    }

}
